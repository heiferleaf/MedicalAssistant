package com.whu.medicalbackend.common.infra.push;

import com.whu.medicalbackend.ws.event.FamilyPushEvent;

public interface FamilyPushEventHook {

    void handle(FamilyPushEvent event);
}
