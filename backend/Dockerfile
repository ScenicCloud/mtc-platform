# 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# 运行阶段：基于 Ubuntu + JRE + Node.js + Playwright 依赖
FROM eclipse-temurin:21-jre

# 安装 Node.js 20.x 和 Playwright 系统依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    ca-certificates \
    gnupg \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

# 预安装 Playwright 浏览器（Chromium）及其系统依赖
RUN npx -y playwright@1.63.0 install --with-deps chromium

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
