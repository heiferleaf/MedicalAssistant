package com.whu.medicalbackend.common.infra.delay;

public interface DelayTaskHandler {

    boolean supports(String taskType);

    void handle(DelayTask task) throws Exception;
}
