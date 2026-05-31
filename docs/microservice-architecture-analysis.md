# 微服务架构问题分析与修复方案

## 问题概述

当前 `docker-compose.microservices.yml` 启动了 7 个业务容器，但**并非真正的微服务架构**，而是"伪微服务"：

- ✅ 优点：按入口路径拆分，通过 Nginx 路由
- ❌ 问题：各服务仍然直接扫描和使用其他领域的 Mapper，跨服务数据访问发生在同一个 JVM 内

这导致：
1. **违反微服务原则**：服务间没有通过 HTTP/RPC 通信，而是直接共享数据库访问
2. **资源浪费严重**：7 个容器重复加载大量 Bean、数据库连接池、Redis 连接
3. **无法独立部署**：服务间强耦合，无法真正独立扩展
4. **内存和连接开销**：每个容器都维护完整的数据库连接池

## 详细分析

### 1. 各服务 MapperScan 配置分析

| 服务 | 应该扫描 | 实际扫描 | 问题 |
|------|---------|---------|------|
| UserService | user.mapper | user.mapper | ✅ 正确 |
| MedicationService | medical.mapper | medical.mapper, **family.mapper, user.mapper** | ❌ 跨域 |
| FamilyService | family.mapper | family.mapper, **user.mapper, medical.mapper, health.mapper** | ❌ 跨域 |
| HealthService | health.mapper | health.mapper, **family.mapper** | ❌ 跨域 |
| AgentService | agent.mapper | agent.mapper, **medical.mapper, family.mapper, health.mapper, user.mapper** | ❌ 跨域 |
| NotificationService | (无) | **family.mapper, user.mapper** | ❌ 跨域 |
| SchedulerService | (无) | **medical.mapper, family.mapper, user.mapper** | ❌ 跨域 |

### 2. 跨领域 Mapper 使用场景

#### Family 领域 → 其他领域
- `FamilyGroupServiceImpl` 使用 `UserMapper`、`MedicationTaskMapper`、`HealthDataMapper`
- `FamilyCacheService` 使用 `UserMapper`

#### Health 领域 → 其他领域
- `HealthDataServiceImpl` 使用 `FamilyMemberMapper`

#### Medical 领域 → 其他领域
- `PlanServiceImpl` 使用 `FamilyMemberMapper`
- `TaskServiceImpl` 使用 `FamilyMemberMapper`、`UserMapper`

#### Common 领域（定时任务）→ 业务领域
- `DynamicTaskScheduler` 使用 `MedicationTaskMapper`、`FamilyMemberMapper`、`UserMapper`

#### Notification 领域 → 其他领域
- 需要查询家庭成员和用户信息用于 WebSocket 推送

### 3. 资源开销分析

每个业务容器都会创建：
- **数据库连接池**：默认 HikariCP 最小 10 个连接 × 7 容器 = 70 个连接
- **Redis 连接**：每个容器独立的 Lettuce 连接池
- **RabbitMQ 连接**：每个容器独立的 AMQP 连接
- **Spring Bean**：大量重复加载的 Service、Mapper、Config

对于当前代码规模（约 10 个 Mapper，17 个 Service），这种开销是不必要的。

## 修复方案

### 方案一：回退到单体架构（推荐）

**适用场景**：
- 当前代码规模不大（< 10 万行）
- 团队规模较小（< 10 人）
- 不需要独立扩展各个服务
- 优先考虑资源效率和开发效率

**实施步骤**：
1. 使用现有的 `docker-compose.yml`（单体模式）
2. 保持代码的模块化结构（按领域划分包）
3. 只部署一个后端容器 + 基础设施栈（MySQL、Redis、RabbitMQ）

**优点**：
- 资源占用少（1 个后端容器 vs 7 个）
- 部署简单，启动快
- 调试方便，日志集中
- 事务管理简单（同一个数据库连接）
- 代码仍然保持模块化，未来可以拆分

**缺点**：
- 无法独立扩展各个服务
- 无法展示微服务架构能力

### 方案二：真正的微服务改造（长期）

**适用场景**：
- 需要独立扩展各个服务（如 Agent 服务需要更多资源）
- 团队规模较大，需要独立开发和部署
- 需要展示微服务架构能力

**实施步骤**：

#### 第一阶段：设计服务间通信接口

为每个领域设计 REST API：

```java
// UserService 对外提供的 API
@RestController
@RequestMapping("/internal/user")
public class UserInternalController {
    @GetMapping("/{userId}")
    public UserDTO getUserById(@PathVariable Long userId) { ... }
    
    @PostMapping("/batch")
    public List<UserDTO> getUsersByIds(@RequestBody List<Long> userIds) { ... }
}

// FamilyService 对外提供的 API
@RestController
@RequestMapping("/internal/family")
public class FamilyInternalController {
    @GetMapping("/member/{userId}")
    public FamilyMemberDTO getMemberByUserId(@PathVariable Long userId) { ... }
    
    @GetMapping("/group/{groupId}/members")
    public List<FamilyMemberDTO> getGroupMembers(@PathVariable Long groupId) { ... }
}
```

