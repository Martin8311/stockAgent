# Engineering Log

## 阶段 4 修复：LLM 结构化输出偏离 Schema

### 本地小模型把摘要字段返回成对象，导致前端展示原始 JSON

- 阶段：4
- 现象：AI 分析结果中 `investmentSummary` 被模型返回为对象，且把 `["AAPL", "NASDAQ"]` 放进 `assetName`，前端最终展示一整段 JSON，看起来像 Agent 输出异常。
- 影响：结构化分析页可读性下降；用户容易误以为系统把 NASDAQ 当成第二个持仓；如果直接透传模型文本，也会削弱金融合规和审计可控性。
- 原因：原 prompt 只列出了字段名，没有声明字段类型；`Asset: AAPL / NASDAQ / USD` 这种格式对小模型有歧义，容易把交易所理解成资产；后端原解析逻辑直接反序列化 DTO，失败后把原始模型内容包进摘要字段。
- 定位过程：检查 `InvestmentAnalysisContent` 发现 `investmentSummary` 期望是 `String`；检查 `InvestmentAnalysisService.buildPrompt` 发现 prompt 未约束 `investmentSummary` 不允许 object/array；检查 fallback 发现解析失败会把 raw content 作为 summary 返回。
- 解决方案：prompt 明确 JSON 字段类型，拆分 `Asset symbol`、`Exchange/venue`、`Currency`，说明 exchange 是交易场所；解析层改为逐字段读取 `JsonNode`，当 `investmentSummary` 是对象时使用服务端 quote 修复为一句摘要；非法 JSON fallback 不再泄露原始模型输出。
- 验证方式：新增 `InvestmentAnalysisServiceTest` 覆盖对象摘要修复和非法 JSON 降级；执行后端 `mvn test`，20 个测试全部通过。
- 面试表达：LLM 不能被当成可信结构化数据源。我的处理方式是“prompt 约束 + 服务端 schema 收口 + 容错修复 + 合规降级”，这样即便本地小模型输出偏离，也不会直接污染前端、审计记录或投资辅助结论。

## 阶段 4：Spring AI、模型选择与 token 计费

### 只读事务里调用带审计写入的行情服务导致 MySQL 500

- 阶段：4
- 现象：前端持仓工作台点击“保存交易”后显示 500；日志中实际报错路径是 `GET /api/portfolio/summary`，MySQL 报 `Connection is read-only. Queries leading to data modification are not allowed`。
- 影响：交易保存后前端刷新组合摘要失败，用户感知为“保存交易接口 500”，实际是保存后的 summary 查询失败。
- 原因：`PortfolioService.getSummary` 使用 `@Transactional(readOnly = true)`，但阶段 3 接入市场数据后，summary 内部调用 `MarketDataService.getQuoteForPortfolioValuation`，后者复用了公开 quote 查询逻辑并写入 `MARKET_DATA_QUOTE_REQUESTED` 审计事件。MySQL 严格执行只读连接限制，因此在只读事务里写审计被拦截。
- 定位过程：先查 `.dev/logs/backend.log`，发现 500 栈指向 `/api/portfolio/summary` 而不是 `POST /api/portfolio/transactions`；继续向上追踪到 `MarketDataService.getQuote` 的审计写入。
- 解决方案：拆分行情获取和审计记录。公开 `getQuote` 继续写审计；内部组合估值 `getQuoteForPortfolioValuation` 只取 quote，不写审计，避免污染只读事务，也避免组合摘要每个持仓都刷大量行情审计事件。
- 验证方式：新增 `MarketDataServiceTest`，断言公开 quote 会写 audit，portfolio valuation quote 不写 audit；后端 `mvn test` 通过 18 个测试。
- 面试表达：这个问题体现了事务边界的重要性。只读查询链路中不能隐藏写操作，尤其 MySQL 会真实 enforcement。修复时我没有粗暴去掉 readOnly，而是把“用户显式行情查询”和“内部估值查询”的审计语义拆开。

### Spring AI 版本要跟 Spring Boot 版本对齐

