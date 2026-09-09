# LeetCode hot 100 · 刷题助手

一款**纯 Java Swing** 开发的力扣热题 100 刷题桌面应用：内置 100 道题的完整官方题面、每种题三种参考解法（Java + Python 双语言）、**可编译运行的答题区**、**训练骨架模板**、**按题笔记**与刷题进度管理。所有数据本地存储，开箱即用。


## 功能特性

- **题目浏览**：100 道热题按官方 17 个分类组织，支持关键字搜索（序号/题号/标题/标签）、难度与分类筛选、分类彩点标识
- **完整官方题面**：每题含完整题面描述、示例（输入/输出/解释）与数据范围提示
- **三种参考解法**：每题提供 3 种不同思路的解法（含解题思路、复杂度分析、Java + Python 双语言实现）
- **内置编译器**：答题区代码可直接编译运行（Java 调用 JDK 内置 JavaCompiler，Python 调用本机解释器）
- **训练骨架模板**：每题模板自动生成本题的类与方法签名骨架（方法体为 TODO），像力扣一样刷题
- **编辑体验**：载入题解自动合并（保留 main 函数），补全 import；`sout`/`psvm`/`fori`/`100.sout` 等快速输入；
- **按题笔记**：右侧可折叠笔记栏，按题目自动保存
- **刷题进度**：完成打卡、收藏标记，本地持久化，重装不丢失

## 环境要求

| 组件 | 要求 | 用途 |
|------|------|------|
| JDK | 21+（推荐 25/26） | 运行应用、内置 Java 编译器 |
| Python 3 | 3.8+（可选） | 运行 Python 代码 |
| WiX Toolset | 3.x（仅打包 exe 时需要） | jpackage 生成 exe |

## 快速开始

### 方式一：运行源码

```bat
git clone https://github.com/ChencLi819/leetcodehot100utils.git
cd leetcodehot100utils
run.bat
```

### 方式二：打包 exe（推荐）

```bat
build_exe.bat
```

打包完成后，可执行文件位于 `dist\Hot100\Hot100.exe`，整个 `dist\Hot100` 目录绿色免安装，可直接拷贝给他人使用（自带精简 JRE，无需对方安装 Java）。

## 快捷键

| 快捷键 | 作用 |
|--------|------|
| `Ctrl+Enter` | 运行答题区代码 |
| `Enter` | 展开快速输入模板 / 换行（自动缩进） |
| `Tab` / `Shift+Tab` | 缩进 / 反缩进（选中多行整体生效） |
| `Ctrl+Z` / `Ctrl+Y` | 撤销 / 重做 |
| `Ctrl+A` / `Ctrl+C` / `Ctrl+X` / `Ctrl+V` | 全选 / 复制 / 剪切 / 粘贴 |
| `Ctrl+滚轮` | 缩放内容区字号 |

**快速输入模板**（输入缩写后按 `Enter` 展开）：

| 缩写 | 展开为 | 缩写 | 展开为 |
|------|--------|------|--------|
| `sout` | `System.out.println()` | `psvm` / `main` | main 方法 |
| `souf` | `System.out.printf()` | `serr` | `System.err.println()` |
| `fori` | 计数 for 循环 | `for` / `iter` | 增强 for |
| `ifn` / `inn` | 判空 if | `tryc` | try-catch |
| `psf` / `psfi` / `psfs` | 常量声明 | `thr` | throw 语句 |
| `alist` / `hmap` | 集合初始化 | | |

## 使用流程

1. 左侧列表选题（支持搜索与难度/分类筛选）
2. 「题目描述」阅读题面 → 「参考题解」查看三种解法与思路
3. 「恢复模板」生成本题训练骨架（含方法签名与示例调用）
4. 在「我的作答」实现方法，点击「运行」验证
5. 卡壳时「载入题解代码」对照参考实现（自动合并，保留你的 main）
6. 完成后点「标记完成」打卡，可在右侧笔记栏记录总结

## 目录结构

```
├── src/hot100/          # 应用源码（Swing 界面、数据加载、内置编译器、用户存储）
├── test/                # 回归测试（合并 / import 补全 / 骨架生成 / 编译器 E2E）
├── data/                # 题库（17 个分类 100 题 + samples_*.txt 每题示例）
├── lib/                 # FlatLaf 3.4.1
├── tools/               # 图标生成脚本
├── docs/                # 使用说明
├── run.bat / run.sh     # 开发运行（Windows / Linux·macOS）
├── build_exe.bat        # 一键打包 exe
├── test.bat             # 一键回归测试
└── icon.ico / icon.png  # 应用图标
```

## 题库数据格式

题库为自定义纯文本格式（UTF-8），每分类一个文件，易于扩充：

```
@@PROBLEM
@@NO: 1            # 题单序号
@@ID: 1            # 力扣题号
@@TITLE: 两数之和
@@DIFF: 简单
@@CATEGORY: 哈希
@@TAGS: 数组;哈希表
@@URL: https://leetcode.cn/problems/two-sum/
@@DESC             # 完整官方题面（含示例与提示）
@@SOLUTION: 方法一：哈希表（推荐）
@@IDEA             # 解题思路与复杂度
@@JAVA             # Java 实现
@@PY               # Python 实现
```

`data/samples_*.txt` 中的 `@@SAMPLE-JAVA: no` / `@@SAMPLE-PY: no` 为每题模板的定制示例。

## 测试

```bat
test.bat
```

覆盖 5 套回归：题解合并（保留 main）、import 补全、骨架生成（100 题全覆盖）、编译器端到端（Java/Python/错误行号）。

## 技术栈

- Java Swing + FlatLaf 3.4.1（无 HTML/CSS/JS 渲染）
- javax.tools.JavaCompiler（内置编译）
- jpackage（app-image 模式打包，--add-modules jdk.compiler,jdk.zipfs）

## License

[MIT](LICENSE)
