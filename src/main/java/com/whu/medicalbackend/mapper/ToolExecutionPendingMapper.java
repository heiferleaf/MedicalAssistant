package com.whu.medicalbackend.mapper;

import com.whu.medicalbackend.entity.ToolExecutionPending;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Tool 执行待确认 Mapper
 */
@Mapper
public interface ToolExecutionPendingMapper {
    
    /**
     * 插入待确认记录
     */
    @Insert("""
        INSERT INTO tool_execution_pending 
        (request_id, user_id, session_id, tool_name, tool_arguments, original_ai_message, status, created_at, expires_at)
        VALUES 
        (#{requestId}, #{userId}, #{sessionId}, #{toolName}, #{toolArguments}, #{originalAiMessage}, #{status}, #{createdAt}, #{expiresAt})
        """)
    int insert(ToolExecutionPending pending);
    
    /**
     * 根据 requestId 查询
     */
    @Select("""
        SELECT * FROM tool_execution_pending 
        WHERE request_id = #{requestId}
        """)
    ToolExecutionPending findByRequestId(@Param("requestId") String requestId);
    
    /**
     * 根据 userId 和 status 查询待确认列表
     */
    @Select("""
        SELECT * FROM tool_execution_pending 
        WHERE user_id = #{userId} AND status = 'PENDING'
        ORDER BY created_at DESC
        """)
    List<ToolExecutionPending> findPendingByUserId(@Param("userId") Long userId);
    
    /**
     * 更新状态
     */
    @Update("""
        UPDATE tool_execution_pending 
        SET status = #{status}, 
            edited_data = #{editedData},
            executed_at = #{executedAt}
        WHERE request_id = #{requestId}
        """)
    int updateStatus(@Param("requestId") String requestId, 
                     @Param("status") String status,
                     @Param("editedData") String editedData,
                     @Param("executedAt") LocalDateTime executedAt);
    
    /**
     * 删除过期记录
     */
    @Delete("""
        DELETE FROM tool_execution_pending 
        WHERE expires_at < #{expireTime}
        """)
    int deleteExpired(@Param("expireTime") LocalDateTime expireTime);
    
    /**
     * 删除指定 requestId 的记录
     */
    @Delete("""
        DELETE FROM tool_execution_pending 
        WHERE request_id = #{requestId}
        """)
    int deleteByRequestId(@Param("requestId") String requestId);
}