- 阶段：4
- 现象：项目使用 Spring Boot 3.5.x，而 Spring AI 最新主线可能面向更新的 Spring Boot 版本。
- 影响：如果盲目引入最新 Spring AI，容易出现依赖树不兼容、自动配置失败或 API 签名不匹配。
- 原因：Spring AI 仍在快速演进，starter、自动配置属性和 ChatModel API 都有版本差异。
- 定位过程：先检查本地 Maven 缓存，确认已有 `spring-ai-bom`、`spring-ai-starter-model-ollama`、`spring-ai-model` 的 1.0.9 版本，再用后端测试验证 Spring Boot 3.5.3 可正常加载上下文。
- 解决方案：阶段 4 固定使用 Spring AI 1.0.9，并把 Ollama 作为官方 Spring AI 接入口；MiniMax starter 本地未缓存，因此先做 HTTP/OpenAI-compatible 适配层，后续可替换为官方 starter。
- 验证方式：`mvn test` 通过，AI Controller、Flyway V4、JPA、Security 和 MockMvc 测试全部成功。
- 面试表达：AI 工程不是只调一个模型 API，首先要处理框架版本、自动配置、依赖可用性和可替换边界，否则项目会被供应商 SDK 或版本冲突拖住。

### 免费本地模型和付费模型需要统一模型目录

- 阶段：4
- 现象：用户希望本地 Ollama Qwen2.5 3B 免费提供服务，同时 MiniMax 作为付费用户能力预留，并且用户可以选择模型。
- 影响：如果直接在前端写死模型，后续加套餐、审批、灰度和禁用模型都会很难。
- 原因：模型不仅是调用地址，还包含 provider、启用状态、是否本地、是否免费、是否需要 API Key、计费模式和状态说明。
- 定位过程：抽象出 `/api/ai/models`，让后端返回模型目录，由后端决定模型是否可用。
- 解决方案：新增 `AiModelCatalog` 和 `AiModelDescriptor`。Ollama 默认免费可用；MiniMax 只有在 `MINIMAX_API_KEY` 存在，并且 `AI_TEST_MODE=true` 或 `AI_PAID_ACCESS_ENABLED=true` 时才启用。
- 验证方式：`AiAnalysisControllerTest` 断言模型目录返回 Ollama 和 MiniMax，并带有 freeTier、paidTier、testModeFree 等计费元数据。
- 面试表达：我把“模型选择”设计成后端模型目录，而不是前端枚举。这样可以把计费、权限、灰度、供应商状态和审批策略集中在服务端治理。

### token 计费先做记录，不急着做扣费

- 阶段：4
- 现象：用户要求采用 token 计费模式，但当前还没有用户余额、套餐、支付订单和账单系统。
- 影响：如果直接做扣费，会引入支付、幂等、退款、余额并发等大量复杂度，超出阶段 4 目标。
- 原因：计费系统应该先有可信用量记录，余额扣减可以在后续付费模块中基于用量记录演进。
- 定位过程：拆分“用量记录”和“资金扣减”两个职责，本阶段只落 `ai_token_usage_record`。
- 解决方案：记录 prompt tokens、completion tokens、total tokens、usageSource、estimatedCost、currency、billable、testMode。真实供应商价格不硬编码，通过环境变量注入。
- 验证方式：AI 分析测试断言 tokenUsage 返回 totalTokens、usageSource=MOCK、testMode=true；Flyway V4 创建分析任务和 token 用量表。
- 面试表达：token 计费的第一步不是收钱，而是建立可审计、可重放、可对账的 usage ledger。这样后面接套餐和支付时有可靠事实源。

### AI 输出必须服务端合规兜底

- 阶段：4
- 现象：模型可能输出“必买”“保证收益”等不合规内容，或者不按 JSON 结构返回。
- 影响：投资助手如果直接透传模型文本，会带来金融合规风险，也会破坏前端结构化展示和审计能力。
- 原因：LLM 输出天然不稳定，需要后端在模型之后做结构化解析、兜底包装和合规后处理。
- 定位过程：把 prompt 约束、JSON 解析、fallback 包装、禁止用语替换、风险提示补齐放在 `InvestmentAnalysisService`。
- 解决方案：模型返回内容先解析为 `InvestmentAnalysisContent`，解析失败则包装成低置信度结构化响应；服务端追加行情风险、合规 required warnings 和用户画像缺口提示。
- 验证方式：AI 分析接口测试断言返回结构化 summary、riskWarnings、disclaimer 和 tokenUsage。
- 面试表达：金融场景不能把 LLM 当最终裁判。我的设计是“模型生成候选内容，服务端负责合规收口”，这样才能审计和治理。

