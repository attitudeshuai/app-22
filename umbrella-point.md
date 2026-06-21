# 项目22：雨伞借还点（UmbrellaPoint）

## 请帮我从 0 到 1 实现以下小众项目

### 项目概述
在公司、商场或小区门口放置共享雨伞，用户扫码借伞、到任一站点还伞。系统记录借还时间、逾期提醒与站点库存。

### 创新点 / 小众定位
极简借还场景，通过站点库存同步、信用借还与雨天预约，解决"下雨没带伞"的瞬间痛点。

### 目标用户
写字楼上班族、商场顾客、小区居民

## 项目范围说明
- 本项目为纯后端系统开发，不涉及任何前端页面、UI、CSS/JS 改动。
- 所有功能均通过 RESTful API 对外提供服务，可使用 Postman、curl 或任意 HTTP 客户端进行测试与验收。

## 技术栈（必须严格使用）
- **后端框架**: Java Spring Boot 3.2 (Spring Web)
- **数据库**: MySQL 8.0
- **ORM**: Spring Data JPA + Hibernate
- **认证**: JWT (Spring Security)
- **API文档**: SpringDoc OpenAPI (Swagger)
- **定时任务**: Spring Scheduler（如需要）
- **容器化**: Docker + Docker Compose
- **测试**: JUnit 5 + Testcontainers（可选）+ Postman 测试集合

