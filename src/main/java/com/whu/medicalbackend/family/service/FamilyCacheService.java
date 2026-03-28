package com.whu.medicalbackend.family.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.medicalbackend.family.dto.FamilyMemberVO;
import com.whu.medicalbackend.agent.service.serviceImpl.RedisService;
import com.whu.medicalbackend.user.entity.User;
import com.whu.medicalbackend.common.exception.BusinessException;
import com.whu.medicalbackend.family.mapper.FamilyMemberMapper;
import com.whu.medicalbackend.user.mapper.UserMapper;
import com.whu.medicalbackend.common.util.RedisKeyBuilderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class FamilyCacheService {
    @Autowired private RedisService redisService;
    @Autowired private FamilyMemberMapper   memberMapper;
    @Autowired private UserMapper           userMapper;
    @Autowired private ObjectMapper         objectMapper;

    /**
     * 将单个成员信息同步到 Redis Hash
     * 场景：创建家庭成功、审批通过、邀请成功后实时调用
     */
    public void syncSingleMemberToCache(Long groupId, Long userId) throws JsonProcessingException {
        User user = userMapper.findByUserId(userId);
        if(user == null) return;

        String role = memberMapper.getRoleInGroup(groupId, userId);
        if(role == null) return;

        FamilyMemberVO vo = new FamilyMemberVO.FamilyMemberVOBuilder()
                .setUserId(userId)
                .setUserName(user.getUsername())
                .setUserNickname(user.getNickname())
                .setRole(role)
                .setStatus("active")
                .build();

        // 使用工具类构建 Key: family:members:{groupId}
        String key = RedisKeyBuilderUtil.getMemberHashKey(groupId);
        redisService.putWithHash(key, userId.toString(), objectMapper.writeValueAsString(vo));
    }

    /**
     * 强制刷新整个家庭组的成员缓存
     * 场景：家庭组初始化、数据大幅度变更同步
     */
    public void refreshMemberCache(Long groupId) {
        // 使用工具类构建 Key
        String key = RedisKeyBuilderUtil.getMemberHashKey(groupId);

        // 删除旧缓存
        redisService.delete(key);

        // 获取该组所有激活状态的成员ID
        List<Long> userIds = memberMapper.findActiveUserIdsByGroupId(groupId);

        userIds.forEach(userId -> {
            try {
                syncSingleMemberToCache(groupId, userId);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 从缓存移除单个成员
     * 场景：成员退出家庭组时调用
     */
    public void removeMemberFromCache(Long groupId, Long userId) {
        // 使用工具类构建 Key
        String key = RedisKeyBuilderUtil.getMemberHashKey(groupId);
        redisService.deleteWithHash(key, userId.toString());
    }

    /**
     * 获取 Redis 中的家庭成员列表（供 Service 调用返回前端）
     */
    public Set<Object> getGroupMemberIds(Long groupId) {
        // 使用工具类构建 Key
        String key = RedisKeyBuilderUtil.getMemberHashKey(groupId);
        return redisService.keysWithHash(key);
    }

    /**
     * 获取 Redis 缓存中存储的成员信息
     */
    public List<FamilyMemberVO> getFamilyMembers(Long groupId){
        String key = RedisKeyBuilderUtil.getMemberHashKey(groupId);
        Set<Object> memberIds = redisService.keysWithHash(key);

        return memberIds.stream()
                .map(o -> (String) redisService.getWithHash(key ,(String) o))
                .map(jsonStr -> {
                    try {
                        return objectMapper.readValue(jsonStr, FamilyMemberVO.class);
                    } catch (JsonProcessingException e) {
                        throw new BusinessException("读取redis中的家庭成员信息出错" + e.getMessage());
                    }
                })
                .toList();
    }
}