### 测试不能依赖本地 Ollama 或外部 MiniMax

- 阶段：4
- 现象：阶段 4 引入模型调用后，自动化测试如果真实访问 Ollama 或 MiniMax，会依赖本机服务、网络和 API Key。
- 影响：CI 和本地测试会不稳定，甚至可能产生外部费用。
- 原因：模型调用属于外部依赖，必须和业务编排测试解耦。
- 定位过程：测试只需要验证模型选择、结构化响应、计费记录和审计链路，不需要验证真实模型质量。
- 解决方案：`application-test.yml` 开启 `AI_MOCK_RESPONSES=true` 和 `AI_TEST_MODE=true`，使用确定性响应，同时仍走完整 service/controller/billing/audit 流程。
- 验证方式：后端 `mvn test` 通过 16 个测试，不需要本地 Ollama 运行，也不需要 MiniMax API Key。
- 面试表达：我把“模型质量验证”和“业务流程验证”分开。单元/集成测试验证系统稳定性，真实模型效果留给人工评估、回归样例和后续评测集。

本文件持续记录项目开发过程中遇到的问题、坑点、难点、原因、定位过程和解决方案。目标是沉淀可复盘的工程经验，方便后续写简历、准备面试和做项目演示。

## 记录模板

```text
### 标题

- 阶段：
- 现象：
- 影响：
- 原因：
- 定位过程：
- 解决方案：
- 验证方式：
- 面试表达：
```

## 阶段 0：项目初始化

### Maven 不在 PATH，但本机存在可用 Maven 分发

- 阶段：0
- 现象：执行 `mvn -version` 失败，PowerShell 提示无法识别 `mvn`。
- 影响：不能直接用常规 Maven 命令创建或验证 Spring Boot 后端。
- 原因：本机没有把 Maven 放到 PATH，但用户目录下存在 Maven Wrapper 下载缓存。
- 定位过程：先检查 `java -version`、`mvn -version`，再确认 `$HOME/.m2/wrapper/dists/.../mvn.cmd` 是否存在。
- 解决方案：使用本机已有的 Maven 分发路径执行 `mvn.cmd test`，不额外安装全局 Maven。
- 验证方式：`mvn.cmd -version` 成功，后续 `mvn test` 通过。
- 面试表达：遇到工具链缺失时，我没有直接要求安装软件，而是先盘点本地可用运行时，复用已有 Maven 分发完成构建验证。

### H2 测试库与 Hibernate schema 校验大小写不一致

- 阶段：0
- 现象：Flyway 已执行 `V1__create_audit_event.sql`，但 Hibernate 启动时报 `Schema-validation: missing table [audit_event]`。
- 影响：Spring Boot 测试上下文无法启动，公共 API 测试全部失败。
- 原因：H2 默认标识符处理和 Hibernate 校验期望不一致，表实际存在但按大小写策略查询时匹配失败。
- 定位过程：查看 Surefire 报告，确认 Flyway 日志中已经成功执行 migration，再对比 Hibernate 缺表错误，判断不是脚本未加载，而是测试数据库兼容模式问题。
- 解决方案：将测试 datasource URL 调整为 `MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1`。
- 验证方式：重新运行后端测试，Flyway、JPA、Security、MockMvc 测试全部通过。
- 面试表达：这个问题不是简单的“表没建”，而是测试数据库方言与 ORM 校验之间的细节不一致。定位时要同时看 migration 日志和 Hibernate schema validation 日志。

### npm PowerShell 脚本被执行策略拦截

