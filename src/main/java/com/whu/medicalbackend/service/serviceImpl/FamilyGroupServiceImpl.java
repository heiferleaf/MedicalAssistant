package com.whu.medicalbackend.service.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.medicalbackend.dto.*;
import com.whu.medicalbackend.entity.*;
import com.whu.medicalbackend.enumField.EventLogEnum;
import com.whu.medicalbackend.enumField.FamilyEventEnum;
import com.whu.medicalbackend.enumField.InviteStatus;
import com.whu.medicalbackend.enumField.InviteType;
import com.whu.medicalbackend.exception.BusinessException;
import com.whu.medicalbackend.mapper.*;
import com.whu.medicalbackend.schedule.DynamicTaskScheduler;
import com.whu.medicalbackend.service.FamilyGroupService;
import com.whu.medicalbackend.util.RedisKeyBuilderUtil; // 引入工具类
import com.whu.medicalbackend.ws.event.FamilyMedicineUpdateEvent;
import com.whu.medicalbackend.ws.event.FamilyMemberUpdateEvent;
import com.whu.medicalbackend.ws.event.FamilyPushEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
public class FamilyGroupServiceImpl implements FamilyGroupService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private FamilyGroupMapper groupMapper;

    @Autowired
    private FamilyMemberMapper memberMapper;

    @Autowired
    private FamilyInviteApplyMapper applyMapper;

    @Autowired
    private FamilyEventLogMapper eventLogMapper;

    @Autowired
    private FamilyCacheService familyCacheService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MedicationTaskMapper taskMapper;

    @Autowired
    private HealthDataMapper healthDataMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private static final Logger logger = LoggerFactory.getLogger(FamilyGroupServiceImpl.class);
    @Autowired
    private DynamicTaskScheduler dynamicTaskScheduler;

    /**
     * 1.1 创建家庭组
     */
    @Override
    public FamilyCreateVO createGroup(Long userId, String groupName, String description)
    {
        // 使用工具类构建分布式锁 Key
        String lockKey = RedisKeyBuilderUtil.getFamilyCreateLockKey(userId);

        boolean isLocked = redisService.tryLock(lockKey, 5, 10);
        if (!isLocked) {
            throw new BusinessException("操作处理中，请勿重复提交");
        }

        try {
            if (memberMapper.checkUserInGroup(userId)) {
                throw new BusinessException("您已加入一个家庭组，无法创建新组");
            }

            FamilyGroup group = new FamilyGroup();
            group.setGroupName(groupName);
            group.setOwnerUserId(userId);
            group.setDescription(description);
            group.setCreateTime(LocalDateTime.now());
            group.setUpdateTime(LocalDateTime.now());
            groupMapper.insert(group);

            FamilyMember leader = new FamilyMember();
            leader.setGroupId(group.getId());
            leader.setUserId(userId);
            leader.setRole("leader");
            leader.setJoinTime(LocalDateTime.now());
            leader.setStatus("active");
            memberMapper.insert(leader);

            familyCacheService.syncSingleMemberToCache(group.getId(), userId);
            eventLogMapper.insertLog(group.getId(), userId, EventLogEnum.INFO.name(), "创建家庭组");

            return new FamilyCreateVO.FamilyCreateVOBuilder()
                    .fromFamilyGroup(group)
                    .setOwnerUserNickname(userMapper.findByUserId(userId).getNickname())
                    .build();

        } catch (JsonProcessingException e) {
            throw new BusinessException("写入缓存出错");
        } finally {
            redisService.unlock(lockKey);
        }
    }

    /**
     * 1.2 发出加入家庭组的申请
     */
    @Override
    public void applyJoin(Long userId, Long groupId, String remark) {
        // 使用工具类构建分布式锁 Key
        String lockKey = RedisKeyBuilderUtil.getFamilyApplyLockKey(userId, groupId);
        if(!redisService.tryLock(lockKey, 5, 10)) {
            throw new BusinessException("申请处理中");
        }

        try {
            if(memberMapper.checkUserInGroup(userId)) {
                throw new BusinessException("当前已有家庭组");
            }
            FamilyGroup group = groupMapper.selectById(groupId);
            if(group == null) {
                throw new BusinessException("目标家庭组不存在");
            }

            // 使用工具类构建 48 小时频率限制 Key
            String limitedKey = RedisKeyBuilderUtil.getFamilyApplyLimitKey(userId, groupId);
            if(redisService.get(limitedKey) != null) {
                throw new BusinessException("48 小时内只能向同一小组发出一次申请");
            }

            if (applyMapper.hasPendingRecord(groupId, userId, InviteType.apply)) {
                throw new BusinessException("已有待处理的申请，等待组长审批");
            }

            FamilyInviteApply apply = new FamilyInviteApply();
            apply.setGroupId(groupId);
            apply.setInviterId(null);
            apply.setInviteeId(userId);
            apply.setType(InviteType.apply);
            apply.setStatus(InviteStatus.pending);
            apply.setCreateTime(LocalDateTime.now());
            apply.setExpireTime(LocalDateTime.now().plusDays(2));
            apply.setRemark(remark);
            applyMapper.insert(apply);

            redisService.setWithExpire(limitedKey, "1", 48, TimeUnit.HOURS);
            dynamicTaskScheduler.addInviteExpireTask(apply.getId(), apply.getExpireTime());

        } finally {
            redisService.unlock(lockKey);
        }
    }

    /**
     * 1.3 发出加入家庭组的邀请
     * @param leaderId
     * @param inviteePhone
     * @param remark
     */
    @Override
    public void inviteMember(Long leaderId, String inviteePhone, String remark) {
        // 基础校验，leader用户是否是每一个家庭组的组长
        Long groupId = memberMapper.getGroupIdByUserId(leaderId);
        if(groupId == null) throw new BusinessException("您不在任何家庭组中，无法发送邀请");
        String role = memberMapper.getRoleInGroup(groupId, leaderId);
        if(!role.equals("leader")) throw new BusinessException("您不是组长，无法邀请");

        String lockKey = RedisKeyBuilderUtil.getFamilyInviteLockKey(inviteePhone, groupId);
        if(!redisService.tryLock(lockKey, 5, 10)) {
            throw new BusinessException("邀请处理中");
        }

        try {
            // 冗余校验
            User invitee = userMapper.findByPhoneNumber(inviteePhone);
            if(invitee == null) { throw new BusinessException("邀请的用户不存在"); }

            if (memberMapper.checkUserInGroup(invitee.getId())) {
                throw new BusinessException("该用户已经加入其他家庭组");
            }

            String limitedKey = RedisKeyBuilderUtil.getFamilyInviteLimitKey(inviteePhone, groupId);
            if(redisService.get(limitedKey) != null) {
                throw new BusinessException("48 小时内只能向该用户发起一次邀请");
            }

            if (applyMapper.hasPendingRecord(groupId, invitee.getId(), InviteType.invite)) {
                throw new BusinessException("已有待处理的邀请，请勿重复发起");
            }

            // 6. 插入申请/邀请表
            FamilyInviteApply invite = new FamilyInviteApply();
            invite.setGroupId(groupId);
            invite.setInviterId(leaderId);
            invite.setInviteeId(invitee.getId());
            invite.setType(InviteType.invite);
            invite.setStatus(InviteStatus.pending);
            invite.setCreateTime(LocalDateTime.now());
            invite.setExpireTime(LocalDateTime.now().plusDays(2)); // 48小时过期
            invite.setRemark(remark);
            applyMapper.insert(invite);

            redisService.setWithExpire(limitedKey, "1", 48, TimeUnit.HOURS);
            dynamicTaskScheduler.addInviteExpireTask(invite.getId(), invite.getExpireTime());
        } finally {
            redisService.unlock(lockKey);
        }
    }

    /**
     * 1.4 处理申请/邀请
     */
    @Override
    public void approveApply(Long applyId, Long userId, String opType, String remark){
        // 使用工具类构建审批锁 Key
        String lockKey = RedisKeyBuilderUtil.getFamilyApproveLockKey(applyId);
        if (!redisService.tryLock(lockKey, 5, 10)) {
            throw new BusinessException("系统繁忙，请稍后再试");
        }

        try {
            FamilyInviteApply apply = applyMapper.selectById(applyId);
            if (apply == null || (!InviteStatus.pending.equals(apply.getStatus()) && !InviteStatus.expired.equals(apply.getStatus()))) {
                throw new BusinessException("该申请不存在或已过期或已被处理");
            }

            // 自己同意自己的申请
            if(apply.getInviteeId().equals(userId)) {
                throw new BusinessException("无法同意自己的申请");
            }

            if ("reject".equals(opType)) {
                apply.setStatus(InviteStatus.rejected);
                apply.setDealTime(LocalDateTime.now());
                applyMapper.updateStatus(apply);
                eventLogMapper.insertLog(apply.getGroupId(), apply.getInviteeId(), EventLogEnum.INFO.name(), "加入申请被拒绝");
                return;
            }

            if ("accept".equals(opType)) {
                if (memberMapper.checkUserInGroup(apply.getInviteeId())) {
                    throw new BusinessException("该用户已加入其他家庭组");
                }

                apply.setStatus(InviteStatus.accepted);
                apply.setDealTime(LocalDateTime.now());
                applyMapper.updateStatus(apply);

                FamilyMember member = new FamilyMember();
                member.setGroupId(apply.getGroupId());
                member.setUserId(apply.getInviteeId());
                member.setRole("normal");
                member.setJoinTime(LocalDateTime.now());
                member.setStatus("active");
                memberMapper.insert(member);

                logger.info("处理apply类型：{} success", apply.getType());

                String logDesc = (apply.getType() == InviteType.apply) ? "通过申请加入小组" : "接受邀请加入小组";
                eventLogMapper.insertLog(apply.getGroupId(), apply.getInviteeId(), EventLogEnum.INFO.name(), logDesc);

                // 更新家庭组的缓存，对成员的加入进行广播
                try {
                    familyCacheService.syncSingleMemberToCache(apply.getGroupId(), apply.getInviteeId());
                } catch (JsonProcessingException e) {
                    throw new BusinessException("缓存数据更新出错", e);
                }
                publishJoinEvent(apply.getGroupId(), apply.getInviteeId());
            }

            // 取消定时任务
            dynamicTaskScheduler.cancelInviteExpireTask(applyId);

        } finally {
            redisService.unlock(lockKey);
        }
    }

    /**
     * 1.5 获取家庭健康数据快照
     * @param groupId
     * @return
     */
    @Override
    public FamilyHealthSnapshotVO getFamilyHealthSnapshot(Long groupId) {
        String dateStr = LocalDate.now().toString();
        // 使用工具类构建快照数据 Key
        String snapshotKey = RedisKeyBuilderUtil.getFamilySnapshotKey(groupId, dateStr);

        String cachedData = redisService.get(snapshotKey);
        if (cachedData != null) {
            try {
                return objectMapper.readValue(cachedData, FamilyHealthSnapshotVO.class);
            } catch (JsonProcessingException e) {
                logger.error("快照解析失败", e);
            }
        }

        // 使用工具类构建快照分布式锁 Key
        String lockKey = RedisKeyBuilderUtil.getFamilySnapshotLockKey(groupId);
        if (redisService.tryLock(lockKey, 3, 10)) {
            try {
                cachedData = redisService.get(snapshotKey);
                if (cachedData != null) {
                    return objectMapper.readValue(cachedData, FamilyHealthSnapshotVO.class);
                }

                FamilyHealthSnapshotVO snapshot = new FamilyHealthSnapshotVO();
                snapshot.setGroupId(groupId);
                snapshot.setUpdateTime(LocalDateTime.now());

                List<FamilyHealthSnapshotVO.MemberHealthDetail> details = new ArrayList<>();

                Set<Object> userIds = familyCacheService.getGroupMemberIds(groupId);

                // 构建成员缓存的 Hash Key 供下文读取 Field
                String memberHashKey = RedisKeyBuilderUtil.getMemberHashKey(groupId);

                userIds.forEach(idObj -> {
                    Long uid = Long.parseLong(idObj.toString());
                    FamilyHealthSnapshotVO.MemberHealthDetail detail = new FamilyHealthSnapshotVO.MemberHealthDetail();
                    detail.setUserId(uid);

                    // 从成员 Hash 缓存读取
                    String memberInfoJson = (String) redisService.getWithHash(memberHashKey, uid.toString());
                    try {
                        FamilyMemberVO memberVO = objectMapper.readValue(memberInfoJson, FamilyMemberVO.class);
                        detail.setNickname(memberVO.getUserNickname());
                    } catch (Exception e) {
                        logger.warn("解析成员缓存失败: userId={}", uid);
                    }

                    HealthData latestHealth = healthDataMapper.findLatestByUserId(uid);
                    if (latestHealth != null) {
                        detail.setLastHeartRate(latestHealth.getHeartRate());
                        detail.setLastBloodPressure(latestHealth.getBloodPressure());
                        detail.setHealthUpdateTime(latestHealth.getMeasureTime());
                    }

                    int completed = taskMapper.countStatusByDate(uid, LocalDate.now(), 1);
                    int total = taskMapper.countTotalByDate(uid, LocalDate.now());
                    detail.setCompletedTasks(completed);
                    detail.setTotalTasks(total);

                    details.add(detail);
                });
                snapshot.setMembers(details);

                redisService.setWithExpire(snapshotKey, objectMapper.writeValueAsString(snapshot), 300, TimeUnit.SECONDS);
                return snapshot;

            } catch (Exception e) {
                throw new BusinessException("聚合健康快照失败", e);
            } finally {
                redisService.unlock(lockKey);
            }
        } else {
            throw new BusinessException("系统繁忙，请刷新重试");
        }
    }

    /**
     * 1.6 成员退出家庭组
     * @param groupId
     * @param userId
     */
    @Override
    public void leaveGroup(Long groupId, Long userId) {
        // 使用工具类构建退出锁 Key
        String lockKey = RedisKeyBuilderUtil.getFamilyLeaveLockKey(userId);
        if (!redisService.tryLock(lockKey, 5, 10)) {
            throw new BusinessException("操作处理中，请稍后");
        }

        try {
            String role = memberMapper.getRoleInGroup(groupId, userId);
            if ("leader".equals(role)) {
                throw new BusinessException("组长不能直接退出，请先解散家庭组或转让权限");
            }

            LocalDateTime now = LocalDateTime.now();
            int rows = memberMapper.updateMemberStatus(groupId, userId, "quit", now);
            if (rows == 0) {
                throw new BusinessException("退出失败：您不在此家庭组或已处于退出状态");
            }

            eventLogMapper.insertLog(groupId, userId, EventLogEnum.INFO.name(), "成员主动退出小组");

            // 同步移除缓存
            familyCacheService.removeMemberFromCache(groupId, userId);

            // 使用工具类构建健康数据快照 Key 并废弃
            String dateStr = LocalDate.now().toString();
            String snapshotKey = RedisKeyBuilderUtil.getFamilySnapshotKey(groupId, dateStr);
            redisService.delete(snapshotKey);

            publishQuitEvent(groupId, userId);

        } finally {
            redisService.unlock(lockKey);
        }
    }

    @Override
    public FamilyDetailVO getMyGroupDetails(Long userId) {
        Long groupId = memberMapper.getGroupIdByUserId(userId);
        if(groupId == null) {
            throw new BusinessException("当前不在任何组中");
        }

        FamilyGroup group = groupMapper.selectById(groupId);
        String ownerNickname = userMapper.findByUserId(group.getOwnerUserId()).getNickname();

        List<FamilyMemberVO> memberInfos = familyCacheService.getFamilyMembers(groupId);

        FamilyDetailVO.GroupInfo groupInfo = new FamilyDetailVO.GroupInfo();
        groupInfo.setGroupName(group.getGroupName());
        groupInfo.setGroupId(groupId);
        groupInfo.setDescription(group.getDescription());
        groupInfo.setOwnerUserNickname(ownerNickname);

        FamilyDetailVO familyDetailVO = new FamilyDetailVO();
        familyDetailVO.setMembers(memberInfos);
        familyDetailVO.setGroup(groupInfo);

        return familyDetailVO;
    }


    @Override
    public List<FamilyAlarmVO> getTodayAlarms(Long groupId) {
        String cacheKey = RedisKeyBuilderUtil.getFamilyAlarmKey(groupId, LocalDate.now().toString());

        String cachedData = redisService.get(cacheKey);
        if (cachedData != null) {
            try {
                return objectMapper.readValue(cachedData, new TypeReference<List<FamilyAlarmVO>>(){});
            } catch (JsonProcessingException e) {
                throw new BusinessException("获取数据出错", e);
            }
        }

        String lockKey = RedisKeyBuilderUtil.getFamilyAlarmsLockKey(groupId);
        if (redisService.tryLock(lockKey, 5, 10)) {
            try {
                if ((cachedData = redisService.get(cacheKey)) != null) {
                    return objectMapper.readValue(cachedData, new TypeReference<List<FamilyAlarmVO>>(){});
                }

                List<FamilyAlarmVO> alarms = eventLogMapper.findDailyAlarms(groupId);
                if (alarms != null) {
                    redisService.setWithExpire(cacheKey, objectMapper.writeValueAsString(alarms), 300, TimeUnit.SECONDS);
                }

                return alarms;

            } catch (JsonProcessingException e) {
                throw new BusinessException("获取数据出错", e);
            } finally {
                redisService.unlock(lockKey);
            }
        } else {
            throw new BusinessException("系统繁忙，稍后再试");
        }
    }

    /**
     * 2.1 & 2.2 查看本账号所有申请历史/收到的邀请
     */
    @Override
    public List<FamilyInviteApplyVO> getMyApplyRecords(Long userId) {
        // 直接调用 Mapper 关联查询结果
        return applyMapper.findUserRecords(userId);
    }

    /**
     * 2.3 组长审批待处理申请列表
     */
    @Override
    public List<FamilyInviteApplyVO> getPendingApplies(Long groupId, Long userId) {
        // 1. 安全校验：验证当前操作人是否是该组组长
        FamilyGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("家庭组不存在");
        }
        if (!group.getOwnerUserId().equals(userId)) {
            throw new BusinessException("权限不足，仅组长可查看审批列表");
        }

        // 2. 查询该组所有 type='apply' 且 status='pending' 的记录
        return applyMapper.findPendingAppliesByGroupId(groupId);
    }


    private void publishJoinEvent(Long groupId, Long userId) {
        User newUser = userMapper.findByUserId(userId);
        if(newUser == null) return;

        Map<String, Object> pushData = new HashMap<>();
        pushData.put("type", "join_success");
        pushData.put("targetUserId", userId);
        pushData.put("groupId", groupId);
        pushData.put("targetNickname", newUser.getNickname());

        eventPublisher.publishEvent(new FamilyMemberUpdateEvent(
                this,
                groupId,
                pushData
        ));
    }

    private void publishQuitEvent(Long groupId, Long userId) {
        User newUser = userMapper.findByUserId(userId);
        if(newUser == null) return;
        Map<String, Object> pushData = new HashMap<>();
        pushData.put("type", "member_leave");
        pushData.put("targetUserId", userId);
        pushData.put("groupId", groupId);
        pushData.put("targetNickname", newUser.getNickname());

        eventPublisher.publishEvent(new FamilyMemberUpdateEvent(
                this,
                groupId,
                pushData
        ));
    }
}