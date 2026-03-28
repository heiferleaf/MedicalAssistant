package com.whu.medicalbackend.common.schedule;

import com.whu.medicalbackend.family.entity.FamilyInviteApply;
import com.whu.medicalbackend.medical.entity.MedicationTask;
import com.whu.medicalbackend.medical.entity.Medicine;
import com.whu.medicalbackend.family.mapper.FamilyEventLogMapper;
import com.whu.medicalbackend.family.mapper.FamilyInviteApplyMapper;
import com.whu.medicalbackend.family.mapper.FamilyMemberMapper;
import com.whu.medicalbackend.medical.mapper.MedicationTaskMapper;
import com.whu.medicalbackend.medical.mapper.MedicineMapper;
import com.whu.medicalbackend.user.entity.User;
import com.whu.medicalbackend.common.enumField.EventLogEnum;
import com.whu.medicalbackend.common.enumField.InviteStatus;
import com.whu.medicalbackend.agent.service.serviceImpl.RedisService;
import com.whu.medicalbackend.user.mapper.UserMapper;
import com.whu.medicalbackend.common.util.RedisKeyBuilderUtil;
import com.whu.medicalbackend.ws.event.FamilyMedicineAlarmEvent;
import com.whu.medicalbackend.ws.event.UserTaskMedicineRemindEvent;
import io.jsonwebtoken.lang.Assert;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation. Scheduled;
import org.springframework. stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 动态定时任务调度器
 *
 * 核心功能：
 * 1. 应用启动时，为今天所有未服用任务创建超时定时器
 * 2. 每天凌晨，刷新定时任务
 * 3. 用户操作任务时，动态取消定时器
 *
 * 设计模式：
 * 1. 单例模式：Spring Bean默认单例
 * 2. 观察者模式：定时器到期触发回调
 * 3. 策略模式：不同任务有不同的超时时间
 */
