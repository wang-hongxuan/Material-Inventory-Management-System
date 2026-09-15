# 物资库存管理系统

前后端分离课程项目，包含系统用户管理、物资基本信息管理、物资照片上传、入库管理、出库管理、库存报表、统计图表和 AI 助手。

## 技术栈

- 前端：Vue 3、Vite、Element Plus、ECharts、Axios、Vue Router
- 后端：SpringBoot 3.3.5、Spring Security、JWT、Spring Data JPA
- 数据库：MySQL 8.0，默认库名 `inventory_db`
- 接口文档：Springdoc OpenAPI / Swagger UI
- 接口风格：RESTful

## 默认账号

- 管理员：admin / admin123
- 操作员：operator / operator123

普通用户也可以在登录页点击“创建账号”自助注册。自助注册的账号默认角色为操作员，注册成功后会自动登录。
管理员仍然可以在系统的“用户管理”页面为用户创建账号、编辑角色、启用或停用账号。

## 项目功能

- 登录认证：JWT 登录、请求拦截、路由守卫、管理员权限控制。
- 用户管理：用户自助注册、管理员分页查询、新增、编辑、删除、启用和停用。
- 物资管理：唯一物资编号、增删改查、分页查询、分类筛选、低库存筛选、图片上传。
- 入库管理：新增入库记录后自动增加库存，管理员可删除入库记录并回滚库存。
- 出库管理：管理员登记出库后直接扣减库存；操作员提交出库申请后等待管理员审批，通过后再扣减库存，库存不足时前后端双重拦截。
- 统计报表：库存总览、分类库存图表、出入库趋势图、库存 Top、低库存预警、CSV 导出。
- AI 助手：登录后可在系统内咨询新增物资、入库出库、出库审批、库存不足、低库存预警、角色权限和库存报表等操作问题；也支持实时查询当前库存总览、指定物资库存、最近出入库记录和待审批出库申请，后端统一读取 MySQL 实时数据并调用智谱 API。
- 容错处理：参数校验、登录过期、权限不足、重复编号、库存不足、图片类型和大小限制、关联数据删除限制。

## 首次运行

首次运行前请确认已经安装：

- JDK 17 或更高版本
- Maven
- Node.js
- MySQL 8.0

在项目根目录执行：

```powershell
npm install
npm run install:all
```

设置 MySQL 密码环境变量：

```powershell
$env:DB_PASSWORD="@"
```

如果需要使用 AI 助手，请复制本地密钥模板：

```powershell
copy backend\.env.local.example backend\.env.local
```

然后打开 `backend/.env.local`，把 `ZHIPU_API_KEY=` 后面改成你的智谱 API Key。这个文件已被 `.gitignore` 忽略，不要打包提交。

启动前后端：

```powershell
npm run dev
```

访问地址：

- 前端：http://localhost:5173/login
- 后端：http://localhost:3000
- Swagger 文档：http://localhost:3000/api/docs

## 电脑重启后的运行流程

1. 确认 MySQL 已启动。

   可以在 Windows“服务”里启动 MySQL，也可以在 PowerShell 中查看：

   ```powershell
   Get-Service MySQL*
   ```

2. 打开 PowerShell，进入项目根目录。

   ```powershell
   cd "D:\大学\学习\web应用开发项目\code"
   ```

3. 设置本次终端的数据库密码。

   ```powershell
   $env:DB_PASSWORD="@Xuanxuan13579"
   ```

   如需使用 AI 助手，确认 `backend/.env.local` 已填写：

   ```powershell
   ZHIPU_API_KEY=你的智谱 API Key
   ```

4. 启动项目。

   ```powershell
   npm run dev
   ```

5. 等待终端出现前端和后端启动信息后，打开：

   ```text
   http://localhost:5173/login
   ```

6. 使用管理员账号登录：

   ```text
   admin / admin123
   ```

## 常见问题

### 1. Maven clean 删除 jar 失败

原因通常是后端已经在运行，Windows 不允许删除正在占用的 jar 文件。

解决方式：先关闭旧的 Java 后端进程，再重新运行 `npm run dev`。

### 2. 后端连接 MySQL 失败

请检查：

- MySQL 是否启动。
- `DB_PASSWORD` 是否设置正确。
- MySQL root 用户是否能正常登录。
- 默认数据库连接地址是否为 `localhost:3306/inventory_db`。

### 3. 端口被占用

默认端口：

- 前端：5173
- 后端：3000

如果端口被占用，先关闭旧项目进程再启动。

## 验证命令

前端构建：

```powershell
npm --prefix frontend run build
```

后端编译：

```powershell
mvn -f backend/pom.xml -DskipTests test
```

## 项目结构

```text
backend/   SpringBoot API、JPA 实体、JWT 安全认证、Swagger、MySQL 配置、AI 实时数据上下文
frontend/  Vue3 页面、路由、接口封装、Element Plus 组件、ECharts 图表
docs/      测试报告和数据库设计说明
```
