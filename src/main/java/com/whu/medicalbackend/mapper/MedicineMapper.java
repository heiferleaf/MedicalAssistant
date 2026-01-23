package com.whu.medicalbackend.mapper;

import com.whu.medicalbackend.entity.Medicine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MedicineMapper{
    Medicine findById(@Param("id") Long id);
    Medicine findByUserIdAndName(@Param("userId") Long userId, @Param("name") String name);
    int insert(Medicine medicine);
}
