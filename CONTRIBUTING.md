# 贡献指南（Git 协作开发规范）

欢迎参与 **AI 智能商品销售系统** 的迭代开发。本项目采用 **GitHub Flow + 分支模型** 进行版本管理与协作，遵循以下规范可让代码评审与合并更顺畅。

## 1. 分支模型

| 分支 | 用途 | 说明 |
|---|---|---|
| `main` | 稳定发布分支 | 只接受来自 `develop` 的 release 合并，打 tag 发布 |
| `develop` | 集成分支 | 所有 feature 合并到这里，保持可运行 |
| `feature/*` | 功能开发分支 | 从 `develop` 拉出，开发完成合并回 `develop` |
| `hotfix/*` | 线上紧急修复 | 从 `main` 拉出，修复后合并回 `main` 和 `develop` |

```text
main        ────────────────────────● (v2.0.0)
                                     │
develop     ─────────●──●──●────────●
                    /   /    \      /
feature/xxx  ──────●  ●      ●──●
```

## 2. 协作流程

```text
1. 从 develop 拉取最新代码
   git checkout develop && git pull origin develop

2. 创建功能分支
   git checkout -b feature/<功能描述>

3. 开发并提交（遵循提交规范）
   git add .
   git commit -m "feat(ai): 新增多轮对话上下文管理"

4. 推送到远端并创建 Pull Request
   git push origin feature/<功能描述>

5. 在 GitHub 上发起 PR → develop，通过 CI 与评审后合并
```

## 3. Commit 提交规范（Conventional Commits）

格式：`<type>(<scope>): <subject>`

- **type**：`feat`（新功能）/ `fix`（修复）/ `perf`（性能）/ `refactor`（重构）/ `docs`（文档）/ `test`（测试）/ `build`（构建）/ `ci`（CI）/ `chore`（杂项）
- **scope**：`backend` / `frontend` / `ai` / `db` / `docker` / `docs` 等
- **subject**：简洁描述，中文或英文，动词开头

示例：

```text
feat(ai): 实现智能导购 SSE 流式输出
fix(backend): 修复支付接口库存并发超卖问题
perf(ai): 增加问答结果缓存，降低大模型调用成本
docs: 补充 docker-compose 一键启动说明
```

## 4. 代码规范

- **后端（Java/Spring Boot）**：遵循阿里 Java 开发手册核心规范；分层 Controller → Service → Mapper；异常统一由全局异常处理器捕获。
- **前端（Vue）**：组件化、模板语义化；接口调用统一收敛到 `src/api`。
- **AI 服务（Python/FastAPI）**：Pydantic 校验请求/响应；服务逻辑收敛到 `app/services`；所有对外接口必须有健康检查与异常兜底。

## 5. 合并要求（Merge 准入）

- [ ] CI（后端 mvn test / 前端 build / AI pytest）通过
- [ ] 无未解决的冲突
- [ ] 有对应测试（新功能至少 1 个用例）或说明手工验证方式
- [ ] 通过至少 1 人 Code Review

## 6. 发布流程

```text
1. 功能全部合入 develop 并验证通过
2. 从 develop 创建 release 分支（可选）
3. 合并到 main，打 tag（如 v2.0.0）
4. 在 GitHub Releases 编写发布说明
```
