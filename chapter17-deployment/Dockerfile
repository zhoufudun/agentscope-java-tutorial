# =============================================================================
# AgentScope Java - Chapter 17: Deployment
# 多阶段构建 Dockerfile
# =============================================================================

# 阶段 1: 构建阶段
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

# 复制 Maven 相关文件
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# 设置 Maven wrapper 执行权限
RUN chmod +x mvnw

# 使用 .mvn/jvm.config 配置 Maven（如果存在）
RUN if [ -f .mvn/jvm.config ]; then cat .mvn/jvm.config; fi || true

# 下载依赖（利用 Docker 缓存）
RUN ./mvnw dependency:go-offline -B || true

# 复制源代码
COPY src ./src

# 构建应用
RUN ./mvnw package -DskipTests -B

# =============================================================================
# 阶段 2: 运行阶段 - JRE 运行时镜像
# =============================================================================
FROM eclipse-temurin:21-jre AS runtime

# labels
LABEL maintainer="AgentScope Team"
LABEL description="AgentScope Java Tutorial - Chapter 17: Deployment"
LABEL version="1.0.0"

# 环境变量配置
ENV JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_OPTS=""
ENV TZ=Asia/Shanghai

# 创建非 root 用户
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# 创建应用目录
RUN mkdir -p /app && chown -R appuser:appgroup /app

WORKDIR /app

# 从构建阶段复制 jar 文件
COPY --from=builder /build/target/chapter17-deployment.jar app.jar

# 生产环境所需的目录
RUN mkdir -p /app/logs /app/config && chown -R appuser:appgroup /app

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health/readiness || exit 1

# 暴露端口
EXPOSE 8080

# 使用非 root 用户运行
USER appuser

# JVM 参数和启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar $SPRING_OPTS"]

# =============================================================================
# 可选: 调试阶段 (通过 docker build --target debug 构建)
# =============================================================================
FROM runtime AS debug

# 安装调试工具
USER root
RUN apt-get update && apt-get install -y curl netcat procps && rm -rf /var/lib/apt/lists/*
USER appuser

# 开启远程调试
ENV JAVA_DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# 覆盖入口点以启用调试
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS $JAVA_DEBUG_OPTS -jar app.jar"]

# =============================================================================
# 可选: Native Image 阶段 (需要 GraalVM - 实验性)
# =============================================================================
# FROM oracle/graalvm-ce:21 AS native-build
#
# WORKDIR /app
# COPY --from=builder /build/target/chapter17-deployment.jar app.jar
#
# # 构建原生镜像
# RUN native-image --enable-all-security-services \
#                  -jar app.jar \
#                  -H:Name=chapter17-deployment \
#                  -H:+ReportExceptionStackTraces \
#                  -R:MaxRuntimeCompileMethods=10000 \
#                  --initialize-at-run-time=org.apache.catalina \
#                  --initialize-at-run-time=org.apache.tomcat
#
# FROM ubuntu:22.04 AS native-runtime
# COPY --from=native-build /app/chapter17-deployment /usr/local/bin/
# EXPOSE 8080
# ENTRYPOINT ["/usr/local/bin/chapter17-deployment"]