# 第一阶段：打包
# 使用包含 Maven 和 JDK 的镜像来编译项目
FROM maven:3.9-eclipse-temurin-21 AS builder

# 设置工作目录
WORKDIR /build

# 先只复制 pom.xml，利用缓存机制提前下载依赖
# 这样只要 pom.xml 没变，依赖层就不会重新下载
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 再复制源代码，执行打包，跳过测试
COPY src ./src
RUN mvn clean package -DskipTests -B


# 第二阶段：运行
# 只使用轻量的 JRE 镜像，不需要 JDK 和 Maven
# 最终镜像体积会小很多
FROM eclipse-temurin:21-jre-alpine

# 设置工作目录
WORKDIR /app

# 从第一阶段的构建结果里，只取出 jar 文件
COPY --from=builder /build/target/*.jar app.jar

# 声明容器使用 8080 端口
EXPOSE 8080

# MAIN_CLASS 为空时按原单体启动；设置后可从同一个镜像启动指定微服务入口
ENTRYPOINT ["sh", "-c", "if [ -n \"$MAIN_CLASS\" ]; then exec java $JAVA_OPTS -Dloader.main=$MAIN_CLASS -cp app.jar org.springframework.boot.loader.launch.PropertiesLauncher \"$@\"; else exec java $JAVA_OPTS -jar app.jar \"$@\"; fi", "--"]
