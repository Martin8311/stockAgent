# Engineering Log

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
