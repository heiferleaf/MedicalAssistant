package com.whu.medicalbackend.common.infra.hook;

import com.whu.medicalbackend.common.infra.event.DomainEvent;

public interface DomainEventHook {

    String hookName();

    boolean supports(DomainEvent event);

    void handle(DomainEvent event) throws Exception;

    default int order() {
        return 0;
    }
}
