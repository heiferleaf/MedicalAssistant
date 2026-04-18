package com.whu.medicalbackend.common.mq.entity;

import lombok.Data;

import java.util.List;

@Data
public class CanalMessage<T> {
    private String database;
    private String table;
    private String type;
    private List<T> data;
    private String sql;
    private List<String> pkName;
    private Boolean isDdl;
}
