# 雨伞借还点系统 - 功能说明文档

## 1. 业务背景与解决的问题

### 1.1 业务背景
在写字楼、商场、地铁站、小区门口等公共场所，突发降雨时人们常常面临"下雨没带伞"的困境。传统伞具购买方式既不经济也不环保，共享雨伞模式应运而生。

### 1.2 解决的问题
- **即时借还**：用户扫码即可借伞，到任意站点还伞，无需固定归还地点
- **信用保障**：通过信用积分机制，降低雨伞丢失/损坏风险
- **库存管理**：实时监控各站点雨伞数量，智能调度
- **逾期提醒**：24小时未还自动标记逾期，扣减信用分

## 2. 用户角色与核心用例

### 2.1 用户角色
| 角色 | 说明 |
|------|------|
| 普通用户 | 注册/登录、借伞、还伞、查看个人信用 |
| 站点管理员 | 管理借还点、雨伞、查看借还记录 |

### 2.2 核心用例
1. **用户注册/登录**：用户通过用户名+邮箱注册，JWT Token鉴权
2. **扫码借伞**：用户选择站点雨伞，系统创建借还记录，雨伞状态变为"借出"
3. **任意点还伞**：用户到任意站点还伞，雨伞状态变为"可用"，记录归还站点
4. **逾期检测**：每日凌晨2点自动检查超过24小时未归还的雨伞
5. **信用管理**：逾期自动扣减信用分，影响后续借伞权限
6. **数据看板**：统计总用户数、借还点、雨伞、借还记录趋势等

## 3. 功能模块详细说明

### 3.1 用户认证模块
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录（用户名/邮箱+密码）
- `GET /api/auth/me` - 获取当前登录用户信息
- `PUT /api/auth/me` - 更新个人信息

### 3.2 借还点管理模块
- `GET /api/stations` - 获取借还点列表（分页、搜索、排序）
- `POST /api/stations` - 创建借还点
- `GET /api/stations/{id}` - 获取借还点详情
- `PUT /api/stations/{id}` - 更新借还点
- `DELETE /api/stations/{id}` - 删除借还点

### 3.3 雨伞管理模块
- `GET /api/umbrellas` - 获取雨伞列表（按站点/状态筛选）
- `POST /api/umbrellas` - 创建雨伞
- `GET /api/umbrellas/{id}` - 获取雨伞详情
- `PUT /api/umbrellas/{id}` - 更新雨伞
- `PATCH /api/umbrellas/{id}/status` - 修改雨伞状态
- `DELETE /api/umbrellas/{id}` - 删除雨伞

### 3.4 借还管理模块
- `GET /api/borrowrecords` - 获取借还记录列表
- `POST /api/borrowrecords` - 创建借伞（自动更新雨伞状态）
- `GET /api/borrowrecords/{id}` - 获取借还详情
- `PUT /api/borrowrecords/{id}` - 更新借还记录
- `PATCH /api/borrowrecords/{id}/status` - 修改状态（还伞/逾期）
- `GET /api/borrowrecords/mine` - 获取我的借还记录
- `DELETE /api/borrowrecords/{id}` - 删除借还记录

### 3.5 信用管理模块
- `GET /api/usercredits` - 获取用户信用列表
- `POST /api/usercredits` - 创建信用记录
- `GET /api/usercredits/{id}` - 获取信用详情
- `PUT /api/usercredits/{id}` - 更新信用
- `DELETE /api/usercredits/{id}` - 删除信用
- `GET /api/usercredits/mine` - 获取我的信用

### 3.6 统计分析模块
- `GET /api/stats/overview` - 总览统计（用户数、站点数、雨伞分布等）
- `GET /api/stats/trend` - 借还趋势（按日期范围）

## 4. 数据库 ER 图文字描述

### 4.1 表关系说明

```
Users (1) ------- (1) UserCredits
  |                     |
  | userId              | userId
  |                     |
  v                     v
BorrowRecords (N) -- (1) Umbrellas -- (1) Stations
      |                    |                  |
      | umbrellaId         | stationId        | managerId
      | borrowStationId    |                  |
      | returnStationId    |                  v
      +----------------------------------> Stations
```

