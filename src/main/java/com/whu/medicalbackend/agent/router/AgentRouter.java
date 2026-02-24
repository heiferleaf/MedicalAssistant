package com.whu.medicalbackend.agent.router;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgentRouter {

    private static final List<String> REMINDER_KEYWORDS = List.of("提醒", "闹钟", "定时", "每天", "每日", "设置", "新增", "创建", "添加");

    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(?<h>\\d{1,2})(?:\\s*[:：]\\s*(?<m>\\d{1,2}))?\\s*(?:点|时)?\\s*(?<m2>\\d{1,2})?\\s*(?:分)?"
    );

    private static final Pattern MED_PATTERN_A = Pattern.compile(
            "(?:请|帮我|麻烦|我要|我想|给我)?\\s*(?:设置|创建|新增|添加|设定|定|定个|新建)\\s*(?:一条|一个|个|条)?\\s*(?<med>[\\u4e00-\\u9fffA-Za-z0-9\\-\\s]{1,32}?)\\s*(?:每天|每日|每晚|每早|每周|提醒|闹钟)"
    );

    private static final Pattern MED_PATTERN_B = Pattern.compile(
            "(?<med>[\\u4e00-\\u9fffA-Za-z0-9\\-\\s]{1,32}?)\\s*(?:每天|每日|每晚|每早|每周)\\s*.*?(?:提醒|闹钟)"
    );

    private static final Pattern MED_PATTERN_C = Pattern.compile(
            "提醒\\s*(?<med>[\\u4e00-\\u9fffA-Za-z0-9\\-\\s]{1,32})"
    );

    public record Decision(String intent, Map<String, Object> toolArgs, boolean needConfirm, String clarification) {
    }

    public static Decision routeMessage(String message, String userId, String sessionId) {
        String text = (message == null ? "" : message).trim();
        if (text.isEmpty()) {
            return new Decision("other", Map.of(), false, "请输入内容。");
        }

        if (looksLikeReminder(text)) {
            String medicineName = parseMedicineName(text);
            String timeOfDay = parseTimeOfDay(text);

            if ((medicineName == null || medicineName.isBlank()) && (timeOfDay == null || timeOfDay.isBlank())) {
                return new Decision("spring.plan.create.preview", Map.of(), false, "你想提醒的药名和时间是什么？例如：阿司匹林 每天 20:00。");
            }
            if (medicineName == null || medicineName.isBlank()) {
                return new Decision("spring.plan.create.preview", Map.of(), false, "你想提醒的是哪种药？例如：阿司匹林。");
            }
            if (timeOfDay == null || timeOfDay.isBlank()) {
                return new Decision("spring.plan.create.preview", Map.of(), false, "你希望每天几点提醒？例如：20:00。");
            }

            Map<String, Object> toolArgs = new LinkedHashMap<>();
            toolArgs.put("user_id", userId);
            toolArgs.put("medicineName", medicineName);
            toolArgs.put("dosage", "按医嘱");
            toolArgs.put("startDate", LocalDate.now().toString());
            toolArgs.put("endDate", null);
            toolArgs.put("timePoints", List.of(timeOfDay));
            toolArgs.put("remark", null);

            return new Decision("spring.plan.create.preview", toolArgs, true, null);
        }

        return new Decision("rag.query", Map.of("question", text), false, null);
    }

    private static boolean looksLikeReminder(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        boolean hasAny = REMINDER_KEYWORDS.stream().anyMatch(text::contains);
        boolean hasTrigger = text.contains("提醒") || text.contains("闹钟");
        return hasAny && hasTrigger;
    }

    private static String parseTimeOfDay(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = TIME_PATTERN.matcher(text);
        if (!m.find()) {
            return null;
        }
        int hour = Integer.parseInt(m.group("h"));
        Integer minute = null;
        if (m.group("m") != null) {
            minute = Integer.parseInt(m.group("m"));
        } else if (m.group("m2") != null) {
            minute = Integer.parseInt(m.group("m2"));
        } else {
            minute = 0;
        }

        if (text.contains("下午") || text.contains("晚上") || text.contains("夜里") || text.contains("夜间")) {
            if (hour >= 1 && hour <= 11) {
                hour += 12;
            }
        }
        if (text.contains("中午")) {
            if (hour == 0) {
                hour = 12;
            } else if (hour >= 1 && hour <= 10) {
                hour += 12;
            }
        }

        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            return null;
        }
        return String.format("%02d:%02d", hour, minute);
    }

    private static String parseMedicineName(String text) {
        if (text == null) {
            return null;
        }

        String med = tryMatchAndClean(MED_PATTERN_A, text);
        if (med != null) {
            return med;
        }

        med = tryMatchAndClean(MED_PATTERN_B, text);
        if (med != null) {
            return med;
        }

        med = tryMatchAndClean(MED_PATTERN_C, text);
        return med;
    }

    private static String tryMatchAndClean(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text.trim());
        if (!m.find()) {
            return null;
        }
        String med = m.group("med");
        if (med == null) {
            return null;
        }
        med = cleanMedicine(med);
        return med.isBlank() ? null : med;
    }

    private static String cleanMedicine(String med) {
        String s = med == null ? "" : med.trim();
        s = s.replaceAll("^(?:请|帮我|麻烦|我要|我想|给我)?", "").trim();
        s = s.replaceAll("^(?:设置|创建|新增|添加|设定|定|定个|建|新建)", "").trim();
        s = s.replaceAll("^(?:一条|一个|个|条)", "").trim();
        s = s.replaceAll("^(?:用药)?(?:提醒|闹钟)", "").trim();
        return s;
    }
}
