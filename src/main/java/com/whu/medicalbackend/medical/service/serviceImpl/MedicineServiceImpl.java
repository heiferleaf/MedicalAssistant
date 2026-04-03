package com.whu.medicalbackend.medical.service.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.medicalbackend.medical.entity.Medicine;
import com.whu.medicalbackend.common.exception.BusinessException;
import com.whu.medicalbackend.medical.mapper.MedicineMapper;
import com.whu.medicalbackend.medical.mapper.MedicationPlanMapper;
import com.whu.medicalbackend.medical.dto.*;
import com.whu.medicalbackend.medical.service.MedicineService;
import com.whu.medicalbackend.medical.service.PlanService;
import com.whu.medicalbackend.agent.service.serviceImpl.RedisService;
import com.whu.medicalbackend.common.util.RedisKeyBuilderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Transactional(rollbackFor = Exception.class)
public class MedicineServiceImpl implements MedicineService {

    @Autowired
    private MedicineMapper medicineMapper;
    @Autowired
    private MedicationPlanMapper medicationPlanMapper;
    @Autowired
    private RedisService redisService;

    // 防止依赖循环，通过 set 函数注入属性
    private PlanService planService;

    @Lazy
    @Autowired
    public void setPlanService(PlanService planService) {
        this.planService = planService;
    }

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 查找或创建药品
     *
     * 业务逻辑：
     * 1. 先查询用户是否已有同名药品
     * 2. 如果存在，直接返回
     * 3. 如果不存在，创建新药品并返回
     */
    @Override
    public Medicine findOrCreate(Long userId, String name, String defaultDosage) {
        // 1. 查询现有药品
        Medicine medicine = medicineMapper.findByUserIdAndName(userId, name);

        // 2. 如果存在，直接返回
        if (medicine != null) {
            return medicine;
        }

        // 3. 不存在，创建新药品
        medicine = new Medicine();
        medicine.setUserId(userId);
        medicine.setName(name);
        medicine.setDefaultDosage(defaultDosage);

        // 4. 插入数据库（主键会自动回填）
        medicineMapper.insert(medicine);

        return medicine;
    }

    @Override
    public List<Medicine> getMedicinesByUserId(Long userId) {
        return medicineMapper.findByUserId(userId);
    }

    /** 查询当前用户的全部药品 */
    @Override
    public List<MedicineVO> getMedicineList(Long userId) {
        String cacheKey = RedisKeyBuilderUtil.getUserMedicineKey(userId);
        String obj = redisService.get(cacheKey);
        if (obj != null) {
            try {
                return objectMapper.readValue(obj, new TypeReference<>(){});
            } catch (JsonProcessingException e) {
                throw new BusinessException("获取药物数据出错" + e.getMessage());
            }
        }

        String lockKey = RedisKeyBuilderUtil.getUserMedicineLockKey(userId);
        if(!redisService.tryLock(lockKey, 5, 10)) {
            throw new BusinessException("查询处理中");
        }

        try {
            obj = redisService.get(cacheKey);
            if (obj != null) {
                return objectMapper.readValue(obj, new TypeReference<>(){});
            }
            List<MedicineVO> result = doGetMedicineList(userId);
            redisService.setWithExpire(cacheKey, objectMapper.writeValueAsString(result), 5, TimeUnit.DAYS);
            return result;
        } catch (JsonProcessingException e) {
            throw new BusinessException("获取药物/存入缓存数据出错" + e.getMessage());
        } finally {
            redisService.unlock(lockKey);
        }
    }

    /** 新增药品 */
    @Override
    public MedicineVO addMedicine(Long userId, MedicineCreateDTO dto) {
        String lockKey = RedisKeyBuilderUtil.getUserAddMedicineLockKey(userId);
        if(!redisService.tryLock(lockKey, 0, 10)) {
            throw new BusinessException("请勿重复操作");
        }

        try {
            MedicineVO medicineVO = doAddMedicine(userId, dto);

            return medicineVO;
        } catch (Exception e) {
            throw new BusinessException("业务出错" + e.getMessage());
        } finally {
            redisService.unlock(lockKey);
        }
    }

    /** 编辑药品 */
    @Override
    public MedicineVO updateMedicine(Long userId, Long medicineId, MedicineCreateDTO dto) {
        String lockKey = RedisKeyBuilderUtil.getUserUpdateMedicineLockKey(userId);
        if(!redisService.tryLock(lockKey, 0, 10)) {
            throw new BusinessException("请勿重复操作");
        }

        try {
            Medicine medicine = medicineMapper.findById(medicineId);
            if (medicine == null) {
                throw new BusinessException("更新的药品不存在");
            }
            if (!medicine.getUserId().equals(userId)) {
                throw new BusinessException("药品不属于该用户");
            }
            MedicineVO medicineVO = doUpdateMedicine(medicine, dto);
            return medicineVO;

        } finally {
            redisService.unlock(lockKey);
        }
    }

