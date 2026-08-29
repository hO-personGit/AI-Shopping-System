# ===== SpringBoot 后端多阶段构建 =====
# Stage 1: Maven 编译打包
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
# 先下载依赖缓存
RUN mvn -B dependency:go-offline || true
COPY src ./src
# 用示例配置生成可启动配置（真实参数通过容器环境变量覆盖）
RUN cp src/main/resources/application-example.properties src/main/resources/application.properties
# 跳过测试打包（测试在 CI 中独立执行）
RUN mvn -B package -DskipTests

# Stage 2: 精简运行镜像
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/springboot-*.jar app.jar

# 数据库 / Redis / AI 服务地址通过环境变量覆盖（docker-compose 中注入）
EXPOSE 1234
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
