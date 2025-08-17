# LAN Music Sync

一款现代设计的安卓局域网音乐同步播放软件，支持将一台安卓设备作为主设备，其他安卓设备通过局域网连接主设备同步播放音乐。

## 功能特性

- 🎵 **多设备音乐同步播放** - 支持主从设备架构，实现多台设备同步播放音乐
- 📡 **Wi-Fi Direct连接** - 使用Wi-Fi Direct技术建立设备间直接连接
- 🎨 **现代化UI设计** - 采用Material Design 3设计规范
- ⚡ **低延迟音频传输** - 优化的音频同步算法，确保播放同步
- 🔧 **自动化构建** - 使用GitHub Actions自动构建APK

## 技术架构

### 核心技术栈
- **开发语言**: Kotlin
- **UI框架**: Android Jetpack + Material Design 3
- **媒体播放**: ExoPlayer
- **网络通信**: Wi-Fi Direct + Socket
- **架构模式**: MVVM + Android Architecture Components

### 主要依赖
- ExoPlayer - 媒体播放
- OkHttp/Retrofit - 网络请求
- Gson - JSON序列化
- Glide - 图片加载
- Room - 本地数据存储
- Kotlin Coroutines - 异步编程

## 项目结构

```
LanMusicSync/
├── app/
│   ├── src/main/
│   │   ├── java/com/lanmusicsync/
│   │   │   ├── ui/                 # UI层
│   │   │   ├── service/            # 服务层
│   │   │   ├── network/            # 网络层
│   │   │   ├── data/               # 数据层
│   │   │   ├── model/              # 数据模型
│   │   │   ├── utils/              # 工具类
│   │   │   └── receiver/           # 广播接收器
│   │   ├── res/                    # 资源文件
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
├── gradle.properties
└── README.md
```

## 开发环境要求

- Android Studio Arctic Fox 或更高版本
- Android SDK API 24 (Android 7.0) 或更高版本
- Kotlin 1.9.10
- Gradle 8.0

## 构建说明

### 本地构建
```bash
./gradlew assembleDebug
```

### GitHub Actions自动构建
项目配置了GitHub Actions工作流，每次推送到main分支时会自动构建APK。

## 使用说明

1. **创建主设备**: 在一台设备上选择"创建主设备"，开始等待其他设备连接
2. **连接从设备**: 在其他设备上选择"加入主设备"，搜索并连接到主设备
3. **开始播放**: 在主设备上选择音乐并开始播放，所有连接的设备将同步播放

## 权限说明

应用需要以下权限：
- 网络访问权限 - 用于设备间通信
- Wi-Fi状态权限 - 用于Wi-Fi Direct连接
- 位置权限 - Wi-Fi Direct发现设备所需
- 存储权限 - 读取本地音乐文件
- 前台服务权限 - 后台音乐播放

## 贡献指南

欢迎提交Issue和Pull Request来改进项目。

## 许可证

本项目采用MIT许可证。详见LICENSE文件。

