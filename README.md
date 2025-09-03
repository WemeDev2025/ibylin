# Ibylin Android 项目

这是一个使用最新Android开发技术构建的现代化Android应用程序。

## 🚀 技术栈

### 核心框架
- **Kotlin** - 主要开发语言
- **Jetpack Compose** - 现代UI工具包
- **AndroidX** - 最新的Android支持库

### 架构组件
- **MVVM架构模式** - Model-View-ViewModel
- **Repository模式** - 数据访问抽象层
- **Use Case模式** - 业务逻辑封装

### 依赖注入
- **Hilt** - Android官方推荐的依赖注入框架

### 数据层
- **Room** - 本地数据库
- **Retrofit** - 网络请求
- **Coroutines & Flow** - 异步编程

### UI组件
- **Material Design 3** - 最新的设计系统
- **Navigation Component** - 页面导航
- **ViewBinding & DataBinding** - 视图绑定

## 📱 最低要求

- **Android API Level**: 24 (Android 7.0)
- **Kotlin版本**: 1.9.22
- **Gradle版本**: 8.2.2
- **Java版本**: 17

## 🏗️ 项目结构

```
app/
├── src/main/
│   ├── java/com/ibylin/app/
│   │   ├── data/           # 数据层
│   │   │   ├── local/      # 本地数据源
│   │   │   ├── model/      # 数据模型
│   │   │   └── repository/ # 数据仓库
│   │   ├── di/             # 依赖注入
│   │   ├── ui/             # 用户界面
│   │   │   ├── theme/      # 主题样式
│   │   │   └── viewmodel/  # 视图模型
│   │   └── MainActivity.kt # 主活动
│   ├── res/                # 资源文件
│   └── AndroidManifest.xml # 应用清单
├── build.gradle            # 应用级构建配置
└── proguard-rules.pro     # 代码混淆规则
```

## 🚀 快速开始

### 1. 克隆项目
```bash
git clone <repository-url>
cd ibylin
```

### 2. 同步项目
在Android Studio中打开项目，等待Gradle同步完成。

### 3. 运行应用
- 连接Android设备或启动模拟器
- 点击运行按钮或使用快捷键 `Shift + F10`

### 4. 构建调试版本
```bash
./gradlew assembleDebug
```

### 5. 安装到设备
```bash
./gradlew installDebug
```

## 🔧 构建配置

### Gradle配置
- 使用最新的Android Gradle Plugin 8.2.2
- 支持增量编译和构建缓存
- 启用并行构建和配置缓存

### 编译配置
- 目标SDK: 34 (Android 14)
- 最低SDK: 24 (Android 7.0)
- Java版本: 17
- Kotlin版本: 1.9.22

## 📚 主要特性

- ✅ **现代化UI**: 使用Jetpack Compose构建响应式界面
- ✅ **MVVM架构**: 清晰的代码结构和职责分离
- ✅ **依赖注入**: 使用Hilt管理依赖关系
- ✅ **本地存储**: Room数据库支持
- ✅ **网络请求**: Retrofit + OkHttp
- ✅ **异步编程**: Coroutines + Flow
- ✅ **导航**: Navigation Component
- ✅ **主题支持**: Material Design 3 + 深色模式

## 🧪 测试

项目包含完整的测试配置：
- **单元测试**: JUnit 4
- **UI测试**: Espresso
- **Compose测试**: Compose测试工具

运行测试：
```bash
./gradlew test           # 单元测试
./gradlew connectedTest  # 设备测试
```

## 📦 依赖管理

所有依赖都使用最新的稳定版本，包括：
- AndroidX Core KTX 1.12.0
- Compose BOM 2024.02.00
- Navigation 2.7.6
- Room 2.6.1
- Retrofit 2.9.0
- Hilt 2.48

## 🔍 代码质量

- 使用Kotlin官方代码风格
- 启用AndroidX
- 配置ProGuard规则
- 支持代码混淆

## 📱 应用权限

- `INTERNET` - 网络访问
- `ACCESS_NETWORK_STATE` - 网络状态检查

## 🤝 贡献指南

1. Fork项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开Pull Request

## 📄 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 联系方式

如有问题或建议，请通过以下方式联系：
- 创建Issue
- 发送邮件
- 提交Pull Request

---

**注意**: 这是一个示例项目，展示了现代Android开发的最佳实践。在实际使用中，请根据具体需求进行调整。
