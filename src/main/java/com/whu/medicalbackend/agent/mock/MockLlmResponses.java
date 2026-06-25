package com.whu.medicalbackend.agent.mock;

import java.util.Random;

class MockLlmResponses {

    private static final Random RANDOM = new Random();

    static final String[] RESPONSES = {
        "根据您的症状描述，建议您注意休息，多喝水。如果症状持续或加重，请及时就医。",
        "这种情况可能与多种因素有关，包括饮食、作息、压力等。建议您保持良好的生活习惯，必要时咨询专业医生。",
        "您提到的药物具有一定的副作用，使用时需要注意剂量和禁忌症。建议在医生指导下使用。",
        "根据临床指南，这种症状通常需要进行相关检查以明确诊断。建议您到医院进行详细检查。",
        "您的情况属于常见症状，一般通过调整生活方式和适当用药可以改善。如有疑问请咨询医生。",
        "这类药物的作用机制是通过抑制某些生理过程来达到治疗效果。具体用法用量请遵医嘱。",
        "根据您的描述，可能需要进行血液检查、影像学检查等来辅助诊断。建议尽快就医。",
        "这种情况在临床上比较常见，通常与年龄、体质、环境等因素有关。建议定期体检，监测相关指标。"
    };

    static String random() {
        return RESPONSES[RANDOM.nextInt(RESPONSES.length)];
    }

    static void simulateLatency(int delayMs) {
        if (delayMs <= 0) return;
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
