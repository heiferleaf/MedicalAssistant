package com.whu.medicalbackend.common.infra.delay.handler;

import com.whu.medicalbackend.agent.service.serviceImpl.RedisService;
import com.whu.medicalbackend.common.enumField.InviteStatus;
import com.whu.medicalbackend.common.infra.delay.DelayTask;
import com.whu.medicalbackend.common.infra.delay.DelayTaskHandler;
import com.whu.medicalbackend.common.util.RedisKeyBuilderUtil;
import com.whu.medicalbackend.family.entity.FamilyInviteApply;
import com.whu.medicalbackend.family.mapper.FamilyInviteApplyMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class InviteExpireHandler implements DelayTaskHandler {

    private static final Logger logger = LoggerFactory.getLogger(InviteExpireHandler.class);

    private final FamilyInviteApplyMapper applyMapper;
    private final RedisService redisService;

    public InviteExpireHandler(FamilyInviteApplyMapper applyMapper, RedisService redisService) {
        this.applyMapper = applyMapper;
        this.redisService = redisService;
    }

    @Override
    public boolean supports(String taskType) {
        return "family.invite.expire".equals(taskType);
    }

    @Override
    public void handle(DelayTask task) {
        Long applyId = Long.parseLong(task.getBizId());

        String lockKey = RedisKeyBuilderUtil.getFamilyApproveLockKey(applyId);
        if (redisService.tryLock(lockKey, 2, 5)) {
            try {
                FamilyInviteApply apply = applyMapper.selectById(applyId);
                if (apply != null && InviteStatus.pending.equals(apply.getStatus())) {
                    apply.setStatus(InviteStatus.expired);
                    apply.setDealTime(LocalDateTime.now());
                    applyMapper.updateStatus(apply);
                    logger.info("邀请/申请记录 {} 已自动过期", applyId);
                }
            } finally {
                redisService.unlock(lockKey);
            }
        }
    }
}
