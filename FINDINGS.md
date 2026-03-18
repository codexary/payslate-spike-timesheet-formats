English | [简体中文](FINDINGS.zh-CN.md)

# Timesheet format prototype findings

The prototype explores format choices for the product importer using synthetic text. `TimesheetImporterTest` has **22 cases: 12 supported and 10 rejected representatives**. Each supported case asserts one parsed shift, its employee name, end timestamp and `publicHoliday` value. Each rejected case asserts `IllegalArgumentException` and a diagnostic substring. These representatives establish specific behaviours, not complete coverage of CSV or vendor export formats.

## Supported representatives

| Test partition | What the case shows |
| --- | --- |
| `comma_iso_24h_lf` | Comma-delimited input, ISO date, 24-hour times and LF parse as one shift. |
| `semicolon_au_date` | Semicolon-delimited input with `16/2/2026` parses. |
| `tab_alias_headers` | Tab-delimited input accepts `staff`, `shift date`, `start time` and `end time`. |
| `twelve_hour_time` | `9:00 AM` to `1:00 PM` ends at 13:00. |
| `utf8_bom` | One leading UTF-8 BOM is removed before header handling. |
| `quoted_comma` | A comma inside a quoted employee name remains in the name. |
| `empty_rows` | Blank data rows are ignored. |
| `crlf` | CRLF input parses. |
| `cross_midnight` | Friday 23:00 to 01:00 ends on Saturday. |
| `nearly_24_hours` | 09:00 to 08:59 becomes a next-day end, lasting 23 h 59 m. |
| `holiday_true` | A `public holiday` column containing `true` produces `publicHoliday == true`. |
| `holiday_false` | The same column containing `false` produces `publicHoliday == false`. |

The cases without a holiday column assert false. The public-holiday column is an explicit Boolean partition, not merely an optional extra field: silently losing a true value would fail the supported-case assertion.

## Rejected representatives

| Test partition | What the case shows |
| --- | --- |
| `unknown_header` | `worker` instead of a recognised employee header is rejected with a header diagnostic. |
| `malformed_quote` | An unclosed quoted employee field is rejected. |
| `quoted_newline` | A newline inside a quoted employee field is rejected with `Unclosed quoted field`. |
| `bare_cr` | CR-only record separators are rejected with `Row 1: bare CR`. |
| `bare_cr_in_data` | A bare CR in a data row is rejected with `Row 2: bare CR`. |
| `invalid_date` | `31/2/2026` is rejected with a row diagnostic. |
| `empty_employee` | An empty employee field is rejected. |
| `mixed_delimiter` | A comma header followed by a semicolon data row fails the column-count check. |
| `zero_duration` | Equal start and end times are rejected. |
| `invalid_holiday` | `maybe` in the holiday column is rejected with a row diagnostic. |

## Implementation scope

The parser chooses tab if the header contains a tab, otherwise semicolon if it contains a semicolon, otherwise comma. It looks up these header aliases case-insensitively: `employee` / `employee name` / `staff`, `date` / `shift date`, `start` / `start time`, `end` / `end time`. Holiday aliases are `public holiday`, `public_holiday` and `holiday`. Its Boolean parser accepts `true` / `yes` / `1` and `false` / `no` / `0` / blank without case sensitivity; absence means false. The suite directly exercises `true`, `false`, absence and `maybe`, not every alias and spelling.

The implementation uses strict Australian calendar-date parsing alongside ISO date parsing. It parses ISO local times or AM/PM times, and handles doubled quotes inside a quoted field. The listed tests do not separately exercise doubled quotes or every invalid date/time spelling, and do not prove a strict `HH:mm`-only grammar.

Each row has one date and two times. An earlier end advances to the next day; unequal times therefore yield a shift shorter than 24 hours. Equal times yield zero duration and are rejected. **Shifts of 24 hours or more cannot be expressed**, so a longer-than-24-hour rejection case cannot be constructed in this format.

The parser splits physical lines before parsing fields. Quoted newlines are therefore explicitly rejected by the tested implementation, rather than an untested supported feature. Whether to accept them in the product remains a product-importer decision.

Required header lookup does not reject all unknown extra columns. There is also no general rejection of an embedded BOM: for example, a BOM inside an employee name can remain in that name. The format partition note's proposed rejection rules must not be mistaken for fully enforced parser validation. The mixed-delimiter test proves the listed column-count failure, not detection of every delimiter inconsistency.

## Implications for the product importer

The prototype parser is not the product parser. Keep the accepted and rejected representatives as contract examples, then implement the product boundary with a maintained CSV parser and streaming input. Add checks for the full header, date/time, BOM, quoting and Boolean contract, and decide quoted-newline support explicitly. Real vendor files and product integration still need their own validation.

The prototype's 22 tests do not prove that importer or any real vendor export works.
