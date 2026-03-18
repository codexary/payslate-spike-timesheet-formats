[English](FINDINGS.md) | 简体中文

# 工时表格式原型结论

原型使用合成文本，为产品导入器探索格式选择。`TimesheetImporterTest` 有 **22 个案例：12 个支持类代表、10 个拒绝类代表**。每个支持案例断言恰好解析出一个班次，并检查员工姓名、结束时间戳及 `publicHoliday` 值。每个拒绝案例断言 `IllegalArgumentException` 和错误信息中的指定片段。这些代表确定的是具体行为，并未全面覆盖 CSV 或厂商导出格式。

## 支持的代表案例

| 测试分区 | 案例说明 |
| --- | --- |
| `comma_iso_24h_lf` | 逗号分隔、ISO 日期、24 小时时间和 LF 换行可解析为一个班次。 |
| `semicolon_au_date` | 分号分隔、日期为 `16/2/2026` 的输入可解析。 |
| `tab_alias_headers` | 制表符分隔时接受 `staff`、`shift date`、`start time` 和 `end time` 表头。 |
| `twelve_hour_time` | `9:00 AM` 至 `1:00 PM` 的结束时间为 13:00。 |
| `utf8_bom` | 表头处理前移除文件开头的一个 UTF-8 BOM。 |
| `quoted_comma` | 带引号的员工姓名中的逗号保留在姓名内。 |
| `empty_rows` | 忽略空数据行。 |
| `crlf` | CRLF 换行的输入可解析。 |
| `cross_midnight` | 周五 23:00 至 01:00 的班次在周六结束。 |
| `nearly_24_hours` | 09:00 至 08:59 的结束时间归入次日，时长为 23 小时 59 分。 |
| `holiday_true` | `public holiday` 列为 `true` 时，得到 `publicHoliday == true`。 |
| `holiday_false` | 同一列为 `false` 时，得到 `publicHoliday == false`。 |

没有公共假日列的案例断言 false。公共假日列是独立的 Boolean 分区，不只是一个可选附加字段：如果解析时悄悄丢失 true，支持案例中的断言就会失败。

## 拒绝的代表案例

| 测试分区 | 案例说明 |
| --- | --- |
| `unknown_header` | 使用 `worker` 代替认可的员工表头时拒绝，并报告表头错误。 |
| `malformed_quote` | 员工字段的引号未闭合时拒绝。 |
| `quoted_newline` | 员工字段引号内包含换行时，以 `Unclosed quoted field` 拒绝。 |
| `bare_cr` | 仅用 CR 分隔记录时，以 `Row 1: bare CR` 拒绝。 |
| `bare_cr_in_data` | 数据行中的单独 CR 以 `Row 2: bare CR` 拒绝。 |
| `invalid_date` | 拒绝 `31/2/2026`，错误信息包含行号。 |
| `empty_employee` | 拒绝空员工字段。 |
| `mixed_delimiter` | 逗号表头之后使用分号数据行，会因列数检查失败而拒绝。 |
| `zero_duration` | 拒绝相同的起止时间。 |
| `invalid_holiday` | 公共假日列为 `maybe` 时拒绝，错误信息包含行号。 |

## 实现范围

表头包含制表符时选用制表符，否则有分号时选分号，其余情况选逗号。解析器不区分大小写地查找以下表头别名：`employee` / `employee name` / `staff`、`date` / `shift date`、`start` / `start time`、`end` / `end time`。公共假日列的别名是 `public holiday`、`public_holiday` 和 `holiday`。Boolean 解析接受不区分大小写的 `true` / `yes` / `1` 与 `false` / `no` / `0` / 空值；缺列视为 false。套件直接检查了 `true`、`false`、缺列及 `maybe`，没有逐一检查所有别名和写法。

实现对澳大利亚日期执行严格的日历校验，也接受 ISO 日期。时间通过 ISO 本地时间或 AM/PM 格式解析，引号内的双引号转义也有实现。所列测试没有单独检查双引号转义或每一种非法日期时间写法，也不能证明时间语法严格限制为 `HH:mm`。

每行只有一个日期和两个时间。结束早于开始时，结束日期前进一天，因此起止不同的班次总是短于 24 小时；起止相同则为零时长并被拒绝。**这种格式无法表达 24 小时或更长的班次**，所以无法构造超过 24 小时的拒绝案例。

解析器先按物理行切分，再解析字段。因此在已测试的实现中，引号内换行是明确被拒绝的，并非尚未测试的支持功能。产品是否接受这类输入，仍须由产品导入器明确决定。

必需表头查找不会拒绝所有额外的未知列。实现也没有通用的数据内 BOM 拒绝逻辑，例如员工姓名内的 BOM 可以原样保留。因此不能把格式分区笔记中的目标拒绝规则当作已经完整实现的校验。混用分隔符的测试只证明表中所列的列数错误会被拒绝，不能证明能发现所有分隔符不一致的情况。

## 对产品导入器的意义

原型解析器不是产品解析器。应保留这些支持和拒绝的代表案例作为契约示例，再使用维护中的 CSV 解析器与流式输入实现产品边界。产品还需补齐表头、日期时间、BOM、引号及 Boolean 的完整契约校验，并明确决定引号内换行的支持方式。真实厂商文件与产品集成仍需各自验证。

原型的 22 个测试不能证明该导入器或任何真实厂商导出文件可用。
