package com.whu.medicalbackend.common.infra.delay;

public interface DelayTaskPublisher {

    void publish(DelayTask task);

    void cancel(String taskType, String bizId);
}
