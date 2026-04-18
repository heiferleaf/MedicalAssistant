# MedicalAssistant Backend

医疗助手后端服务（Spring Boot）。

## 技术栈

- Java 17+
- Spring Boot
- Maven
- MyBatis（XML Mapper）
- MySQL
- Docker / Docker Compose

## 项目结构

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/com/whu/medicalbackend/
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── db/
│   │       └── mapper/
│   └── test/
├── docker/
│   └── init.sql
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

## 环境要求

- JDK 17 或更高版本
- Maven 3.8+
- MySQL 8.x
- （可选）Docker Desktop

## 快速开始

### 1. 克隆并进入后端目录

```bash
cd /Users/motao/Desktop/MedicalAssistant/backend
```

### 2. 配置数据库

1. 创建数据库（例如 `medical_assistant`）。
2. 执行 `src/main/resources/db/` 下的 SQL 文件：
   - `database.sql`
   - `user.sql`
   - `family.sql`
   - `medicalPlan.sql`
   - `agentMemory.sql`

> 也可以参考 `docker/init.sql` 进行初始化。

### 3. 修改配置

编辑：

- `src/main/resources/application.yaml`

按本地环境修改数据库连接、端口、账号密码等配置。

### 4. 启动项目

```bash
./mvnw spring-boot:run
```

或：

```bash
mvn spring-boot:run
```

### 5. 运行测试

```bash
./mvnw test
```

## Docker 运行（可选）

### 使用 Docker Compose

```bash
docker compose up -d --build
```

查看日志：

```bash
docker compose logs -f
```

停止服务：

```bash
docker compose down
```

## 打包

```bash
./mvnw clean package -DskipTests
```

生成的 jar 位于 `target/` 目录。

## 常见问题

1. **数据库连接失败**
   - 检查 `application.yaml` 的 `url/username/password`。
   - 确认 MySQL 已启动并允许本机连接。

2. **端口被占用**
   - 修改 `application.yaml` 中 `server.port`，或释放占用端口。

3. **Mapper XML 未生效**
   - 检查 `resources/mapper/` 路径与 MyBatis 扫描配置是否一致。

## 开发说明

- 主要启动类：`MedicalBackendApplication.java`
- 核心包路径：`com.whu.medicalbackend`
- 推荐在 VS Code 或 IntelliJ IDEA 中开发。

## License

如无特殊说明，默认仅用于学习与项目内部使用。