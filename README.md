# 财务记账工具 (Finance Accountant)

跨平台财务记账工具，支持 Windows 和 Android 双平台。

## 功能介绍

- **记账功能**：记录收入和支出，支持多种分类
  - 收入分类：工资、奖金、投资、兼职、其他收入
  - 支出分类：餐饮、交通、购物、居住、医疗、娱乐、教育、其他支出
- **账单管理**：查看、筛选、删除账单记录
- **月度统计**：查看每月收支概况
- **统计图表**：
  - 饼图：展示支出分类占比
  - 折线图：展示每日收支趋势
- **数据导出**：支持 CSV 和 Excel 格式导出
- **暗黑主题**：保护眼睛的深色界面设计

## 技术栈

| 平台 | 技术 |
|------|------|
| Windows | C# / WPF / .NET 8 / SQLite / LiveCharts |
| Android | Kotlin / Jetpack Compose / Room / Material 3 |

## 项目结构

```
finance-accountant/
├── windows/              # Windows WPF 应用
│   ├── Models/           # 数据模型
│   ├── ViewModels/       # 视图模型 (MVVM)
│   ├── Views/            # 视图界面
│   ├── Services/         # 业务服务
│   └── Data/             # 数据库上下文
├── android/              # Android 应用
│   └── app/src/main/
│       ├── java/com/finance/accountant/
│       │   ├── domain/model/    # 数据模型
│       │   ├── data/           # 数据库层
│       │   └── ui/             # UI 层 (Compose)
│       └── res/                # 资源文件
└── .github/workflows/    # GitHub Actions 自动化构建
```

## 下载使用

### Windows 版本

从 [Release](https://github.com/BTCUP99/finance-accountant/releases) 页面下载 `FinanceAccountant-v1.0.0-win-x64.zip`，解压后双击 `FinanceAccountant.exe` 即可运行。

### Android 版本

需要本地构建 APK：
```bash
cd android
./gradlew assembleDebug
# APK 输出位置: app/build/outputs/apk/debug/app-debug.apk
```

## 本地开发

### Windows 开发

```bash
cd windows
dotnet restore
dotnet run
```

### Android 开发

```bash
cd android
./gradlew assembleDebug
```

## 自动构建

- **Windows**: 推送代码到 main 分支或发布 tag 时自动构建
- **Android**: 推送代码到 main 分支时自动构建

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0.0 | 2026-04-22 | 首个版本发布 |

## 许可证

MIT License