#### 第二阶段：实现服务间调用客户端

使用 Spring Cloud OpenFeign 或 RestTemplate：

```java
// 在 MedicationService 中调用 FamilyService
@FeignClient(name = "family-service", url = "${service.family.url}")
public interface FamilyServiceClient {
    @GetMapping("/internal/family/member/{userId}")
    FamilyMemberDTO getMemberByUserId(@PathVariable Long userId);
}
```

#### 第三阶段：移除跨领域 MapperScan

修改各个 Bootstrap Application：

```java
// MedicationServiceApplication - 修复后
@MapperScan("com.whu.medicalbackend.medical.mapper")  // 只扫描自己的
public class MedicationServiceApplication { ... }

// FamilyServiceApplication - 修复后
@MapperScan("com.whu.medicalbackend.family.mapper")  // 只扫描自己的
public class FamilyServiceApplication { ... }
```

#### 第四阶段：重构业务代码

将直接的 Mapper 调用替换为服务间调用：

```java
// 修复前
@Autowired
private UserMapper userMapper;

User user = userMapper.selectById(userId);

// 修复后
@Autowired
private UserServiceClient userServiceClient;

UserDTO user = userServiceClient.getUserById(userId);
```

#### 第五阶段：处理特殊场景

**定时任务服务（SchedulerService）**：
- 定时任务需要访问多个领域的数据
- 方案 1：通过 HTTP 调用其他服务的 API
- 方案 2：将定时任务分散到各个领域服务内部

**通知服务（NotificationService）**：
- WebSocket 推送需要查询用户和家庭信息
- 方案 1：通过 HTTP 调用 UserService 和 FamilyService
- 方案 2：在 MQ 消息中携带必要的用户信息，避免二次查询

**优点**：
- 真正的微服务架构
- 可以独立扩展各个服务
- 服务间解耦，可以独立部署
- 可以使用不同的技术栈（如 Agent 服务可以用 Python）

**缺点**：
- 开发复杂度高
- 需要处理分布式事务
- 需要服务注册与发现（Nacos、Eureka）
- 需要 API 网关（Spring Cloud Gateway）
- 网络调用开销
- 调试困难

### 方案三：混合模式（折中）

保留微服务部署形态，但允许共享数据库访问：

- 各服务仍然可以直接访问数据库
- 通过 Nginx 路由实现入口隔离
- 通过 RabbitMQ 实现异步通信（如 WebSocket 推送）
- 不强制要求服务间 HTTP 调用

**优点**：
- 可以展示微服务部署形态
- 开发复杂度较低
- 保留了异步通信的优势

**缺点**：
- 仍然是"伪微服务"
- 资源开销仍然较大
- 无法真正独立部署

## 推荐方案

根据当前项目情况，**推荐方案一：回退到单体架构**。

理由：
1. 代码规模不大，不需要微服务的复杂度
2. 资源效率更高（1 个容器 vs 7 个容器）
3. 开发和调试更简单
4. 代码已经按领域模块化，未来可以平滑迁移到微服务

如果必须保留微服务形态（如用于学习或展示），建议：
1. 短期：使用方案三（混合模式），接受"伪微服务"的现状
2. 长期：按方案二逐步改造为真正的微服务

## 实施建议

### 如果选择方案一（单体架构）

```bash
# 停止微服务集群
docker compose -f docker-compose.microservices.yml down

# 启动单体模式
docker compose up -d --build
```

### 如果选择方案二（真正的微服务）

需要投入 2-4 周时间进行改造：
- Week 1: 设计服务间 API 接口
- Week 2: 实现 Feign Client 和内部 Controller
- Week 3: 重构业务代码，替换 Mapper 调用
- Week 4: 测试和调优

### 如果选择方案三（混合模式）

当前状态已经是混合模式，只需：
1. 在文档中明确说明这是"按入口拆分的多实例部署"
2. 接受各服务共享数据库访问的现状
3. 确保 RabbitMQ 异步通信正常工作（WebSocket 推送已修复）

## 结论

当前微服务架构的核心问题是：**各服务仍然直接共享数据库访问，没有通过服务间 API 通信**。

修复这个问题有三种方案：
1. **回退到单体**（推荐）：资源效率高，开发简单
2. **真正的微服务**（长期）：需要大量改造，适合大型项目
3. **混合模式**（折中）：接受现状，保留部署形态

建议根据项目目标（学习、展示、生产）选择合适的方案。
