# 围棋打谱软件

一个基于 Android 的围棋打谱应用程序。

## AI 贡献说明

本项目由 AI 助手（Trae AI）协助开发。AI 参与了项目的架构设计、代码实现和文档编写等工作。

## 许可证

本项目采用 [WTFPL](http://www.wtfpl.net/) 许可证 - 随便你怎么做公共许可证

```plaintext
            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
                    Version 2, December 2004

 Copyright (C) 2024 Your Name <your@email.com>

 Everyone is permitted to copy and distribute verbatim or modified
 copies of this license document, and changing it is allowed as long
 as the name is changed.

            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

  0. You just DO WHAT THE FUCK YOU WANT TO.
```

## 开发环境要求

- Android SDK
- Java Development Kit (JDK)
- Android Studio (可选)

## 构建和运行

项目提供了便捷的构建脚本 `build.sh`，支持以下命令：

### 查看所有可用设备
```bash
# 列出已连接的设备
./build.sh devices

# 安装到设备
./build.sh install

# 构建并运行
./build.sh run

# 查看日志
./build.sh logcat
```


emulator -avd Pixel_9_Pro_API_33

## SGF 处理逻辑

### 支持的功能

- **标准SGF格式**：支持 FF[4] 标准格式
- **游戏信息**：支持 PB（黑方）、PW（白方）、RE（结果）、DT（日期）等属性
- **让子支持**：支持 HA（让子数）、AB（黑棋让子）、AW（白棋让子）属性
- **分支处理**：
  - 第一手分支：通过 startVariations 管理
  - 后续分支：通过 Move.variations 管理
  - 分支保存和加载：完整支持
- **虚手支持**：
  - 虚手落子：支持坐标 (-1, -1)
  - 虚手保存：保存为 "tt" 格式
  - 虚手加载：从 "tt" 格式解析

### 核心流程

#### 保存流程
```
GoBoard → SGFConverter.boardToSgfTree() → SGFParser.save() → SGF字符串
```
- **boardToSgfTree**：将 moveHistory 作为主序列，startVariations 作为根节点分支
- **save**：保存根节点、主序列和所有分支

#### 加载流程
```
SGF字符串 → SGFParser.parse() → SGFConverter.sgfTreeToBoard() → GoBoard
```
- **parse**：解析SGF字符串为 SGFTree
- **sgfTreeToBoard**：解析根节点信息，主序列为 moveHistory，根节点分支为 startVariations

### 分支管理

1. **第一手分支**：
   - 当回到起始状态再落子时，会创建新的第一手分支
   - 分支保存在 startVariations 中
   - 加载时从根节点分支恢复

2. **后续分支**：
   - 当在非最后一步落子时，会创建后续分支
   - 分支保存在对应 Move 的 variations 中
   - 加载时从节点分支恢复

### 虚手处理

- **落子**：支持坐标 (-1, -1) 表示虚手
- **保存**：虚手保存为 SGF 的 "tt" 格式
- **加载**：SGF 的 "tt" 格式解析为虚手

## SGF 规范

https://www.red-bean.com/sgf/properties.html#CA