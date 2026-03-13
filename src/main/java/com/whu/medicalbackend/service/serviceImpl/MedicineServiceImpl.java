package com.whu.medicalbackend.service.serviceImpl;

import com.whu.medicalbackend.entity.Medicine;
import com.whu.medicalbackend.mapper.MedicineMapper;
import com.whu.medicalbackend.service.MedicineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class MedicineServiceImpl implements MedicineService{

    @Autowired
    private MedicineMapper medicineMapper;

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
}
