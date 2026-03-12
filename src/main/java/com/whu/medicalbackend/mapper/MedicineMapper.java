package com.whu.medicalbackend.mapper;

import com.whu.medicalbackend.entity.Medicine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MedicineMapper{
    Medicine findById(@Param("id") Long id);
    Medicine findByUserIdAndName(@Param("userId") Long userId, @Param("name") String name);
    int insert(Medicine medicine);
    List<Medicine> findAllByUserId(@Param("userId") Long userId);
    int deleteById(@Param("id") Long id);
    int update(@Param("medicine") Medicine medicine);
}
