package com.whu.medicalbackend.mapper;

import com.whu.medicalbackend.entity.Medicine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MedicineMapper {
    Medicine findById(@Param("id") Long id);

    Medicine findByUserIdAndName(@Param("userId") Long userId, @Param("name") String name);

    int insert(Medicine medicine);

    @Select("SELECT id, user_id, name, default_dosage, remark, created_at, updated_at FROM medicine WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Medicine> findByUserId(@Param("userId") Long userId);
}
