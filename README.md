# OABUOD

OABUOD 是一款基于 Java 原生开发的 Android AI 客户端，本质是一个封装了豆包（Doubao）官方网页版的 WebView 应用。用户在应用内的所有操作都发生在豆包官方网页中，请求直接发送至豆包官方服务器。

- **应用名称**：OABUOD
- **包名**：`fdb.r23studio.ai`
- **开发语言**：Java 原生开发
- **编译**：GitHub Actions（Gradle + Android SDK）

## 功能特性

- 内置 WebView 加载豆包官方网页（https://www.doubao.com），请求直接发送到豆包官方服务器
- 支持扫码登录、账号密码登录，登录状态通过 Cookie / localStorage 持久化保存，重启应用无需重复登录
- 完整适配网页端能力：
  - AI 对话
  - 图片生成（支持参考图上传，已适配 WebView 文件选择器）
  - 视频 / 办公任务等网页端功能
- 摄像头、麦克风权限适配（支持网页内拍照、语音输入）
- 文件下载能力（通过系统下载管理器保存到 `Download` 目录）
- 桌面端 UA 模式，获得完整网页功能

## 目录结构

```
.
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/fdb/r23studio/ai/
│       │   └── MainActivity.java      # WebView 封装 + 权限 + 文件上传/下载
│       └── res/                       # 布局、主题、图标、字符串
├── .github/workflows/
│   └── android-build.yml              # GitHub Actions 编译流水线
├── build.gradle
├── settings.gradle
└── gradle.properties
```

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

推送到 `main` / `master` 分支、提交 Pull Request 或手动触发 `workflow_dispatch` 时自动编译：

1. 检出代码
2. 安装 JDK 17、Android SDK、Gradle 8.7
3. 生成 Gradle wrapper 并执行 `./gradlew assembleRelease`
4. 上传 APK 到 Actions Artifact（名称 `OABUOD-release`）
5. 若推送的是 Git tag，会自动创建 GitHub Release 并附带 APK

未配置签名 secrets 时，release APK 使用 debug 证书签名，可直接安装测试。

### 固定签名（已配置）

本仓库已配置固定的正式签名：签名 keystore 以 base64 编码存放在 `KEYSTORE_BASE64`，密码与别名以明文存放在 `KEYSTORE_PASSWORD`、`KEY_PASSWORD`、`KEY_ALIAS`（仓库 `Settings -> Secrets and variables -> Actions`）。CI 构建时自动解码并使用该签名，产出可直接上架的正式签名 APK。

| Secret | 内容 |
|--------|------|
| `KEYSTORE_BASE64` | 签名 keystore 文件的 base64 编码 |
| `KEYSTORE_PASSWORD` | keystore 密码 |
| `KEY_ALIAS` | 密钥别名 |
| `KEY_PASSWORD` | 密钥密码 |

签名参数：

- 别名：`oabuod`
- 证书：`CN=OABUOD, OU=R23 Studio, O=R23 Studio, L=Beijing, ST=Beijing, C=CN`
- 算法：RSA 2048，有效期 10000 天

**重要**：请务必在本地妥善备份签名 keystore（`keystore/oabuod-release.jks`）与密码。丢失签名密钥将无法对已发布应用发布更新（Android 要求同一应用的升级包必须使用相同签名）。

推送任意分支或打 tag 后，CI 都会使用该固定签名编译 APK：

```bash
git tag v1.0.0
git push origin v1.0.0
```

## 说明

- 本应用仅提供网页浏览壳层，不调用任何第三方 API Key，不采集任何用户数据
- 应用内所有请求均由豆包官方网页发起并送达豆包官方服务器，登录凭证仅保存在应用本地的 WebView 数据中