    /** 删除药品 */
    @Override
    public void deleteMedicine(Long userId, Long medicineId) {
        String lockKey = RedisKeyBuilderUtil.getUserDeleteMedicineLockKey(userId);
        if(!redisService.tryLock(lockKey, 0, 10)) {
            throw new BusinessException("请勿重复操作");
        }

        try {
            Medicine medicine = medicineMapper.findById(medicineId);
            if (medicine == null) {
                throw new BusinessException("删除的药品不存在");
            }
            if (!medicine.getUserId().equals(userId)) {
                throw new BusinessException("药品不属于该用户");
            }

            int activePlansCount = medicationPlanMapper.countActivePlansByMedicineId(medicineId);
            if (activePlansCount > 0) {
                throw new BusinessException("该药品仍在当前的用药计划中生效，请先停止/删除相关计划再移除该药品！");
            }

            if(doDeleteMedicine(medicineId) != 1) {
                throw new BusinessException("删除出错");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("删除出错：" + e.getMessage());
        } finally {
            redisService.unlock(lockKey);
        }
    }

    /**
     * 从药箱快速创建计划
     * 找到对应药品后，委托 PlanService 完成计划创建
     */
    @Override
    public PlanVO createPlanFromMedicine(Long userId, Long medicineId, PlanFromMedicineDTO dto) {
        String lockKey = RedisKeyBuilderUtil.getUserMedicinePlanLockKey(userId);
        if(!redisService.tryLock(lockKey, 0, 10)) {
            throw new BusinessException("请勿重复操作");
        }

        try {
            // 1. 查询并校验药品归属
            Medicine medicine = medicineMapper.findById(medicineId);
            if (medicine == null) {
                throw new BusinessException("使用的药品不存在");
            }
            if (!medicine.getUserId().equals(userId)) {
                throw new BusinessException("药品不属于该用户");
            }
            // 2. 剂量未填则使用药品默认剂量
            String dosage = (dto.getDosage() != null && !dto.getDosage().isBlank())
                    ? dto.getDosage()
                    : medicine.getDefaultDosage();
            if (dosage == null || dosage.isBlank()) {
                throw new BusinessException("请指定服药剂量（该药品未设置默认剂量）");
            }

            // 3. 组装成标准计划 DTO，复用 PlanService 的创建逻辑
            PlanCreateDTO planCreateDTO = new PlanCreateDTO();
            planCreateDTO.setMedicineName(medicine.getName());
            planCreateDTO.setDosage(dosage);
            planCreateDTO.setStartDate(dto.getStartDate());
            planCreateDTO.setEndDate(dto.getEndDate());
            planCreateDTO.setTimePoints(dto.getTimePoints());
            planCreateDTO.setRemark(dto.getRemark());

            return planService.createPlan(userId, planCreateDTO);
        } finally {
            redisService.unlock(lockKey);
        }
    }

    // public 是为了让事务传播行为发生
    public List<MedicineVO> doGetMedicineList(Long userId) {
        List<Medicine> medicines = medicineMapper.findAllByUserId(userId);
        return medicines.stream()
                .map(m ->
                        MedicineVO.builder()
                                .id(m.getId())
                                .name(m.getName())
                                .remark(m.getRemark())
                                .defaultDosage(m.getDefaultDosage())
                                .createdAt(m.getCreatedAt())
                                .updatedAt(m.getUpdatedAt())
                                .build())
                .toList();
    }

    public MedicineVO doAddMedicine(Long userId, MedicineCreateDTO dto) {
        Medicine m = Medicine.builder()
                .userId(userId)
                .name(dto.getName())
                .remark(dto.getRemark())
                .defaultDosage(dto.getDefaultDosage())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        medicineMapper.insert(m);

        return MedicineVO.builder()
                .id(m.getId())
                .name(m.getName())
                .remark(m.getRemark())
                .defaultDosage(m.getDefaultDosage())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    public int doDeleteMedicine(Long medicineId) {
        return medicineMapper.deleteById(medicineId);
    }

    public MedicineVO doUpdateMedicine(Medicine medicine, MedicineCreateDTO dto) {
        medicine.setName(dto.getName());
        medicine.setRemark(dto.getRemark());
        medicine.setDefaultDosage(dto.getDefaultDosage());
        medicine.setUpdatedAt(LocalDateTime.now());
        medicineMapper.update(medicine);
        return MedicineVO.builder()
                .id(medicine.getId())
                .name(medicine.getName())
                .remark(medicine.getRemark())
                .defaultDosage(medicine.getDefaultDosage())
                .createdAt(medicine.getCreatedAt())
                .updatedAt(medicine.getUpdatedAt())
                .build();
    }
}
