# 物资库存管理系统 API 接口文档

## 1. 接口概况

- 后端地址：`http://localhost:3000`
- Swagger UI：`http://localhost:3000/api/docs`
- OpenAPI JSON：`http://localhost:3000/v3/api-docs`
- 接口风格：RESTful
- 认证方式：Spring Security + JWT

除登录、注册、健康检查、Swagger 文档、上传图片访问外，大部分业务接口都需要登录后携带 JWT。

请求头格式：

```http
Authorization: Bearer <token>
```

## 2. 认证接口

### 2.1 用户登录

```http
POST /api/auth/login
```

请求体：

```json
{
  "username": "admin",
  "password": "admin123"
}
```

返回：

```json
{
  "token": "JWT_TOKEN",
  "user": {
    "id": 1,
    "username": "admin",
    "name": "系统管理员",
    "role": "admin",
    "status": 1
  }
}
```

### 2.2 用户自助注册

```http
POST /api/auth/register
```

请求体：

```json
{
  "name": "张三",
  "email": "zhangsan@example.com",
  "password": "123456"
}
```

说明：

- 注册账号默认角色为 `operator`。
- 注册成功后直接返回 JWT，可自动登录。

### 2.3 当前登录用户

```http
GET /api/auth/me
```

说明：需要携带 JWT，用于查询当前登录用户信息。

## 3. 用户管理接口

用户管理接口仅管理员可访问。

### 3.1 分页查询用户

```http
GET /api/users?keyword=&page=1&pageSize=10
```

### 3.2 新增用户

```http
POST /api/users
```

请求体：

```json
{
  "username": "operator2",
  "name": "仓库操作员",
  "password": "123456",
  "role": "operator",
  "status": 1
}
```

### 3.3 编辑用户

```http
PUT /api/users/{id}
```

### 3.4 删除用户

```http
DELETE /api/users/{id}
```

### 3.5 当前用户信息

```http
GET /api/users/profile/current
```

## 4. 物资管理接口

### 4.1 分页查询物资

```http
GET /api/materials?keyword=&category=&lowStock=false&page=1&pageSize=10
```

查询参数：

| 参数 | 说明 |
| --- | --- |
| keyword | 物资编号、名称、规格、供应商、库位关键字 |
| category | 物资分类 |
| lowStock | 是否只查低库存 |
| page | 页码 |
| pageSize | 每页条数 |

### 4.2 查询物资详情

```http
GET /api/materials/{id}
```

### 4.3 新增物资

```http
POST /api/materials
```

请求体：

```json
{
  "code": "M-5001",
  "name": "办公椅",
  "category": "办公设备",
  "spec": "标准款",
  "unit": "把",
  "supplier": "办公用品供应商",
  "location": "A-01",
  "safetyStock": 10,
  "remark": "常用办公物资"
}
```

说明：

- `code` 为物资唯一编号。
- 新增物资时库存默认为 0。
- 库存由入库和出库自动计算。

### 4.4 编辑物资

```http
PUT /api/materials/{id}
```

### 4.5 上传物资照片

```http
POST /api/materials/{id}/photo
Content-Type: multipart/form-data
```

表单字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| photo | file | 物资照片 |

限制：

- 支持 JPG、PNG、WEBP、GIF。
- 文件大小不能超过 3MB。

### 4.6 删除物资

```http
DELETE /api/materials/{id}
```

说明：

- 如果该物资已有入库或出库记录，后端会拒绝删除。

### 4.7 查询物资分类选项

```http
GET /api/materials/categories/options
```

## 5. 入库管理接口

### 5.1 分页查询入库记录

```http
GET /api/inbound?keyword=&materialId=&from=&to=&page=1&pageSize=10
```

### 5.2 新增入库记录

```http
POST /api/inbound
```

请求体：

```json
{
  "materialId": 1,
  "quantity": 20,
  "unitPrice": 0,
  "source": "主仓库 / 供应商A",
  "remark": "采购入库"
}
```

说明：

- 入库数量必须大于 0。
- 提交成功后自动增加物资库存。

### 5.3 删除入库记录

```http
DELETE /api/inbound/{id}
```

说明：

- 仅管理员可操作。
- 删除后会同步回滚库存。

## 6. 出库管理接口

### 6.1 分页查询出库记录

```http
GET /api/outbound?keyword=&materialId=&from=&to=&page=1&pageSize=10
```

### 6.2 提交出库 / 出库申请

```http
POST /api/outbound
```

请求体：

```json
{
  "materialId": 1,
  "quantity": 5,
  "recipient": "行政部",
  "purpose": "办公领用",
  "remark": "日常使用"
}
```