@Component
@Transactional(rollbackFor = Exception.class)
public class DynamicTaskScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DynamicTaskScheduler.class);

    @Autowired
    @Qualifier("myTaskScheduler")
    private TaskScheduler taskScheduler;

    @Autowired
    private MedicationTaskMapper taskMapper;

    @Autowired
    private FamilyInviteApplyMapper applyMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 存储任务ID与定时器的映射
     *
     * Java知识点：
     * - ConcurrentHashMap：线程安全，支持高并发读写
     * - ScheduledFuture<?>：泛型通配符，表示任意类型的Future
     *
     * 用途：
     * 1. 查询某个任务是否有定时器
     * 2. 取消某个任务的定时器
     * 3. 统计当前活跃的定时器数量
     */
    private final Map<Long, ScheduledFuture<?>> medicationTaskPool = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> remindTaskPool = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> inviteTaskPool = new ConcurrentHashMap<>();
    @Autowired
    private FamilyEventLogMapper familyEventLogMapper;
    @Autowired
    private FamilyMemberMapper familyMemberMapper;
    @Autowired
    private MedicineMapper medicineMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 应用启动时初始化
     *
     * Spring知识点：
     * @PostConstruct执行时机：
     * 1. 构造方法执行完毕
     * 2. @Autowired依赖注入完成
     * 3. @PostConstruct方法执行  ← 这里
     * 4. @Scheduled定时任务开始执行
     *
     * 为什么需要@PostConstruct？
     * - 确保taskScheduler和taskMapper已经注入
     * - 在第一次@Scheduled执行前初始化任务
     */
    @PostConstruct
    public void init() {
        logger.info("========================================");
        logger.info("【动态调度】应用启动，开始初始化定时任务");
        logger.info("========================================");
        scheduleTasksForToday();
    }

    /**
     * 每天凌晨00: 05执行：刷新定时任务
     *
     * Cron表达式：0 5 0 * * ?
     * - 0：第0秒
     * - 5：第5分钟
     * - 0：第0小时（凌晨）
     * - *：每天
     * - *：每月
     * - ? ：星期不指定
     *
     * 为什么是00:05而不是00:00？
     * 1. 避免与数据库备份、日志切割等凌晨任务冲突
     * 2. 给系统一点缓冲时间
     * 3. 如果00:00有任务生成计划，00:05可以捕获到
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void refreshDailyTasks() {
        logger.info("========================================");
        logger.info("【动态调度】每日刷新：{}", LocalDate.now());
        logger.info("========================================");

        // 1. 取消昨天的所有定时器
        cancelAllTasks();

        // 2. 创建今天的定时器
        scheduleTasksForToday();
    }

    /**
     * 为今天的所有未服用任务创建超时定时器
     *
     * 业务流程：
     * 1. 查询今天所有status=0的任务
     * 2. 遍历任务，为每个任务创建定时器
     * 3. 定时器触发时间：task_date + time_point + 30分钟
     */
    private void scheduleTasksForToday() {
        LocalDate today = LocalDate.now();

        // 1. 查询今天所有未服用的任务
        List<MedicationTask> tasks = taskMapper.findUncompletedTasksByDate(today);

        if (tasks.isEmpty()) {
            logger.info("【动态调度】今天没有未完成的任务");
            return;
        }

        logger.info("【动态调度】今天共有 {} 个未服用任务", tasks.size());

        // 2. 为每个任务创建定时器
        int scheduledCount = 0;
        int expiredCount = 0;

        for (MedicationTask task : tasks) {
            boolean scheduled = scheduleTaskTimeout(task);
            if (scheduled) {
                scheduledCount++;
            } else {
                expiredCount++;
            }
        }

        logger.info("【动态调度】创建定时器：{}个，已超时直接标记：{}个", scheduledCount, expiredCount);
    }

    /**
     * 为单个任务创建超时定时器
     *
     * @param task 任务对象
     * @return true=成功创建定时器，false=已超时直接标记
     *
     * Java知识点：
     * 1. LocalDateTime日期时间操作
     * 2. ZoneId时区转换
     * 3. Lambda表达式
     */
    private boolean scheduleTaskTimeout(MedicationTask task) {
        // 1. 计算超时时间点：任务日期 + 时间点 + 30分钟
        LocalDateTime taskTime = LocalDateTime.of(task.getTaskDate(), task.getTimePoint());
        LocalDateTime timeoutTime = taskTime.plusMinutes(2);

        // 2. 判断是否已经超时
        LocalDateTime now = LocalDateTime.now();
        if (timeoutTime.isBefore(now) || timeoutTime.isEqual(now)) {
            // 已经超时，直接标记为漏服
            logger.info("【动态调度】任务ID={} 已超时，直接标记漏服（计划时间：{}，超时时间：{}）",
                    task.getId(), taskTime, timeoutTime);
            markTaskAsMissed(task.getId());
            return false;
        }

        // 3. 转换为Date类型（TaskScheduler. schedule方法需要Date参数）
        Date executeTime = Date.from(timeoutTime.atZone(ZoneId.systemDefault()).toInstant());

        // 4. 创建定时任务
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> markTaskAsMissed(task.getId()),  // 任务内容
                executeTime  // 执行时间
        );

        // 5. 保存到Map（用于后续取消）
        medicationTaskPool.put(task.getId(), future);

        logger.debug("【动态调度】任务ID={} 定时器已创建，将在 {} 标记为漏服",
                task.getId(), timeoutTime);

        // 计算提醒时间点
        LocalDateTime remindTime = taskTime.minusMinutes(5);
        Medicine medicine = medicineMapper.findById(task.getMedicineId());

        // 没有超时，已经过了提醒时间，直接广播提醒
        if (remindTime.isBefore(now) || remindTime.isEqual(now)) {
            handleRemindBroadcast(task.getUserId(), medicine.getName());
        } else {
            executeTime = Date.from(remindTime.atZone(ZoneId.systemDefault()).toInstant());
            ScheduledFuture<?> remindFuture = taskScheduler.schedule(
                    () -> {
                        handleRemindBroadcast(task.getUserId(), medicine.getName());
                        remindTaskPool.remove(task.getId());
                    },  // 任务内容
                    executeTime  // 执行时间
            );
            remindTaskPool.put(task.getId(), future);
            logger.debug("【动态调度】任务ID={} 定时器已创建，将在 {} 进行提醒",
                    task.getId(), timeoutTime);
        }


        return true;
    }

    /**
     * 标记任务为漏服
     *
     * @param taskId 任务ID
     *
     * 执行时机：
     * 1. 定时器到期自动调用
     * 2. 应用启动时发现已超时任务
     */
    @Transactional(rollbackFor = Exception.class)
    protected void markTaskAsMissed(Long taskId) {
        try {
            logger.info("【动态调度】⏰ 超时！标记任务ID={} 为漏服", taskId);

            // 1. 更新数据库：status=2（漏服），operate_time=null
            taskMapper.updateStatus(taskId, 2, null);

            MedicationTask task = taskMapper.findById(taskId);
            if(task != null) {
                Long userId = task.getUserId();
                Long groupId = familyMemberMapper.getGroupIdByUserId(userId);
                Medicine medicine = medicineMapper.findById(task.getMedicineId());
                if(userId != null && groupId != null && medicine != null) {
                    familyEventLogMapper.insertLog(groupId, userId, EventLogEnum.ALARM_MISSED.name(), medicine.getName());
                    String alarmKey = RedisKeyBuilderUtil.getFamilyAlarmKey(groupId, LocalDate.now().toString());
                    redisService.delete(alarmKey);  // 异常数据变更，缓存数据失效

                    User user = userMapper.findByUserId(userId);
                    Assert.notNull(user, "任务所属用户Id为空");

                    // 2. 广播异常数据（alarm_missed）
                    Map<String, Object> pushData = new HashMap<>();
                    pushData.put("type", "medicine_alarm");
                    pushData.put("groupId", groupId);
                    pushData.put("memberName", user.getNickname());
                    pushData.put("medicineName", medicine.getName());
                    pushData.put("alarmTime", LocalDateTime.now().format(formatter));

                    applicationEventPublisher.publishEvent(new FamilyMedicineAlarmEvent(
                            this, groupId, pushData));
                }
            }

            // 3. 从Map中移除（任务已完成）
            medicationTaskPool.remove(taskId);

        } catch (Exception e) {
            logger.error("【动态调度】标记任务失败，ID=" + taskId, e);
        }
    }

    /**
     * 取消所有定时任务
     *
     * 使用场景：
     * 1. 每天凌晨刷新时，清理昨天的定时器
     * 2. 应用关闭时清理资源（Spring会自动调用）
     *
     */
    private void cancelAllTasks() {
        cancelAllAlarmTasks();
        cancelAllRemindTasks();
    }

    private void cancelAllAlarmTasks() {
        int count = medicationTaskPool.size();

        if (count == 0) {
            logger. info("【动态调度】没有需要取消的漏服标记定时器");
            return;
        }

        logger.info("【动态调度】开始取消 {} 个漏服标记定时器", count);

        // forEach参数是BiConsumer<K, V>
        // (taskId, future) -> {...} 相当于 new BiConsumer<Long, ScheduledFuture<? >>() {...}
        medicationTaskPool.forEach((taskId, future) -> {
            // cancel(false)：不中断正在执行的任务
            // cancel(true)：强制中断正在执行的任务
            future.cancel(false);
        });

        medicationTaskPool.clear();

        logger.info("【动态调度】已取消所有漏服标记定时器");
    }

    private void cancelAllRemindTasks() {
        int count = remindTaskPool.size();

        if (count == 0) {
            logger. info("【动态调度】没有需要取消的提醒定时器");
            return;
        }

        logger.info("【动态调度】开始取消 {} 个提醒定时器", count);

        // forEach参数是BiConsumer<K, V>
        // (taskId, future) -> {...} 相当于 new BiConsumer<Long, ScheduledFuture<? >>() {...}
        remindTaskPool.forEach((taskId, future) -> {
            // cancel(false)：不中断正在执行的任务
            // cancel(true)：强制中断正在执行的任务
            future.cancel(false);
        });

        remindTaskPool.clear();

        logger.info("【动态调度】已取消所有提醒定时器");
    }

    /**
     * 取消单个任务的定时器
     *
     * 使用场景：
     * 用户操作任务时（标记为已服用/漏服），取消定时器
     * 用户修改今天的计划时，取消定时器
     * 调用位置：
     * TaskServiceImpl.updateTaskStatus()方法中
     * PlanServiceImpl.updatePlan()方法中
     * @param taskId 任务ID
     */
    public void cancelTaskSchedule(Long taskId) {
        cancelAlarmTaskSchedule(taskId);
        cancelAllRemindTaskSchedule(taskId);
    }

    public void cancelAlarmTaskSchedule(Long taskId) {
        ScheduledFuture<?> future = medicationTaskPool.remove(taskId);

        if (future != null && !future.isDone()) {
            future.cancel(true);
            logger.debug("【动态调度】✓ 取消任务ID={} 的漏服定时器（用户已操作）", taskId);
        }
    }

    public void cancelAllRemindTaskSchedule(Long taskId) {
        ScheduledFuture<?> future = remindTaskPool.remove(taskId);

        if (future != null && !future.isDone()) {
            future.cancel(true);
            logger.debug("【动态调度】✓ 取消任务ID={} 的提醒定时器（用户已操作）", taskId);
        }
    }

    /**
     * 为新创建的任务添加定时器
     *
     * 使用场景：
     * 用户创建计划时，如果生成了今天的任务，需要动态添加定时器
     *
     * 调用位置：
     * PlanServiceImpl.createPlan()方法中
     *
     * @param task 新创建的任务
     */
    public void addTaskSchedule(MedicationTask task) {
        // 只为今天的任务创建定时器
        if (task.getTaskDate().equals(LocalDate.now())) {
            scheduleTaskTimeout(task);
        }
    }

    // 取消任务
    public void cancelInviteExpireTask(Long applyId) {
        ScheduledFuture<?> future = inviteTaskPool.get(applyId);
        if (future != null && !future.isDone()) {
            future.cancel(true);
            inviteTaskPool.remove(applyId);
        }
    }

    public void addInviteExpireTask(Long applyId, LocalDateTime expireTime) {
        // 如果已经存在先取消
        cancelInviteExpireTask(applyId);

        Date date = Date.from(expireTime.atZone(ZoneId.systemDefault()).toInstant());

        ScheduledFuture<?> future = taskScheduler.schedule(() -> handleInviteExpriration(applyId), date);
        inviteTaskPool.put(applyId, future);
    }

    // 定时处理申请 / 邀请 状态为过时的方法
    private void handleInviteExpriration(Long applyId) {
        // 这里锁的目的是防止定时任务和手动处理冲突
        String lockKey = RedisKeyBuilderUtil.getFamilyApproveLockKey(applyId);
        if(redisService.tryLock(lockKey, 2, 5)) {
            try {
                FamilyInviteApply apply = applyMapper.selectById(applyId);
                if (apply != null && InviteStatus.pending.equals(apply.getStatus())) {
                    apply.setStatus(InviteStatus.expired);
                    apply.setDealTime(LocalDateTime.now());
                    applyMapper.updateStatus(apply);

                    inviteTaskPool.remove(applyId);
                    logger.info("邀请/申请记录 {} 已自动过期", applyId);
                }
            } finally {
                redisService.unlock(lockKey);
            }
        }
    }

    private void handleRemindBroadcast(Long userId, String medicineName) {
        Map<String, Object> pushData = new HashMap<>();
        pushData.put("type", "medicine_remind");
        pushData.put("medicineName", medicineName);
        pushData.put("remindTime", LocalDateTime.now().format(formatter));

        applicationEventPublisher.publishEvent(new UserTaskMedicineRemindEvent(this, userId, pushData));
    }

    /**
     * 获取当前活跃的定时器数量（用于监控）
     */
    private int getActiveTaskCount() {
        return medicationTaskPool.size();
    }
}