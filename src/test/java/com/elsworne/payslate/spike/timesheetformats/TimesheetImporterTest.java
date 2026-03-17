package com.elsworne.payslate.spike.timesheetformats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TimesheetImporterTest {
  static Stream<Arguments> supportedPartitions() {
    return Stream.of(
        Arguments.of(
            "comma_iso_24h_lf",
            "employee,date,start,end\nAva,2026-02-16,09:00,10:00\n",
            "Ava",
            "2026-02-16T10:00"),
        Arguments.of(
            "semicolon_au_date",
            "employee;date;start;end\nAva;16/2/2026;09:00;10:00",
            "Ava",
            "2026-02-16T10:00"),
        Arguments.of(
            "tab_alias_headers",
            "staff\tshift date\tstart time\tend time\nAva\t16/2/2026\t09:00\t10:00",
            "Ava",
            "2026-02-16T10:00"),
        Arguments.of(
            "twelve_hour_time",
            "employee,date,start,end\nAva,2026-02-16,9:00 AM,1:00 PM",
            "Ava",
            "2026-02-16T13:00"),
        Arguments.of(
            "utf8_bom",
            "\uFEFFemployee,date,start,end\nAva,2026-02-16,09:00,10:00",
            "Ava",
            "2026-02-16T10:00"),
        Arguments.of(
            "quoted_comma",
            "employee,date,start,end\n\"Ava, Test\",2026-02-16,09:00,10:00",
            "Ava, Test",
            "2026-02-16T10:00"),
        Arguments.of(
            "empty_rows",
            "employee,date,start,end\n\nAva,2026-02-16,09:00,10:00\n\n",
            "Ava",
            "2026-02-16T10:00"),
        Arguments.of(
            "crlf",
            "employee,date,start,end\r\nAva,2026-02-16,09:00,10:00\r\n",
            "Ava",
            "2026-02-16T10:00"),
        Arguments.of(
            "cross_midnight",
            "employee,date,start,end\nAva,2026-02-13,23:00,01:00",
            "Ava",
            "2026-02-14T01:00"),
        Arguments.of(
            "holiday_true",
            "employee,date,start,end,public holiday\nAva,2026-02-16,09:00,10:00,true",
            "Ava",
            "2026-02-16T10:00"),
        Arguments.of(
            "holiday_false",
            "employee,date,start,end,public holiday\nAva,2026-02-16,09:00,10:00,false",
            "Ava",
            "2026-02-16T10:00"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("supportedPartitions")
  void supports(String partition, String csv, String name, String end) {
    var shifts = new TimesheetImporter().read(csv);
    assertThat(shifts).as(partition).hasSize(1);
    assertThat(shifts.getFirst().employee()).isEqualTo(name);
    assertThat(shifts.getFirst().end().toString()).isEqualTo(end);
    assertThat(shifts.getFirst().publicHoliday()).isEqualTo(partition.equals("holiday_true"));
  }

  static Stream<Arguments> rejectedPartitions() {
    return Stream.of(
        Arguments.of(
            "unknown_header", "worker,date,start,end\nAva,2026-02-16,09:00,10:00", "header"),
        Arguments.of(
            "malformed_quote", "employee,date,start,end\n\"Ava,2026-02-16,09:00,10:00", "quoted"),
        Arguments.of("invalid_date", "employee,date,start,end\nAva,31/2/2026,09:00,10:00", "Row 2"),
        Arguments.of(
            "empty_employee", "employee,date,start,end\n,2026-02-16,09:00,10:00", "empty employee"),
        Arguments.of(
            "mixed_delimiter",
            "employee,date,start,end\nAva;2026-02-16;09:00;10:00",
            "column count"),
        Arguments.of(
            "zero_duration", "employee,date,start,end\nAva,2026-02-16,09:00,09:00", "Row 2"),
        Arguments.of(
            "invalid_holiday",
            "employee,date,start,end,public holiday\nAva,2026-02-16,09:00,10:00,maybe",
            "Row 2"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rejectedPartitions")
  void rejects(String partition, String csv, String message) {
    assertThatThrownBy(() -> new TimesheetImporter().read(csv))
        .as(partition)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(message);
  }
}
