package com.whu.medicalbackend.common.infra.push;

public interface WsPushPublisher {

    void pushToUser(WsPushCommand command);
}
