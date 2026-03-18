English | [简体中文](README.zh-CN.md)

# Timesheet format prototype

Payslate Online will import timesheets exported from different rostering apps. This standalone prototype maps which export formats the product importer should accept and which it must reject. The partitions are listed in [the design note](docs/partitions.md), and each has a test. What the prototype showed is in [FINDINGS.md](FINDINGS.md).

Run `./mvnw -B -ntp verify` with Java 21; all partition tests pass. The prototype uses synthetic timesheet data only.

## About this repository

Elsworne, Payslate and the team are fictional.

The code and documents in this repository were written with the help of AI tools; every recorded output comes from actually running them.