- 阶段：0
- 现象：执行 `npm -v` 时 PowerShell 报 `npm.ps1` 被系统执行策略禁止。
- 影响：不能直接在 PowerShell 中调用 `npm`。
- 原因：Windows PowerShell execution policy 阻止运行 npm 的 `.ps1` shim。
- 定位过程：`node -v` 可用但 `npm -v` 失败，错误指向 `npm.ps1`。
- 解决方案：使用 `cmd /c npm ...` 调用 `npm.cmd`，绕过 PowerShell 脚本策略。
- 验证方式：`cmd /c npm -v` 成功返回版本号。
- 面试表达：跨平台前端工具链在 Windows 上常见执行策略问题，我会优先切换到官方 `.cmd` 入口，而不是修改用户机器的全局安全策略。

### npm 默认 cache 目录无写入权限

- 阶段：0
- 现象：`npm install` 访问官方 registry 后失败，报 `EPERM`，无法写入 `D:\nodejs\node_cache`。
- 影响：前端依赖无法安装，无法生成 lockfile 和验证构建。
- 原因：npm 默认 cache 配置指向 Node 安装目录下的受限路径。
- 定位过程：先发现 `npmmirror` registry 请求长时间无输出，再切官方 registry 加短超时，错误明确指向 cache 路径无权限。
- 解决方案：使用项目内 cache：`npm install --cache .npm-cache`，并将 `.npm-cache/` 加入 `.gitignore`。
- 验证方式：依赖安装成功，`found 0 vulnerabilities`。
- 面试表达：依赖安装失败不一定是包版本问题，也可能是 registry、cache 或权限。把 cache 收敛到项目目录能提高可复现性，也减少对机器全局状态的依赖。

### Vite 7 默认配置加载会写入 node_modules/.vite-temp

- 阶段：0
- 现象：前端构建时报 `EPERM: operation not permitted, mkdir 'node_modules/.vite-temp'`。
- 影响：TypeScript 检查通过后，Vite 无法加载配置并构建。
- 原因：Vite 7 默认 `configLoader=bundle` 会在 `node_modules/.vite-temp` 下生成临时文件，而当前环境限制该目录写入。
- 定位过程：先将 `vite.config.ts` 改为 `vite.config.mjs`，发现仍写 `.vite-temp`，再查看 `npx vite --help`，确认存在 `--configLoader runner/native`。
- 解决方案：将 `dev`、`build`、`preview` 脚本统一加上 `--configLoader runner`。
- 验证方式：`npm run build` 成功生成生产构建。
- 面试表达：这类问题体现了对构建工具内部行为的理解。不是盲目降级依赖，而是查 CLI 能力，用 runner 模式避开不必要的临时打包写入。

### Vite 构建产物目录创建受 sandbox 限制

- 阶段：0
- 现象：Vite 使用 runner 后继续失败，报无法创建 `frontend/dist`。
- 影响：生产构建无法落盘。
- 原因：当前受控工作区对部分由子进程创建的新目录需要显式授权。
- 定位过程：手动用 PowerShell 创建 `dist`、`out-test`、`build-output` 均被拒绝，确认不是 Vite 逻辑问题，而是执行环境权限问题。
- 解决方案：按权限流程请求构建命令授权，让 `npm run build` 在项目目录写入构建产物。
- 验证方式：授权后 `npm run build` 成功，输出 `dist/index.html`、CSS 和 JS bundle。
- 面试表达：在受限执行环境里，构建失败要区分代码错误、工具行为和运行环境权限。这里通过最小复现实验证明是权限问题。

### Git 初始化和提交需要处理仓库元数据权限与 author identity

- 阶段：0
- 现象：`git init` 初次写 `.git/description` 失败；提交时又因缺少 author identity 失败。
- 影响：阶段 0 无法完成 Git 提交。
- 原因：受限环境写 `.git` 元数据需要授权；新仓库没有配置 `user.name` 和 `user.email`。
- 定位过程：先看 `git init` 报错路径，再检查 `.git` 已半初始化；提交失败后确认是 identity 缺失。
- 解决方案：请求授权补完 `git init`；只设置仓库级 `git config user.name` 和 `git config user.email`，不修改全局配置；提交后将默认分支改为 `main`。
- 验证方式：提交成功，commit 为 `8e727fb chore: initialize phase 0 project foundation`，`git status --short --branch` 显示工作区干净。
- 面试表达：我会优先做仓库级配置，避免污染开发者全局环境；同时保证每个阶段都有清晰 commit，便于展示项目演进。

