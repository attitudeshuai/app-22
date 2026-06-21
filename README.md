# UmbrellaPoint 雨伞借还点管理系统

共享雨伞借还管理平台后端系统，支持扫码借伞、任意站点还伞、逾期提醒与信用管理。

## 功能亮点

- 极简借还流程：扫码借伞、任意站点还伞
- 多站点库存同步：实时追踪雨伞位置与状态
- 信用体系：逾期自动扣分，保障服务质量
- JWT安全认证：完善的权限控制机制
- 数据看板：借还趋势、库存统计一目了然

## 技术栈

| 类别 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2 |
| 数据库 | MySQL 8.0 |
| ORM | Spring Data JPA + Hibernate |
| 认证 | JWT (Spring Security) |
| API文档 | SpringDoc OpenAPI (Swagger) |
| 定时任务 | Spring Scheduler |
| 容器化 | Docker + Docker Compose |
| 测试 | JUnit 5 |

## 项目结构

```
.
├── src/main/java/com/umbrellapoint/
│   ├── controller/     # REST API 控制层
│   ├── service/        # 业务逻辑层
│   ├── repository/     # 数据访问层
│   ├── entity/         # 数据库实体
│   ├── dto/            # 数据传输对象
│   ├── config/         # 配置类
│   ├── exception/      # 全局异常处理
│   ├── scheduler/      # 定时任务
│   └── util/           # 工具类
├── src/main/resources/
│   └── application.properties
├── src/test/           # 测试代码
├── docs/               # 文档
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

## 快速启动

### 前置条件

- Docker 20.10+
- Docker Compose 2.0+

### 启动服务

```bash
# 克隆项目并进入目录
git clone <repo-url>
cd umbrella-point

# 一键启动（构建+启动）
docker-compose up --build -d

# 查看服务日志
docker-compose logs -f app
```

### 访问服务

| 服务 | 地址 |
|------|------|
| API 服务 | http://localhost:8092 |
| Swagger UI | http://localhost:8092/swagger-ui.html |
| API 文档 | http://localhost:8092/api-docs |
| 健康检查 | http://localhost:8092/actuator/health |
| MySQL | localhost:13317 |

### 默认测试账号

| 用户名 | 密码 | 说明 |
|--------|------|------|
| admin | 123456 | 管理员 |
| zhangsan | 123456 | 普通用户 |
| lisi | 123456 | 普通用户 |

## 测试方式

### 1. Postman 测试

导入项目根目录下的 `postman_collection.json` 到 Postman，配置环境变量 `baseUrl` 和 `token` 后即可运行所有测试用例。

### 2. curl 示例

```bash
# 用户登录
curl -X POST http://localhost:8092/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin","password":"123456"}'

# 获取借还点列表（需要携带Token）
curl -X GET http://localhost:8092/api/stations \
  -H "Authorization: Bearer <your-token>"
```

### 3. 单元测试

```bash
# Maven 运行测试
mvn test
```

## 停止服务

```bash
# 停止并删除容器（保留数据卷）
docker-compose down

# 停止并删除所有数据（含数据库）
docker-compose down -v
```

## 许可证

MIT License
