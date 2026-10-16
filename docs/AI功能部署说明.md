# AI智能商品销售系统功能说明与部署文档

## 1. 架构说明

本项目在原有“商品销售系统”基础上新增 AI 能力，整体采用三层对接架构：

- Vue 前端：负责用户端 AI 智能导购弹窗、后台商品文案生成按钮、后台销售统计 AI 分析卡片。
- SpringBoot 后端：继续作为业务系统入口和业务网关，提供 `/ai/**` 接口，统一接收前端请求并通过 HTTP 调用 Python AI 微服务。
- FastAPI AI 微服务：独立处理大模型调用、Prompt 编排、商品向量检索、销售分析和 AI 文案生成。

前端不直接调用 FastAPI，SpringBoot 不直接调用大模型 API。这样可以保证业务系统和 AI 能力解耦，后续替换模型或扩展 AI 功能时，只需要调整 Python 微服务。

## 2. 新增目录

- `源码/ai-service`：Python FastAPI AI 微服务。
- `源码/ai-service/sql/ai_extension.sql`：可选 AI 日志表和商品向量缓存表 SQL。
- `源码/springboot/src/main/java/org/example/springboot/controller/AiController.java`：SpringBoot AI 接口入口。
- `源码/springboot/src/main/java/org/example/springboot/service/AiService.java`：SpringBoot 调用 FastAPI 的业务服务。
- `源码/springboot/src/main/java/org/example/springboot/dto/ai`：AI 请求和响应 DTO。
- `源码/vue/src/components/front/AiGuideAssistant.vue`：用户端 AI 智能导购悬浮窗。
- `源码/vue/src/api/ai.js`：Vue 调用 SpringBoot AI 接口的 axios 封装。

## 3. 数据库改造

原有业务表无需改动，AI 功能直接读取已有 `product`、`category`、`review`、`order` 等表。

可选新增表：

- `ai_call_log`：记录 AI 功能调用日志，方便答辩展示可观测性。
- `ai_product_vector_cache`：预留商品向量缓存表，当前实际向量索引使用本地 FAISS 文件。

执行脚本位置：

```bash
源码/ai-service/sql/ai_extension.sql
```

## 4. AI 功能

### AI智能导购

用户在首页右下角点击 AI 悬浮按钮，输入自然语言购物需求。请求流程：

```text
Vue -> SpringBoot /ai/guide -> FastAPI /ai/guide -> FAISS商品检索 + LLM总结 -> 返回推荐商品
```

返回内容包含：导购说明、商品推荐列表、推荐理由、价格、库存、销量。

### AI商品文案生成

后台商品新增/编辑弹窗中新增“AI生成文案”按钮。管理员录入商品基础信息后点击按钮，系统生成：

- 商品标题
- 商品简介
- 商品详情描述
- 营销推荐语

生成结果会回填到商品表单，管理员仍可继续编辑后保存。

### AI销售分析

后台销售统计 ECharts 页面新增“AI智能分析”按钮。SpringBoot 汇总已有统计数据和低库存商品数据后传给 FastAPI，AI 返回：

- 热销商品分析
- 库存预警建议
- 补货建议
- 销售趋势总结
- 管理者总体结论

## 5. 配置说明

### SpringBoot 配置

文件：

```bash
源码/springboot/src/main/resources/application.properties
```

新增配置：

```properties
ai.service.base-url=http://localhost:8001
ai.service.timeout=45000
```

### FastAPI 配置

复制环境变量模板：

```bash
cd 源码/ai-service
copy .env.example .env
```

默认 `LLM_PROVIDER=mock`，不配置大模型 Key 也能演示功能。

如果接通义千问兼容模式，可配置：

```env
LLM_PROVIDER=dashscope
LLM_API_KEY=你的Key
LLM_MODEL=qwen-plus
```

如果接 OpenAI 或 OpenAI 兼容接口，可配置：

```env
LLM_PROVIDER=openai
LLM_API_KEY=你的Key
LLM_MODEL=gpt-4o-mini
LLM_BASE_URL=https://api.openai.com/v1
```

如果接智谱 AI，可配置：

```env
LLM_PROVIDER=zhipu
LLM_API_KEY=你的Key
LLM_MODEL=glm-4-flash
```

## 6. 启动顺序

推荐启动顺序：

```text
MySQL -> FastAPI AI微服务 -> SpringBoot -> Vue
```

也可以先启动 SpringBoot，但调用 AI 前必须保证 FastAPI 已启动。

### 1. MySQL

确认数据库 `db_aps` 已导入原项目 SQL，并可连接。

### 2. FastAPI AI 微服务

```bash
cd C:\Users\伊颖\Desktop\商品销售系统\源码\ai-service
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
python scripts\init_vector_store.py
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

健康检查：

```text
http://localhost:8001/health
```

### 3. SpringBoot

在 IDE 中运行：

```text
org.example.springboot.SpringbootApplication
```

或者 Maven 可用时运行：

```bash
cd C:\Users\伊颖\Desktop\商品销售系统\源码\springboot
mvn spring-boot:run
```

后端端口：

```text
http://localhost:1234
```

### 4. Vue

```bash
cd C:\Users\伊颖\Desktop\商品销售系统\源码\vue
npm.cmd run serve
```

前端端口：

```text
http://localhost:8080
```

## 7. 验证结果

已完成验证：

- FastAPI Python 文件语法检查通过。
- Vue 前端 `npm.cmd run build` 构建通过。

当前 Codex shell 未识别 `mvn` 命令，因此 SpringBoot 编译需要在已配置 Maven PATH 的终端或 IntelliJ IDEA 中执行。