## 后续维护约定

- 每个阶段结束前补充本阶段至少一条工程记录。
- 如果阶段没有明显故障，也记录关键设计取舍，例如为什么选 JWT、为什么引入某个抽象、为什么暂不引入消息队列。
- 高价值记录优先包含可复现现象、定位证据、最终验证命令和面试表达。
- 与金融合规、安全、审计、Sandbox、Skill 审批相关的问题必须记录，因为这些是本项目的核心亮点。

## 阶段 1：用户认证与权限

### Spring 构造器选择在存在测试构造器时不明确

- 阶段：1
- 现象：后端测试启动失败，报 `JwtTokenService`: `No default constructor found`。
- 影响：Spring Boot 上下文无法启动，认证相关测试全部失败。
- 原因：`JwtTokenService` 同时存在生产构造器和包级测试构造器，Spring 没有明确的注入构造器选择。
- 定位过程：查看 Surefire 输出中的 Bean 创建链路，从 `AuthController` 追到 `SecurityConfig`、`JwtAuthenticationFilter`，最终定位到 `JwtTokenService` 实例化失败。
- 解决方案：在生产构造器上显式添加 `@Autowired`，测试构造器保留为包级可见。
- 验证方式：重新运行 `mvn test`，Spring 上下文成功启动。
- 面试表达：当为了可测试性增加额外构造器时，要显式声明 DI 构造器，否则框架反射构造器选择可能变得不确定。

### ResponseStatusException 与统一 API 错误格式冲突

- 阶段：1
- 现象：重复注册邮箱时接口返回 Spring ProblemDetails JSON，而不是项目统一的 `ErrorResponse`。
- 影响：前端无法稳定依赖统一错误结构，测试也无法断言 `errorCode`。
- 原因：启用了 `spring.mvc.problemdetails.enabled=true` 后，`ResponseStatusException` 会被 Spring 的 ProblemDetails 流程处理。
- 定位过程：MockMvc 输出显示状态码是 409，但响应体是 `application/problem+json`，说明不是业务逻辑失败，而是异常映射路径不符合预期。
- 解决方案：新增项目级 `ApiRequestException`，由 `GlobalExceptionHandler` 显式映射为 `ErrorResponse`。
- 验证方式：重复注册测试通过，返回 `errorCode=REQUEST_ERROR`。
- 面试表达：统一 API 错误格式不能只靠默认异常，尤其在 Spring Boot 3 的 ProblemDetails 机制下，业务异常最好有项目级抽象和集中映射。

### 一键启动脚本需要兼顾 Windows 工具链差异

- 阶段：1
- 现象：本地开发涉及 Docker Compose、Maven、npm、PowerShell 执行策略、日志和进程停止，手动启动容易漏步骤。
- 影响：项目演示和面试复现成本变高，也不利于后续持续推进。
- 原因：当前机器上 `mvn` 不一定在 PATH，`npm.ps1` 可能被 PowerShell 策略拦截，前后端又需要分别保持长进程。
- 定位过程：复盘阶段 0 和阶段 1 的启动路径，确认最稳定的组合是 PowerShell 编排、`cmd /c npm` 调用 npm、自动发现 Maven 路径、后台进程日志落盘。
- 解决方案：新增 `start-dev.cmd`、`scripts/start-dev.ps1` 和 `scripts/stop-dev.ps1`。启动脚本负责启动 MySQL、等待健康检查、启动后端和前端、记录 PID 与日志；停止脚本按 PID 清理进程，可选停止 MySQL。
- 验证方式：对 PowerShell 脚本做语法解析检查，避免脚本本身存在语法错误；运行时日志输出到 `.dev/logs/` 便于排查。
- 面试表达：我不仅实现业务功能，也关注工程可运行性。通过一键启动和日志/PID 管理，让项目更接近真实团队开发体验，而不是只停留在代码能编译。

### 一键启动失败时窗口直接关闭，缺少可见诊断信息

