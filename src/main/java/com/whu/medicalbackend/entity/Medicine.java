package com.whu.medicalbackend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Medicine implements Serializable{
    private static final long serialVersionUID = 2L;

    private Long          id;
    private Long          userId;
    private String        name;
    private String        defaultDosage;
    private String        remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
