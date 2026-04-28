package com.whu.medicalbackend.common.infra.mq;

public final class MqNames {

    private MqNames() {
    }

    public static final String EXCHANGE_DOMAIN = "medical.domain.topic";
    public static final String EXCHANGE_DELAY = "medical.delay.topic";
    public static final String EXCHANGE_DEAD_LETTER = "medical.dlx.topic";

    public static final String QUEUE_DOMAIN_EVENTS = "queue.domain.events";
    public static final String QUEUE_WS_PUSH = "queue.ws.push";
    public static final String QUEUE_AI_TASK = "queue.ai.task";
    public static final String QUEUE_CACHE_INVALIDATE = "queue.cache.invalidate";

    public static final String QUEUE_DELAY_MEDICATION_REMIND = "queue.delay.medication.remind";
    public static final String QUEUE_DELAY_MEDICATION_MISSED = "queue.delay.medication.missed";
    public static final String QUEUE_DELAY_FAMILY_INVITE = "queue.delay.family.invite";
    public static final String QUEUE_DEAD_LETTER = "queue.dead.letter";

    public static final String ROUTING_MEDICATION_EVENTS = "medication.#";
    public static final String ROUTING_FAMILY_EVENTS = "family.#";
    public static final String ROUTING_HEALTH_EVENTS = "health.#";
    public static final String ROUTING_AGENT_EVENTS = "agent.#";
    public static final String ROUTING_WS_PUSH = "ws.push.*";
    public static final String ROUTING_AI_TASK = "ai.task.*";
    public static final String ROUTING_CACHE_INVALIDATE = "cache.invalidate.*";

    public static final String ROUTING_DELAY_MEDICATION_REMIND = "medication.remind";
    public static final String ROUTING_DELAY_MEDICATION_MISSED = "medication.missed";
    public static final String ROUTING_DELAY_FAMILY_INVITE = "family.invite.expire";
    public static final String ROUTING_DEAD_LETTER = "#";
}
