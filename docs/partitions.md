English | [简体中文](partitions.zh-CN.md)

# Timesheet export partitions

The prototype receives one CSV file with employee, date, start and end, plus an optional public-holiday flag. Each row represents one shift. Tests use synthetic names. Base choice is comma, ISO date, 24-hour time, LF, no BOM, one row; one class changes at a time. The quoted comma and delimiter interaction is explicit because a plain `split` would fail. A midnight row checks the relation between date and times.

| Variable | Product importer supports | Rejected with error |
| --- | --- | --- |
| Delimiter | comma, semicolon, tab, chosen from header | mixed delimiter within file |
| Header names | employee/employee name/staff; date/shift date; start/start time; end/end time, case insensitive | unknown/missing header |
| Date | ISO `yyyy-MM-dd`, Australian `d/M/yyyy` with strict calendar validation | invalid date or other format |
| Time | 24-hour `HH:mm`, 12-hour `h:mm AM/PM` | malformed time |
| Public holiday | optional `public holiday`/`public_holiday`/`holiday`; true/yes/1 or false/no/0/blank, case insensitive; absent means false | any other value |
| BOM | UTF-8 BOM at beginning, or none | BOM inside data |
| Quoting | comma in quoted employee name, doubled quote | unclosed quote, newline inside quoted field |
| Rows | blank rows ignored | wrong column count, empty employee |
| Line ending | CRLF or LF | bare CR |
| Shift relation | end earlier than start means next day | equal start/end |

Each row has one date and two times. With an earlier end interpreted as the next day, unequal times always produce a shift shorter than 24 hours; equal times produce zero duration and are rejected. Shifts of 24 hours or longer cannot be expressed in this format, so longer than 24 hours is not a reachable rejection class.

The product importer should use a maintained CSV parser for full quoting and streaming; this small parser demonstrates the decisions and their tests. The prototype rejects newlines inside quoted fields with an unclosed quoted field error. Support for such files needs an explicit decision in the product importer.
