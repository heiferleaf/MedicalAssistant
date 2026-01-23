package com.whu.medicalbackend.service.serviceImpl;

import com.whu.medicalbackend.common.ResultCode;
import com.whu. medicalbackend.dto.PlanCreateDTO;
import com.whu.medicalbackend.dto. PlanVO;
import com.whu. medicalbackend.entity.Medicine;
import com.whu. medicalbackend.entity.MedicationPlan;
import com.whu.medicalbackend.entity.MedicationTask;
import com. whu.medicalbackend. exception.BusinessException;
import com.whu.medicalbackend.mapper. MedicationPlanMapper;
import com.whu.medicalbackend.mapper.MedicationTaskMapper;
import com.whu.medicalbackend.mapper.MedicineMapper;
import com.whu.medicalbackend.schedule.DynamicTaskScheduler;
import com.whu.medicalbackend.service.MedicineService;
import com.whu.medicalbackend.service. PlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream. Collectors;

/**
 * 用药计划服务实现类
 *
 * 设计模式：
 * 1. 门面模式（Facade）：封装Medicine、Plan、Task三个Mapper的复杂操作
 * 2. 模板方法模式（Template Method）：创建/编辑计划的流程固定，部分步骤可变
 * 3. 策略模式（Strategy）：任务生成策略（当前是按时间点生成，未来可扩展）
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class PlanServiceImpl implements PlanService {

    @Autowired
    private MedicineService medicineService;

    @Autowired
    private MedicationPlanMapper planMapper;

    @Autowired
    private MedicationTaskMapper taskMapper;

    @Autowired
    private MedicineMapper medicineMapper;

    @Autowired
    private DynamicTaskScheduler dynamicTaskScheduler;

    /**
     * 获取用户的所有计划
     *
     * 性能优化：批量查询药品，避免N+1查询问题
     * Java知识点：Stream API、Map、Lambda表达式
     */
    @Override
    public List<PlanVO> getPlanList(Long userId) {
        // 1. 查询计划列表
        List<MedicationPlan> plans = planMapper.findByUserId(userId);

        if (plans.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 收集所有药品ID（去重）
        List<Long> medicineIds = plans.stream()
                .map(MedicationPlan::getMedicineId)
                .distinct()  // 去重
                .collect(Collectors.toList());

        // 3. 批量查询药品（一次查询，避免N次循环查询）
        // Java知识点：Function. identity() 等价于 medicine -> medicine
        Map<Long, Medicine> medicineMap = medicineIds.stream()
                .map(id -> medicineMapper.findById(id))
                .filter(medicine -> medicine != null)  // 过滤null
                .collect(Collectors.toMap(Medicine::getId, Function.identity()));

        // 4. 转换为VO
        return plans.stream()
                .map(plan -> {
                    Medicine medicine = medicineMap.get(plan.getMedicineId());
                    String medicineName = medicine != null ? medicine.getName() : "未知药品";
                    return new PlanVO.PlanVOBuilder()
                            .setMedicationPlan(plan)
                            .setMedicineName(medicineName)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 创建用药计划
     *
     * 设计模式：模板方法模式
     * 算法骨架：
     * 1. 查找或创建药品（可变）
     * 2. 创建计划（固定）
     * 3. 生成所有任务（可变）
     */
    @Override
    public PlanVO createPlan(Long userId, PlanCreateDTO dto) {
        // 1. 查找或创建药品
        Medicine medicine = medicineService.findOrCreate(
                userId,
                dto.getMedicineName(),
                dto.getDosage()
        );

        // 2. 创建计划对象
        MedicationPlan plan = new MedicationPlan();
        plan.setUserId(userId);
        plan.setMedicineId(medicine.getId());
        plan.setDosage(dto.getDosage());
        plan.setStartDate(dto.getStartDate());
        plan.setEndDate(dto.getEndDate());
        plan.setTimePoints(dto.getTimePoints());
        plan.setRemark(dto.getRemark());
        plan.setDeleted(0);

        // 3. 保存计划（主键会自动回填）
        planMapper.insert(plan);

        // 4. 生成所有任务（从开始日期到结束日期）
        List<MedicationTask> tasksForPlan = generateFutureTasksForPlan(plan, dto.getStartDate());

        LocalDate today = LocalDate.now();
        if(today.isAfter(plan.getStartDate())) {
            tasksForPlan.stream()
                    .filter(task -> task.getTaskDate().equals(today))
                    .forEach(dynamicTaskScheduler::addTaskSchedule);
        }

        // 5. 返回VO
        return new PlanVO.PlanVOBuilder()
                .setMedicationPlan(plan)
                .setMedicineName(dto.getMedicineName())
                .build();
    }

    /**
     * 编辑用药计划
     *
     * 设计模式：模板方法模式
     * 算法骨架：
     * 1. 查询计划（固定）
     * 2. 验证权限和状态（固定）
     * 3. 删除未来任务（固定）
     * 4. 更新计划（固定）
     * 5. 重新生成未来任务（可变）
     */
    @Override
    public void updatePlan(Long userId, Long planId, PlanCreateDTO dto) {
        // 1. 查询计划
        MedicationPlan plan = planMapper.findById(planId);
        if (plan == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getMessage() + "计划不存在");
        }

        // 2. 验证权限
        if (!plan.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getMessage() + "无权操作此计划");
        }

        // 3. 验证计划状态（只能编辑进行中或未开始的计划）
        LocalDate today = LocalDate.now();
        if (plan.getEndDate() != null && plan.getEndDate().isBefore(today)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getMessage() + "已结束的计划不可编辑");
        }

        // 4. 删除未来任务（从明天开始，保留今天的任务）
        // 原因：今天的任务用户可能已经标记状态，不能删除
        LocalDate tomorrow = today.plusDays(1);
        taskMapper.deleteFutureTasks(planId, tomorrow);

        // 5. 查找或创建药品（药品名可能变了）
        Medicine medicine = medicineService.findOrCreate(
                userId,
                dto.getMedicineName(),
                dto.getDosage()
        );

        // 6. 更新计划
        plan.setMedicineId(medicine.getId());
        plan.setDosage(dto.getDosage());
        plan.setStartDate(dto.getStartDate());
        plan.setEndDate(dto.getEndDate());
        plan.setTimePoints(dto.getTimePoints());
        plan.setRemark(dto.getRemark());

        planMapper.update(plan);

        // 7. 重新生成未来任务（从明天到结束日期）
        generateFutureTasksForPlan(plan, tomorrow);
    }

    /**
     * 删除用药计划
     *
     * 业务逻辑：
     * 1. 软删除计划（保留历史记录）
     * 2. 删除未来任务（从今天开始）
     */
    @Override
    public void deletePlan(Long userId, Long planId) {
        // 1. 查询计划
        MedicationPlan plan = planMapper.findById(planId);
        if (plan == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getMessage() + "计划不存在");
        }

        // 2. 验证权限
        if (!plan. getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getMessage() + "无权操作此计划");
        }

        LocalDate today = LocalDate.now();
        List<MedicationTask> todayTasks = taskMapper.findByUserIdAndDate(userId, today)
                .stream()
                .filter(task -> task.getPlanId().equals(planId))
                .collect(Collectors.toList());

        // 取消今天任务的定时器
        for (MedicationTask task : todayTasks) {
            dynamicTaskScheduler.cancelTaskSchedule(task.getId());
        }

        // 3. 软删除计划
        planMapper.softDelete(planId);

        // 4. 删除未来任务（从今天开始）
        // 注意：历史任务保留（task_date < 今天的任务）
        taskMapper.deleteFutureTasks(planId, today);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 为计划在指定日期生成任务
     *
     * 设计模式：策略模式（任务生成策略）
     * 当前策略：根据时间点列表生成任务
     * 未来可扩展：按剂量递减策略、按周期策略等
     *
     * Java知识点：
     * 1. 集合操作
     * 2. 对��创建和属性设置
     */
    private List<MedicationTask> generateTasksForPlanAndDate(MedicationPlan plan, LocalDate date) {
        List<MedicationTask> tasks = new ArrayList<>();

        // 为每个时间点创建一个任务
        for (LocalTime timePoint : plan.getTimePoints()) {
            MedicationTask task = new MedicationTask();
            task.setUserId(plan.getUserId());
            task.setPlanId(plan.getId());
            task.setMedicineId(plan.getMedicineId());
            task.setDosage(plan.getDosage());
            task.setTaskDate(date);
            task.setTimePoint(timePoint);
            task.setStatus(0);  // 默认未服用

            tasks.add(task);
        }

        // 批量插入（性能优化：一条SQL插入多条记录）
        if (! tasks.isEmpty()) {
            taskMapper.batchInsert(tasks);
        }
        return tasks;
    }

    /**
     * 为计划生成未来的所有任务
     *
     * Java知识点：
     * 1. LocalDate日期操作（plusDays、isBefore、isAfter）
     * 2. while循环
     * 3. 三元运算符
     */
    private List<MedicationTask> generateFutureTasksForPlan(MedicationPlan plan, LocalDate fromDate) {
        // 计算结束日期
        // 如果计划有结束日期，用计划的结束日期
        // 如果是长期计划（无结束日期），生成未来30天的任务
        LocalDate endDate = plan.getEndDate() != null
                ? plan.getEndDate()
                : fromDate.plusDays(30);

        // 从fromDate到endDate，每天生成任务
        List<List<MedicationTask>> tasksForPlan = new ArrayList<>();
        LocalDate currentDate = fromDate;
        while (! currentDate.isAfter(endDate)) {
            tasksForPlan.add(generateTasksForPlanAndDate(plan, currentDate));
            currentDate = currentDate.plusDays(1);
        }
        return tasksForPlan.stream()
                .filter(tasks -> tasks.size() != 0)
                .flatMap(tasks -> tasks.stream())
                .collect(Collectors.toList());
    }
}