## 项目必须包含的交付物
- **Dockerfile**：多阶段构建，基于上述技术栈。
- **docker-compose.yml**：一键启动应用服务 + MySQL 8.0 + 可选管理工具（如 Adminer）。
- **.gitignore**：针对 Java Spring Boot 的标准忽略配置。
- **README.md**：项目简介、目录说明、快速启动、API 文档入口、测试方式。
- **docs/functional_intro.md**：功能说明、ER 图文字描述、核心用例、业务规则。
- **src/**：完整后端源码（Controller / Service / Repository / Entity / DTO / Mapper / Config 等）。
- **tests/**：单元测试 + 集成测试。
- **postman_collection.json**（或同等测试脚本）：覆盖所有接口的功能测试集合。
- **初始化 SQL / Seed Data**：Docker 启动后自动建表并插入示例数据。

## 数据库设计

### 主要数据表
1. **Users** - 用户表
   - Id（主键）
   - Username（用户名，唯一）
   - Email（邮箱，唯一）
   - PasswordHash（密码哈希）
   - Avatar（头像 URL，可选）
   - CreatedAt / UpdatedAt

2. **Stations** - 借还点
   - Name
   - Address
   - ManagerId
   - Latitude
   - Longitude
   - Capacity
   - IsActive
   - CreatedAt

3. **Umbrellas** - 雨伞
   - Code（伞编号）
   - StationId
   - Color
   - Status（Available / Borrowed / Lost / Damaged）
   - CreatedAt

4. **BorrowRecords** - 借还记录
   - UmbrellaId
   - UserId
   - BorrowStationId
   - ReturnStationId
   - BorrowTime
   - ReturnTime
   - Status（Ongoing / Returned / Overdue）
   - Deposit

5. **UserCredits** - 用户信用
   - UserId
   - Score
   - OverdueCount
   - UpdatedAt

## 核心功能模块
### 1. 用户认证模块
- 用户注册 / 登录 / JWT 鉴权
- 获取当前登录用户信息

### 2. 借还点管理模块
- 借还点的增删改查（支持分页、搜索、排序）
- 借还点状态/详情/关联操作
- 借还点权限控制（仅所有者或管理员可操作）

### 3. 雨伞管理模块
- 雨伞的增删改查（支持分页、搜索、排序）
- 雨伞状态/详情/关联操作
- 雨伞权限控制（仅所有者或管理员可操作）

### 4. 借还管理模块
- 借还的增删改查（支持分页、搜索、排序）
- 借还状态/详情/关联操作
- 借还权限控制（仅所有者或管理员可操作）

### 5. 信用管理模块
- 信用的增删改查（支持分页、搜索、排序）
- 信用状态/详情/关联操作
- 信用权限控制（仅所有者或管理员可操作）

### 6. 统计与搜索模块
- 全局搜索与筛选
- 基础数据看板（数量、趋势、排行榜等）
- 导出关键数据（可选）

## API 接口清单
### Auth
- POST /api/auth/register - 用户注册
- POST /api/auth/login - 用户登录
- GET /api/auth/me - 获取当前用户信息
- PUT /api/auth/me - 更新个人信息

### Stations（借还点）
- GET /api/stations - 获取借还点列表（支持分页、搜索、筛选）
- POST /api/stations - 创建借还点
- GET /api/stations/{id} - 获取借还点详情
- PUT /api/stations/{id} - 更新借还点
- DELETE /api/stations/{id} - 删除借还点

### Umbrellas（雨伞）
- GET /api/umbrellas - 获取雨伞列表（支持分页、搜索、筛选）
- POST /api/umbrellas - 创建雨伞
- GET /api/umbrellas/{id} - 获取雨伞详情
- PUT /api/umbrellas/{id} - 更新雨伞
- DELETE /api/umbrellas/{id} - 删除雨伞
- PATCH /api/umbrellas/{id}/status - 修改雨伞状态

### BorrowRecords（借还）
- GET /api/borrowrecords - 获取借还列表（支持分页、搜索、筛选）
- POST /api/borrowrecords - 创建借还
- GET /api/borrowrecords/{id} - 获取借还详情
- PUT /api/borrowrecords/{id} - 更新借还
- DELETE /api/borrowrecords/{id} - 删除借还
- PATCH /api/borrowrecords/{id}/status - 修改借还状态
- GET /api/borrowrecords/mine - 获取我发布的/关联的借还

### UserCredits（信用）
- GET /api/usercredits - 获取信用列表（支持分页、搜索、筛选）
- POST /api/usercredits - 创建信用
- GET /api/usercredits/{id} - 获取信用详情
- PUT /api/usercredits/{id} - 更新信用
- DELETE /api/usercredits/{id} - 删除信用
- GET /api/usercredits/mine - 获取我发布的/关联的信用

### Statistics
- GET /api/stats/overview - 总览统计
- GET /api/stats/trend - 趋势统计（按时间范围）

## Docker 配置要求

### Dockerfile（Java Spring Boot）
```dockerfile
# 阶段1：构建
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 阶段2：运行
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8092
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3   CMD wget --no-verbose --tries=1 --spider http://localhost:8092/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

要求：
1. 使用 Maven 多阶段构建，最终镜像只包含 JRE 与 jar 包。
2. 暴露 8092 端口。
3. 启用 Spring Boot Actuator 健康检查 `/actuator/health`。
4. 通过环境变量读取数据库连接配置。

### docker-compose.yml 要求
```yaml
version: '3.8'
services:
  app:
    build: .
    container_name: umbrellapoint_app
    ports:
      - "8092:8092"
    environment:
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_NAME=umbrellapoint
      - DB_USER=app_user
      - DB_PASSWORD=app_pass
    depends_on:
      mysql:
        condition: service_healthy
  mysql:
    image: mysql:8.0
    container_name: umbrellapoint_mysql
    environment:
      - MYSQL_ROOT_PASSWORD=root_pass
      - MYSQL_DATABASE=umbrellapoint
      - MYSQL_USER=app_user
      - MYSQL_PASSWORD=app_pass
    ports:
      - "13317:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
  volumes:
    mysql_data:
```

要求：
1. MySQL 使用 8.0 镜像。
2. 应用服务必须等 MySQL healthy 后再启动。
3. 使用 named volume 持久化数据库数据。
4. 环境变量集中管理，禁止在源码中硬编码密码。

## .gitignore 参考
```text
# Java / Maven / Gradle
target/
build/
*.class
*.jar
*.war
*.ear
*.iml

# IDE
.idea/
*.ipr
*.iws
.vscode/

# Secrets & local config
.env
.env.local
application-local.properties
application-dev.properties

# Logs
*.log
logs/

# Test results
surefire-reports/

# OS
Thumbs.db
.DS_Store
```

## 文档要求

### README.md
至少包含：
1. 项目名称与一句话介绍。
2. 功能亮点（3-5 条）。
3. 技术栈说明。
4. 目录结构说明。
5. 快速启动步骤（克隆 → Docker 启动 → 访问接口）。
6. 测试命令与 Postman 集合导入说明。
7. 贡献与许可（可选）。

### docs/functional_intro.md
至少包含：
1. 业务背景与解决的问题。
2. 用户角色与核心用例。
3. 功能模块详细说明。
4. 数据库 ER 图文字描述（表关系）。
5. 关键业务规则（如状态流转、权限规则、时间计算逻辑）。
6. 接口调用示例（至少 3 个）。

## 运行与测试步骤

1. **克隆并进入项目目录**：
   ```bash
   git clone <repo-url>
   cd UmbrellaPoint
   ```

2. **Docker 启动**：
   ```bash
   docker-compose up --build -d
   ```

3. **查看日志**：
   ```bash
   docker-compose logs -f app
   ```

4. **验证服务健康**：
   - .NET：`curl http://localhost:8092/health`
   - Java：`curl http://localhost:8092/actuator/health`

5. **导入并执行 Postman 测试集合**，验证所有接口：
   - 注册 / 登录
   - 各实体的 CRUD
   - 搜索 / 筛选 / 分页
   - 统计接口
   - 权限控制（未登录访问受限资源应返回 401）

6. **执行自动化测试**：
   - .NET：`dotnet test`
   - Java：`./mvnw test` 或 `mvn test`

7. **停止服务**：
   ```bash
   docker-compose down -v
   ```

## 其他质量要求
- 使用 Spring Data JPA 操作 MySQL，禁止手写 SQL 进行日常 CRUD（复杂统计可手写）。
- 代码分层清晰，遵循 RESTful API 设计规范。
- 关键代码必须有中文注释，说明业务意图。
- 统一的异常处理与参数校验（.NET FluentValidation / Spring Validation）。
- 使用 JWT 保护敏感接口，未携带 Token 返回 401。
- 数据库连接字符串通过环境变量注入，支持 Docker 内外运行。
- 提供 Seed Data，容器启动后至少有 5-10 条示例数据可用于测试。
- 接口返回统一包装格式（code / message / data）。
- 日志使用框架原生日志（.NET ILogger / SLF4J），记录关键操作与异常。
- 项目必须是小众生活/工作场景，禁止做成通用商城、OA、CMS、ERP。
