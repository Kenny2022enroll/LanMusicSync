# 贡献指南

感谢您对 LAN Music Sync 项目的关注！我们欢迎各种形式的贡献。

## 如何贡献

### 报告问题

如果您发现了 bug 或有功能建议，请：

1. 检查 [Issues](https://github.com/yourusername/LanMusicSync/issues) 确保问题尚未被报告
2. 创建新的 Issue，包含：
   - 清晰的标题和描述
   - 重现步骤（对于 bug）
   - 预期行为和实际行为
   - 设备信息（Android 版本、设备型号等）
   - 相关的日志或截图

### 提交代码

1. Fork 本仓库
2. 创建功能分支：`git checkout -b feature/your-feature-name`
3. 提交更改：`git commit -am 'Add some feature'`
4. 推送到分支：`git push origin feature/your-feature-name`
5. 创建 Pull Request

### 代码规范

- 使用 Kotlin 编写代码
- 遵循 [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
- 确保代码通过 lint 检查
- 为新功能添加适当的测试
- 更新相关文档

### 提交信息规范

使用清晰的提交信息：

```
类型(范围): 简短描述

详细描述（可选）

关闭的 Issue（可选）
```

类型包括：
- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `style`: 代码格式化
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

示例：
```
feat(network): 添加设备自动重连功能

当网络连接中断时，自动尝试重新连接到主设备。
包含指数退避算法以避免频繁重连。

Closes #123
```

## 开发环境设置

### 要求

- Android Studio Arctic Fox 或更高版本
- JDK 17
- Android SDK API 24+
- Git

### 设置步骤

1. 克隆仓库：
   ```bash
   git clone https://github.com/yourusername/LanMusicSync.git
   cd LanMusicSync
   ```

2. 在 Android Studio 中打开项目

3. 同步 Gradle 文件

4. 运行项目：
   ```bash
   ./gradlew assembleDebug
   ```

### 运行测试

```bash
# 单元测试
./gradlew test

# UI 测试
./gradlew connectedAndroidTest

# 代码质量检查
./gradlew lintDebug
```

## 发布流程

项目使用 GitHub Actions 进行自动化构建和发布：

1. **持续集成**：每次推送到 `main` 或 `develop` 分支时自动运行测试和构建
2. **代码质量**：自动运行 lint、detekt 等代码质量检查
3. **发布**：创建 tag 时自动构建并发布 APK

### 创建发布

1. 更新版本号在 `app/build.gradle` 中
2. 更新 `CHANGELOG.md`
3. 创建 tag：`git tag -a v1.0.0 -m "Release version 1.0.0"`
4. 推送 tag：`git push origin v1.0.0`

## 项目结构

```
LanMusicSync/
├── app/
│   ├── src/main/
│   │   ├── java/com/lanmusicsync/
│   │   │   ├── ui/                 # UI 层
│   │   │   ├── service/            # 服务层
│   │   │   ├── network/            # 网络层
│   │   │   ├── data/               # 数据层
│   │   │   ├── model/              # 数据模型
│   │   │   ├── utils/              # 工具类
│   │   │   └── receiver/           # 广播接收器
│   │   └── res/                    # 资源文件
│   └── build.gradle
├── .github/workflows/              # GitHub Actions
├── docs/                           # 文档
└── README.md
```

## 架构说明

项目采用 MVVM 架构模式：

- **Model**: 数据模型和业务逻辑
- **View**: UI 组件（Activity、Fragment）
- **ViewModel**: 连接 View 和 Model，管理 UI 状态

主要组件：

- **MusicSyncService**: 核心服务，处理音乐播放和设备同步
- **SocketManager**: 网络通信管理
- **DeviceDiscovery**: 设备发现和连接
- **ExoPlayer**: 音乐播放引擎

## 许可证

通过贡献代码，您同意您的贡献将在与项目相同的 MIT 许可证下授权。

## 联系方式

如有问题，请通过以下方式联系：

- 创建 Issue
- 发送邮件到 [your-email@example.com]
- 加入我们的讨论群

感谢您的贡献！

