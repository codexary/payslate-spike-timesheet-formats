[English](README.md) | 简体中文

# 工时表格式原型

Payslate Online 要导入各种排班软件导出的工时表。这个独立的原型用来弄清产品导入器应该接受哪些导出格式、必须拒绝哪些。各个类别列在[设计笔记](docs/partitions.zh-CN.md)里，每一类都有对应的测试。

用 Java 21 运行 `./mvnw -B -ntp verify`，所有分区测试都应通过。原型只使用合成的工时数据。

## 关于本仓库

Elsworne、Payslate 和团队成员均为虚构。

本仓库的代码与文档借助 AI 工具编写；记录的输出都来自实际运行。
