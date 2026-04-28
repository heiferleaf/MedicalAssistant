package com.whu.medicalbackend.common.infra.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.medicalbackend.common.infra.idempotency.MessageIdempotencyService;
import com.whu.medicalbackend.common.infra.mq.MqNames;
import com.whu.medicalbackend.ws.WsPubSubBroadcaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "service.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WsPushMessageListener {

    private static final Duration RUNNING_TTL = Duration.ofMinutes(5);
    private static final Duration DONE_TTL = Duration.ofDays(3);

    private final ObjectMapper objectMapper;
    private final WsPubSubBroadcaster broadcaster;
    private final MessageIdempotencyService idempotencyService;

    public WsPushMessageListener(
            ObjectMapper objectMapper,
            WsPubSubBroadcaster broadcaster,
            MessageIdempotencyService idempotencyService
    ) {
        this.objectMapper = objectMapper;
        this.broadcaster = broadcaster;
        this.idempotencyService = idempotencyService;
    }

    @RabbitListener(queues = MqNames.QUEUE_WS_PUSH)
    public void onWsPush(Message message) throws Exception {
        WsPushCommand command = objectMapper.readValue(message.getBody(), WsPushCommand.class);
        command.ensureMetadata();

        String idempotencyKey = "ws-push:" + command.getCommandId();
        if (idempotencyService.isDone(idempotencyKey)) {
            return;
        }
        if (!idempotencyService.tryStart(idempotencyKey, RUNNING_TTL)) {
            return;
        }

        try {
            String payload = command.getJsonPayload();
            if (!StringUtils.hasText(payload)) {
                payload = objectMapper.writeValueAsString(command.getPayload());
            }
            broadcaster.pushToUser(command.getUserId(), payload, command.getGroupId() == null ? 0L : command.getGroupId());
            idempotencyService.markDone(idempotencyKey, DONE_TTL);
        } catch (Exception ex) {
            idempotencyService.clearRunning(idempotencyKey);
            log.error("WebSocket 推送命令执行失败: commandId={}, userId={}",
                    command.getCommandId(), command.getUserId(), ex);
            throw ex;
        }
    }
}
