package com.whu.medicalbackend.common.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.ibatis.type.*;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class LocalTimeListTypeHandler implements TypeHandler<List<LocalTime>>{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    static {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    @Override
    public void setParameter(PreparedStatement ps, int i, List<LocalTime> parameter, JdbcType jdbcType) throws SQLException {
        try {
            List<String> timeStrings = parameter.stream()
                    .map(time -> time.format(FORMATTER))
                    .collect(Collectors.toList());
            ps.setString(i, OBJECT_MAPPER.writeValueAsString(timeStrings));
        } catch (JsonProcessingException e) {
            throw new SQLException("将List<LocalTime>转为JSON失败", e);
        }
    }

    @Override
    public List<LocalTime> getResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public List<LocalTime> getResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public List<LocalTime> getResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private List<LocalTime> parseJson(String json) throws SQLException {
        try {
            List<String> list = OBJECT_MAPPER.readValue(json, new TypeReference<List<String>>() {});

            List<LocalTime> timeList = list.stream()
                    .map(str -> LocalTime.parse(str, FORMATTER))
                    .collect(Collectors.toList());
            return timeList;
        } catch (JsonProcessingException e) {
            throw new SQLException("解析JSON为List<LocalTime>失败:  " + json, e);
        }
    }
}