### 4.2 表关联详细说明

| 关联 | 类型 | 说明 |
|------|------|------|
| Users → UserCredits | 一对一 | 每个用户对应一条信用记录 |
| Users → BorrowRecords | 一对多 | 一个用户可有多条借还记录 |
| Umbrellas → BorrowRecords | 一对多 | 一把伞可被多次借还 |
| Stations → Umbrellas | 一对多 | 一个站点可有多把雨伞 |
| Stations → BorrowRecords | 一对多 | 借出站点/归还站点 |

### 4.3 状态枚举

**雨伞状态 UmbrellaStatus**：
- `Available` - 可借
- `Borrowed` - 已借出
- `Lost` - 丢失
- `Damaged` - 损坏

**借还状态 BorrowStatus**：
- `Ongoing` - 借出中
- `Returned` - 已归还
- `Overdue` - 已逾期

## 5. 关键业务规则

### 5.1 状态流转规则

**雨伞状态流转**：
```
Available --(借伞)--> Borrowed --(还伞)--> Available
Available --(标记损坏)--> Damaged
Available --(标记丢失)--> Lost
Borrowed --(逾期)--> 仍为 Borrowed（借还记录变 Overdue）
```

**借还状态流转**：
```
Ongoing --(还伞)--> Returned
Ongoing --(逾期检查/手动标记)--> Overdue
```

### 5.2 权限规则
- 注册/登录接口匿名可访问
- 其他所有接口必须携带有效 JWT Token
- 未登录或 Token 无效返回 `401 Unauthorized`
- 个人数据（我的借还、我的信用）仅本人可访问

### 5.3 时间计算逻辑
- **借伞时间**：创建借还记录时自动写入当前时间
- **还伞时间**：状态变更为 Returned 时自动写入当前时间
- **逾期判定**：借出超过 24 小时未还即为逾期
- **定时任务**：每日凌晨 2 点自动执行逾期检查

### 5.4 信用规则
- 新用户初始信用分：100 分
- 单次逾期扣减：5 分
- 最低信用分：0 分
- 逾期次数：每次逾期 +1

### 5.5 借伞规则
- 只有 `Available` 状态的雨伞可以被借出
- 借伞成功后雨伞状态自动变为 `Borrowed`
- 还伞成功后雨伞状态自动变为 `Available`
- 还伞时可指定归还站点，自动更新雨伞所属站点

## 6. 接口调用示例

### 示例 1：用户注册

**请求**：
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "newuser",
  "email": "newuser@example.com",
  "password": "mypassword123"
}
```

**响应**：
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "user": {
      "id": 7,
      "username": "newuser",
      "email": "newuser@example.com",
      "avatar": null,
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    }
  }
}
```

### 示例 2：创建借伞

**请求**：
```http
POST /api/borrowrecords
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

{
  "umbrellaId": 1,
  "borrowStationId": 1,
  "deposit": 29.90
}
```

**响应**：
```json
{
  "code": 200,
  "message": "借伞成功",
  "data": {
    "id": 3,
    "umbrellaId": 1,
    "userId": 2,
    "borrowStationId": 1,
    "returnStationId": null,
    "borrowTime": "2024-01-15T10:35:00",
    "returnTime": null,
    "status": "Ongoing",
    "deposit": 29.90,
    "createdAt": "2024-01-15T10:35:00"
  }
}
```

### 示例 3：获取总览统计

**请求**：
```http
GET /api/stats/overview
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalUsers": 6,
    "totalStations": 6,
    "activeStations": 6,
    "totalUmbrellas": 30,
    "availableUmbrellas": 28,
    "borrowedUmbrellas": 2,
    "ongoingBorrows": 1,
    "returnedBorrows": 1,
    "overdueBorrows": 0,
    "umbrellaStatusDistribution": {
      "Available": 28,
      "Borrowed": 2,
      "Lost": 0,
      "Damaged": 0
    },
    "borrowStatusDistribution": {
      "Ongoing": 1,
      "Returned": 1,
      "Overdue": 0
    }
  }
}
```