- 阶段：1
- 现象：用户双击 `start-dev.cmd` 后项目启动失败，但命令窗口立即关闭，看不到报错。
- 影响：本地演示无法自助排查，面试或录屏时会显得项目不可运行。
- 原因：`.cmd` 只透传 PowerShell 脚本，没有保留退出码、没有 `pause`，PowerShell 脚本也没有全局 transcript 和 trap。
- 定位过程：检查 `.dev/logs/` 发现没有后端/前端日志和 PID，说明脚本在启动服务前已经失败；随后直接在终端执行脚本复现错误。
- 解决方案：`start-dev.cmd` 捕获 `%ERRORLEVEL%`，成功或失败后保留窗口；`scripts/start-dev.ps1` 增加 `Start-Transcript`、全局 `trap` 和 `.dev/logs/start-dev.log`。
- 验证方式：故意触发 Docker 权限失败，窗口输出失败原因和日志路径，不再静默关闭。
- 面试表达：启动脚本不是简单拼命令，关键是失败可观测。CLI/脚本也要像后端服务一样有日志、错误边界和可操作提示。

### Docker Engine 权限不足导致 MySQL 无法启动

- 阶段：1
- 现象：`docker compose up -d mysql` 报 `Access is denied`，无法连接 `//./pipe/docker_engine`。
- 影响：默认 MySQL 开发环境无法启动，后端依赖数据库时整个项目不可用。
- 原因：Windows 上 Docker Desktop/Engine pipe 需要当前用户具备访问权限，当前终端或沙箱用户无法连接 Docker daemon。
- 定位过程：先运行一键脚本复现，再查看 Docker 输出，确认 Docker CLI 存在但 daemon pipe 被拒绝访问，不是 compose 文件语法问题。
- 解决方案：启动脚本先执行 `docker info` 探测 Engine 可用性；不可用时给出管理员权限/Docker Desktop 提示，并自动降级到 `local-h2` profile，保证本地演示可继续运行。需要强制 MySQL 时可加 `-RequireDocker`。
- 验证方式：在 Docker 不可访问环境下运行脚本，看到明确 warning，并进入 H2 fallback。
- 面试表达：我把基础设施依赖做成可探测、可降级、可强制的三种路径：默认追求真实 MySQL，演示环境保证可运行，严格模式保证问题不会被隐藏。

### PowerShell 5.1 与 native command/环境变量兼容问题

- 阶段：1
- 现象：脚本先后遇到 `RandomNumberGenerator.Fill` 不存在、Docker stderr warning 触发 trap、`Path`/`PATH` 重复导致 `Start-Process` 报重复键。
- 影响：同一脚本在不同 Windows/PowerShell 版本和受限运行环境中表现不一致。
- 原因：Windows PowerShell 5.1 基于 .NET Framework，不支持较新的 `RandomNumberGenerator.Fill` 静态方法；`$ErrorActionPreference = Stop` 会把部分 native stderr 记录当作终止错误；环境变量字典在 Windows 下大小写不敏感，重复的 `Path`/`PATH` 会影响子进程创建。
- 定位过程：逐次运行脚本，观察每次失败的最小错误；确认失败发生在服务启动前、Docker 探测阶段和 `Start-Process` 创建阶段。
- 解决方案：随机密钥生成改为 `RandomNumberGenerator.Create().GetBytes()`；native 命令封装中临时将错误策略切回 `Continue` 并使用 exit code 判断；启动前规范化进程级 PATH 变量。
- 验证方式：PowerShell 解析检查通过；脚本可以继续走到 Docker fallback、后端/前端启动和健康检查阶段。
- 面试表达：跨平台脚本要考虑 shell、运行时和环境变量模型，不应只在一台机器的“幸运路径”上验证。

### 后台 Maven 子进程与本地仓库权限/上下文不一致

