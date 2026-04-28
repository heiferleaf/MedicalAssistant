package com.whu.medicalbackend.common.infra.hook;

import com.whu.medicalbackend.common.infra.event.DomainEvent;
import com.whu.medicalbackend.common.infra.idempotency.MessageIdempotencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class DomainEventHookExecutor {

    private static final Duration RUNNING_TTL = Duration.ofMinutes(10);
    private static final Duration DONE_TTL = Duration.ofDays(7);

    private final List<DomainEventHook> hooks;
    private final MessageIdempotencyService idempotencyService;

    public DomainEventHookExecutor(List<DomainEventHook> hooks, MessageIdempotencyService idempotencyService) {
        this.hooks = hooks.stream()
                .sorted(Comparator.comparingInt(DomainEventHook::order))
                .toList();
        this.idempotencyService = idempotencyService;
    }

    public void execute(DomainEvent event) {
        for (DomainEventHook hook : hooks) {
            if (!hook.supports(event)) {
                continue;
            }

            String idempotencyKey = event.getEventId() + ":" + hook.hookName();
            if (idempotencyService.isDone(idempotencyKey)) {
                log.debug("hook 已处理，跳过: hook={}, eventId={}", hook.hookName(), event.getEventId());
                continue;
            }
            if (!idempotencyService.tryStart(idempotencyKey, RUNNING_TTL)) {
                log.debug("hook 正在处理中，跳过本次: hook={}, eventId={}", hook.hookName(), event.getEventId());
                continue;
            }

            try {
                hook.handle(event);
                idempotencyService.markDone(idempotencyKey, DONE_TTL);
            } catch (Exception ex) {
                idempotencyService.clearRunning(idempotencyKey);
                log.error("hook 执行失败: hook={}, eventType={}, eventId={}",
                        hook.hookName(), event.getEventType(), event.getEventId(), ex);
                throw new IllegalStateException("domain event hook failed: " + hook.hookName(), ex);
            }
        }
    }
}
