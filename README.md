# AI智能商品销售系统

本项目是在原有商品销售系统基础上扩展的 AI 应用项目，包含 Vue 前端、SpringBoot 后端、MySQL 数据库和独立 FastAPI AI 微服务。

## 核心功能

- 用户端商品浏览、搜索、购物车、收藏、下单。
- 后台商品、分类、订单、库存、公告、用户管理。
- ECharts 销售统计可视化。
- AI 智能导购：根据自然语言需求推荐商品。
- AI 商品文案生成：辅助管理员生成标题、简介、详情和营销语。
- AI 销售分析：基于订单、销量、库存数据生成经营建议。

## 技术栈

- 前端：Vue 2、Element UI、Axios、ECharts。
- 后端：SpringBoot 3、MyBatis-Plus、MySQL、Spring Security。
- AI 微服务：Python、FastAPI、LangChain、FAISS、大模型 API 兼容层。
- 数据库：MySQL。

## 目录结构

```text
源码/
  vue/          # Vue 前端
  springboot/   # SpringBoot 后端
  ai-service/   # FastAPI AI 微服务
```

## 本地配置

后端真实配置不提交到 GitHub。首次运行时复制示例配置：

```bash
copy springboot\src\main\resources\application-example.properties springboot\src\main\resources\application.properties
```

然后修改数据库密码、邮箱授权码等本地参数。

AI 微服务配置：

```bash
copy ai-service\.env.example ai-service\.env
```

默认 `LLM_PROVIDER=mock`，不配置大模型 Key 也可以演示。

## 启动顺序

1. 启动 MySQL，并导入数据库脚本。
2. 启动 FastAPI AI 微服务。
3. 启动 SpringBoot 后端。
4. 启动 Vue 前端。

详见：`../文档/AI功能部署说明.md`