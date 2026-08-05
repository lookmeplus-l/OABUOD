# OABUOD

OABUOD 是一款基于 Java 原生开发的 Android AI 客户端。它采用「自定义 UI + 后台驱动豆包网页」的架构：App 展示自有的原生界面，后端隐藏 WebView 打开豆包（Doubao）官方网页，用户在 App 界面上的操作会同步到豆包网页执行，豆包的回复再同步回 App 界面展示。

- **应用名称**：OABUOD
- **包名**：`fdb.r23studio.ai`
- **开发语言**：Java 原生开发
- **编译**：GitHub Actions（Gradle + Android SDK）

## 架构说明

```
┌─────────────────────────────────────────────┐
│  App 原生 UI（自定义界面）                    │
│  首页 / 对话 / 生图 / 视频 / 办公 / 扫码登录    │
└──────────────────┬──────────────────────────┘
                   │ 双向同步
                   ▼
┌─────────────────────────────────────────────┐
│  DoubaoEngine（后台隐藏 WebView）             │
│  加载豆包官方网页，注入桥接脚本驱动 DOM         │
│  发送消息 → 填入豆包输入框并发送               │
│  MutationObserver → 监听回复差异回传原生       │
└─────────────────────────────────────────────┘
```

- **DoubaoEngine**（`core/DoubaoEngine.java`）：后台 WebView 的驱动核心，负责加载豆包网页、注入桥接脚本、Cookie 会话保持，提供发送消息、切换功能、登录检测等能力。
- **JsBridge**（`core/JsBridge.java`）：`@JavascriptInterface` 桥接，网页通过 `DoubaoNative.onEvent(type, payload)` 把二维码、回复差异、登录状态回传原生。
- **doubao_bridge.js**（assets）：注入豆包网页的 DOM 适配脚本，封装输入框定位、React 受控输入、发送按钮点击、消息列表差异提取、二维码提取等逻辑。
- **doubao_dom_config.json**（assets）：DOM 选择器与各功能对应的豆包入口名称配置，豆包网页改版时可在此调整。

## 功能特性

- 自定义原生界面：首页、对话、生图、视频、办公五大页面
- 对话：App 输入消息自动填入豆包发送，回复流式同步回 App 气泡显示
- 生图 / 视频：App 输入提示词，映射到豆包对应创作功能，结果图同步展示
- 办公：AI 文档 / 表格 / PPT / 思维导图四种任务切换
- 扫码登录：后端 WebView 打开豆包登录页，JS 提取二维码元素并搬移到 App 界面，配合「登录完成 / 取消」按钮完成登录
- 会话保持：Cookie + localStorage 持久化，重启免重复登录
- 桌面 UA 模式获得豆包完整功能，支持文件上传、下载

## 目录结构

```
.
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   ├── doubao_bridge.js           # 注入豆包网页的 DOM 桥接脚本
│       │   └── doubao_dom_config.json     # 选择器与功能入口配置
│       ├── java/fdb/r23studio/ai/
│       │   ├── App.java                   # Application 单例（Engine/JsBridge 持有）
│       │   ├── MainActivity.java          # 主容器 + 底部导航
│       │   ├── core/                      # DoubaoEngine / JsBridge
│       │   ├── model/                     # ChatMessage / TaskResult
│       │   ├── ui/                        # 首页/对话/生图/视频/办公/登录页面
│       │   └── util/                      # 图片加载等
│       └── res/                           # 布局、主题、图标、字符串
├── .github/workflows/
│   └── android-build.yml                  # GitHub Actions 编译流水线
└── README.md
```

## 双向同步原理

1. **App → 豆包**：发送消息时调用 `DoubaoEngine.sendMessage(text, agentKey)`，注入脚本定位豆包输入框，用 React 原生 value setter + `input` 事件填充文本，再点击发送按钮（或回退触发回车）。生图/视频/办公先通过 `switchAgent()` 点击豆包侧边栏对应功能入口再发送。
2. **豆包 → App**：注入脚本在消息列表容器上挂 `MutationObserver`，内容变化后提取增量文本与新增图片（blob 转 data URL），通过 `DoubaoNative.onEvent('diff', ...)` 回传，App 端追加到当前气泡/结果卡片。

## 本地构建

环境要求：JDK 17、Android SDK（platform 34 / build-tools 34）、Gradle 8.7。

```bash
# 使用 gradle wrapper（CI 会自动生成）
gradle wrapper --gradle-version 8.7

# 构建 release APK（默认使用 debug 证书签名，可直接安装）
./gradlew assembleRelease
```

产物位于 `app/build/outputs/apk/release/app-release.apk`。

## GitHub Actions 编译

推送到任意分支、提交 Pull Request 或手动触发 `workflow_dispatch` 时自动编译：

1. 检出代码，安装 JDK 17、Android SDK、Gradle 8.7
2. 执行 `./gradlew assembleRelease`，上传 APK 到 Artifact（`OABUOD-release`）
3. 校验 APK 签名证书，确保使用固定签名
4. 推送 Git tag 时自动创建 GitHub Release 并附带 APK

### 固定签名（已配置）

签名 keystore 以 base64 编码存放在 `KEYSTORE_BASE64`，密码与别名明文存放在 `KEYSTORE_PASSWORD`、`KEY_PASSWORD`、`KEY_ALIAS`（仓库 `Settings -> Secrets and variables -> Actions`）。

| Secret | 内容 |
|--------|------|
| `KEYSTORE_BASE64` | 签名 keystore 文件的 base64 编码 |
| `KEYSTORE_PASSWORD` | keystore 密码 |
| `KEY_ALIAS` | 密钥别名 |
| `KEY_PASSWORD` | 密钥密码 |

**重要**：请妥善备份 `keystore/oabuod-release.jks` 与密码，丢失将无法对已发布应用发布更新。

## 适配说明

豆包网页结构变化时，主要调整 `assets/doubao_dom_config.json` 与 `assets/doubao_bridge.js`：

- `selectors.chatInput`：输入框选择器候选列表
- `selectors.sendButton`：发送按钮选择器候选列表
- `selectors.messageList`：消息列表容器选择器
- `selectors.qrImage`：登录二维码元素选择器
- `agents`：各功能对应的豆包侧边栏入口名称（对话/生图/视频/办公）

## 说明

- 本应用仅提供网页浏览与操作桥接层，不调用任何第三方 API Key，不采集任何用户数据
- 所有请求均由豆包官方网页发起并送达豆包官方服务器，登录凭证仅保存在应用本地的 WebView 数据中