- 阶段：1
- 现象：后台启动 Spring Boot 时，Maven 先找不到本地依赖并访问 Maven Central，随后在受限环境中写 `C:\Users\admin\.m2\repository` 报 `AccessDeniedException`。
- 影响：后端无法在当前沙箱里通过 `spring-boot:run` 启动，但单独执行后端测试可以通过。
- 原因：Codex 沙箱中的后台子进程用户上下文与 `$HOME`/本地 Maven 仓库权限不完全一致；同时网络访问受限，缺失的 Maven plugin 依赖不能即时下载。
- 定位过程：查看 `.dev/logs/backend.log`，区分应用启动失败和构建工具失败；再执行 `mvn test` 验证业务代码、Flyway、JPA、Security 测试均通过。
- 解决方案：生成的后端 run script 显式传入 `-Dmaven.repo.local=$HOME\.m2\repository`，减少子进程仓库解析漂移；健康检查发现后端提前退出时让一键脚本返回失败并指向后端日志。
- 验证方式：`mvn test` 通过 7 个后端测试；脚本在后端提前退出时返回非零退出码并保留错误提示。
- 面试表达：排查启动失败时要先分层：是应用代码、数据库、构建工具、网络还是权限。这个案例里通过日志把 Maven 仓库权限问题和后端业务正确性拆开验证。

### 停止脚本只停止 wrapper 进程，Maven/Vite 子进程残留

- 阶段：1
- 现象：`stop-dev.ps1` 停止 PID 文件里的 PowerShell wrapper 后，仍能看到 Java、Node 和 cmd 子进程占用端口。
- 影响：再次启动可能端口冲突，演示环境也会出现“明明停止了但服务还在”的错觉。
- 原因：`Start-Process` 启动的是 PowerShell wrapper，wrapper 再启动 Maven/Vite；单独 `Stop-Process` wrapper 不一定会递归停止 Maven、Spring Boot、npm、Vite 这些后代进程。
- 定位过程：停止脚本执行后检查 `Get-Process java,node,cmd`，再通过 `Win32_Process` 查看 ParentProcessId 和 CommandLine，确认残留进程全部来自本项目启动链路。
- 解决方案：停止脚本优先使用 `taskkill /T /F` 停整棵进程树；PID 文件不存在时再按命令行包含项目根目录的 Java/Node/CMD 做 orphan 兜底清理。
- 验证方式：H2 模式启动成功后执行停止脚本；提升权限清理后确认不再存在本项目 Java/Node/CMD 残留进程。
- 面试表达：本地开发脚本也要做资源生命周期管理。只记录 PID 不够，还要考虑 wrapper、shell、构建工具和实际服务进程之间的父子关系。

### 前端中英文切换先做本地 i18n，合规公告保留后端演进空间

- 阶段：1
- 现象：前端页面需要支持中英文切换，但当前后端合规免责声明接口只返回英文内容。
- 影响：如果直接把中英文硬编码散落在组件里，后续页面增多后维护成本会快速上升；如果立刻改后端语言协商，又会扩大当前阶段改动范围。
- 原因：国际化能力横跨前端展示、后端 API、用户偏好和合规文案来源，应该分阶段落地。
- 定位过程：检查 `App.tsx` 后发现当前文案集中在单页组件内，适合先抽出前端本地翻译字典；合规公告可以先在前端对已知结构做中文映射。
- 解决方案：新增 `frontend/src/i18n.ts`，集中管理语言类型、翻译字典、枚举标签和合规公告本地化；语言选择写入 `localStorage`，页面顶部提供 EN/中文切换。
- 验证方式：执行 `npm run build`，TypeScript 和 Vite 构建通过。
- 面试表达：我没有为了一个 UI 切换引入完整 i18n 框架，而是先用类型安全的轻量字典满足当前阶段；同时保留后续通过 `Accept-Language` 或用户偏好由后端返回合规文案的演进路径。

## 阶段 2：持仓管理与组合风险摘要

### 用交易流水推导持仓，而不是直接维护持仓快照