说明：

- 出库数量必须大于 0。
- 出库数量不能大于提交时的当前库存。
- 管理员提交成功后记录状态为 `approved`（已通过），系统立即扣减库存。
- 操作员提交成功后记录状态为 `pending`（待审批），库存暂不变化。
- 库存不足时后端返回冲突错误，阻止负库存或无效申请产生。

返回字段中包含：

| 字段 | 说明 |
| --- | --- |
| status | 审批状态：`pending` 待审批、`approved` 已通过、`rejected` 已驳回 |
| approved_by | 审批人 |
| approved_at | 审批时间 |
| approval_remark | 审批备注或驳回原因 |

### 6.3 管理员审批通过出库申请

```http
POST /api/outbound/{id}/approve
```

说明：

- 仅管理员可操作。
- 只有 `pending` 状态的申请可以审批通过。
- 管理员自己提交的出库记录已经是 `approved`，不需要再审批。
- 审批时后端会再次校验库存；库存足够才扣减 `materials.stock`。
- 审批成功后状态变为 `approved`，并记录审批人和审批时间。

### 6.4 管理员驳回出库申请

```http
POST /api/outbound/{id}/reject
```

请求体：

```json
{
  "remark": "领用信息不完整"
}
```

说明：

- 仅管理员可操作。
- 只有 `pending` 状态的申请可以驳回。
- 驳回后状态变为 `rejected`，库存保持不变。

### 6.5 删除出库记录

```http
DELETE /api/outbound/{id}
```

说明：

- 仅管理员可操作。
- 删除已通过记录时会把该出库数量加回库存。
- 删除待审批或已驳回记录时库存不变。

## 7. 统计报表接口

### 7.1 工作台汇总数据

```http
GET /api/reports/dashboard
```

返回内容包括：

- 物资总数
- 库存总量
- 低库存数量
- 今日入库
- 今日出库
- 分类库存
- 出入库趋势
- 库存 Top
- 低库存预警

### 7.2 出入库趋势

```http
GET /api/reports/movement?days=14
```

说明：

- `days` 范围会被后端限制在 7 到 60 天。

### 7.3 分类库存统计

```http
GET /api/reports/stock-by-category
```

### 7.4 低库存列表

```http
GET /api/reports/low-stock
```

## 8. AI 助手接口

AI 助手接口需要登录后携带 JWT。前端只调用本系统后端，智谱 API Key 由后端通过环境变量 `ZHIPU_API_KEY` 或本地文件 `backend/.env.local` 读取，避免暴露在浏览器中。

当用户提问涉及库存、低库存、最近入库、最近出库、待审批申请等业务数据时，后端会先从 MySQL 实时查询当前数据，再把精简后的数据上下文发送给智谱模型，因此 AI 回答会以当前数据库数据为准。

### 8.1 与 AI 助手对话

```http
POST /api/ai/chat
```

请求体：

```json
{
  "message": "当前低库存物资有哪些？",
  "history": [
    {
      "role": "user",
      "content": "如何新增物资？"
    },
    {
      "role": "assistant",
      "content": "进入物资管理页面，点击新增物资..."
    }
  ]
}
```

返回：

```json
{
  "reply": "当前低库存物资包括：MC-1001 螺栓，库存 80 件，安全库存 100 件..."
}
```

说明：

- `message` 为本次问题，不能为空。
- `history` 为可选上下文，只保留最近几轮对话。
- 支持实时查询库存总览、指定物资库存、低库存预警、最近入库记录、最近出库记录、待审批出库申请和分类库存。
- 后端默认调用智谱 `glm-4-flash`，可通过环境变量 `ZHIPU_MODEL` 修改。
- 如果未设置环境变量，可复制 `backend/.env.local.example` 为 `backend/.env.local`，并填写 `ZHIPU_API_KEY`。

## 9. 系统接口

### 9.1 健康检查

```http
GET /api/health
```

说明：

- 不需要登录。
- 用于确认后端服务是否启动成功。

### 9.2 Swagger 页面

```http
GET /api/docs
```

说明：

- 不需要登录。
- 用于查看和测试后端接口。

## 10. 常见状态码

| 状态码 | 说明 |
| --- | --- |
| 200 | 请求成功 |
| 400 | 参数错误 |
| 401 | 未登录或 token 无效 |
| 403 | 权限不足 |
| 404 | 数据或接口不存在 |
| 409 | 数据冲突，例如编号重复、库存不足 |
| 500 | 服务器内部错误 |