- 阶段：2
- 现象：持仓管理既要支持 CRUD，又要计算平均成本、已实现盈亏、未实现盈亏和组合风险；如果直接维护一张持仓快照表，删除或补录历史交易时很容易出现数据不一致。
- 影响：错误的持仓和成本会直接影响后续 PortfolioAgent、RiskAgent 的分析输入，属于投资辅助系统中的高风险数据质量问题。
- 原因：持仓是交易流水的派生状态，交易才是可审计事实；阶段 2 尚未引入复杂事件溯源，但可以先用交易表作为事实源。
- 定位过程：对买入、卖出、删除交易三个场景建模，确认平均成本、已实现盈亏和剩余成本都可以由按时间排序的交易流水重放得到。
- 解决方案：新增 `investment_asset` 和 `portfolio_transaction` 表，Service 每次按用户交易流水重算持仓；卖出前校验不会出现负持仓；删除交易后重新计算摘要。
- 验证方式：新增 `PortfolioControllerTest` 覆盖买入/卖出计算、超卖拒绝和删除交易后重算；后端 `mvn test` 通过 10 个测试。
- 面试表达：我把交易记录作为审计事实，把持仓作为可重算视图。这样比直接写持仓快照更容易解释一致性、审计和后续 Agent 输入可信度。

### 阶段 2 估值暂用最近交易价，避免提前引入行情复杂度

- 阶段：2
- 现象：组合市值和未实现盈亏需要当前价格，但阶段 3 才规划市场数据接入。
- 影响：如果阶段 2 直接接外部行情，会引入 API Key、限流、数据源可靠性和审计问题，扩大当前阶段边界。
- 原因：当前目标是持仓管理闭环，而不是行情系统；最近交易价可以作为本地 demo 的临时估值基线。
- 定位过程：梳理后续路线图，确认市场数据抽象、mock 数据和外部适配器在阶段 3 单独交付更清晰。
- 解决方案：资产表保存 `latest_price`，录入交易时用该交易价格更新；组合摘要基于 `latest_price` 计算市值和未实现盈亏，并明确输出风险免责声明。
- 验证方式：买入后卖出测试中，资产最新价更新为卖出价，组合市值和盈亏按最新价计算通过断言。
- 面试表达：阶段交付要控制复杂度。我先让持仓闭环可运行，再把行情接入作为独立阶段处理，避免外部依赖污染核心账本逻辑。

## 阶段 3：市场数据接入抽象

### 先做 Provider 抽象和 Mock 行情，不直接接真实外部数据

- 阶段：3
- 现象：组合估值需要市场价格，但真实行情 API 会带来 API Key、限流、费用、授权范围、数据延迟和合规责任。
- 影响：如果过早接真实外部源，阶段 3 会被外部网络和供应商细节拖住，也可能把真实行情误用于演示中的投资判断。
- 原因：当前目标是建立可替换的数据源边界和结构化输出契约，而不是追求实时行情覆盖。
- 定位过程：沿着后续 Agent 需求反推 quote DTO，确认至少要包含价格、来源、时间、置信度、假设、风险提示和免责声明。
- 解决方案：新增 `marketdata` 模块，定义 `MarketDataProvider`、`MarketQuote`、Provider descriptor；默认启用 deterministic mock provider，外部 provider 仅作为 disabled placeholder。
- 验证方式：新增 `MarketDataControllerTest` 覆盖 provider 列表、mock quote、认证要求；后端 `mvn test` 通过 12 个测试。
- 面试表达：我把“可运行 demo”和“真实外部集成”分开：先稳定契约、审计和风险披露，再在后续阶段用同一个接口替换为真实数据源。

### 组合估值接入行情服务时保留 fallback

- 阶段：3
- 现象：Portfolio summary 现在可以使用 MarketDataService 报价，但行情 provider 可能禁用或不可用。
- 影响：如果没有 fallback，市场数据异常会导致整个持仓摘要不可用；如果静默失败，又会隐藏估值来源。
- 原因：投资研究系统要区分账本数据和行情数据，账本应尽量可用，行情失败时应退回可解释的本地估值。
- 定位过程：检查阶段 2 的 `InvestmentAsset.latestPrice`，它已经保存了最近交易价，可作为最低限度 fallback。
- 解决方案：持仓估值优先调用 MarketDataService；异常时回落到资产 latest price。quote 本身仍包含 sourceType、assumptions、riskWarnings，前端展示来源和风险。
- 验证方式：Portfolio 测试断言 AAPL 市值改用 mock quote `189.32`，证明组合估值已由 MarketDataService 驱动。
- 面试表达：这体现了降级设计：行情服务增强估值质量，但账本和组合摘要不被外部数据源单点拖垮。
