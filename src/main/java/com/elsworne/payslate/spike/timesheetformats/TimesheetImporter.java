package com.elsworne.payslate.spike.timesheetformats;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Small prototype for the product importer contract. */
public final class TimesheetImporter {
  public record Shift(
      String employee, LocalDateTime start, LocalDateTime end, boolean publicHoliday) {}

  private static final DateTimeFormatter AU_DATE =
      new DateTimeFormatterBuilder()
          .appendPattern("d/M/uuuu")
          .toFormatter(Locale.ROOT)
          .withResolverStyle(ResolverStyle.STRICT);
  private static final DateTimeFormatter TWELVE_HOUR =
      DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

  public List<Shift> read(String input) {
    if (input == null) throw new IllegalArgumentException("CSV is null");
    String content = input.startsWith("\uFEFF") ? input.substring(1) : input;
    String[] lines = content.split("\r?\n", -1);
    for (int line = 0; line < lines.length; line++) {
      if (lines[line].contains("\r"))
        throw new IllegalArgumentException("Row " + (line + 1) + ": bare CR");
    }
    if (lines.length == 0 || lines[0].isBlank())
      throw new IllegalArgumentException("Missing header");
    char delimiter = lines[0].contains("\t") ? '\t' : lines[0].contains(";") ? ';' : ',';
    List<String> header = fields(lines[0], delimiter);
    int name = column(header, "employee", "employee name", "staff");
    int date = column(header, "date", "shift date");
    int start = column(header, "start", "start time");
    int end = column(header, "end", "end time");
    int holiday = optionalColumn(header, "public holiday", "public_holiday", "holiday");
    var shifts = new ArrayList<Shift>();
    for (int line = 1; line < lines.length; line++) {
      if (lines[line].isBlank()) continue;
      List<String> cells = fields(lines[line], delimiter);
      if (cells.size() != header.size())
        throw new IllegalArgumentException("Row " + (line + 1) + ": column count");
      String employee = cells.get(name).trim();
      if (employee.isEmpty())
        throw new IllegalArgumentException("Row " + (line + 1) + ": empty employee");
      try {
        String dateText = cells.get(date).trim();
        LocalDate day =
            dateText.contains("-") ? LocalDate.parse(dateText) : LocalDate.parse(dateText, AU_DATE);
        LocalDateTime from = day.atTime(time(cells.get(start).trim()));
        LocalDateTime to = day.atTime(time(cells.get(end).trim()));
        if (to.isBefore(from)) to = to.plusDays(1);
        if (!to.isAfter(from)) throw new IllegalArgumentException("duration");
        String holidayText = holiday < 0 ? "" : cells.get(holiday).trim();
        boolean publicHoliday;
        if (holidayText.isEmpty()
            || holidayText.equalsIgnoreCase("false")
            || holidayText.equalsIgnoreCase("no")
            || holidayText.equals("0")) publicHoliday = false;
        else if (holidayText.equalsIgnoreCase("true")
            || holidayText.equalsIgnoreCase("yes")
            || holidayText.equals("1")) publicHoliday = true;
        else throw new IllegalArgumentException("invalid public holiday flag");
        shifts.add(new Shift(employee, from, to, publicHoliday));
      } catch (RuntimeException error) {
        throw new IllegalArgumentException(
            "Row " + (line + 1) + ": invalid date, time or duration", error);
      }
    }
    return List.copyOf(shifts);
  }

  private static LocalTime time(String text) {
    return text.toUpperCase(Locale.ROOT).contains("M")
        ? LocalTime.parse(text.toUpperCase(Locale.ROOT), TWELVE_HOUR)
        : LocalTime.parse(text);
  }

  private static int column(List<String> header, String... aliases) {
    int found = optionalColumn(header, aliases);
    if (found >= 0) return found;
    throw new IllegalArgumentException("Unknown or missing header: " + String.join("/", aliases));
  }

  private static int optionalColumn(List<String> header, String... aliases) {
    for (int i = 0; i < header.size(); i++) {
      for (String alias : aliases) {
        if (header.get(i).trim().equalsIgnoreCase(alias)) return i;
      }
    }
    return -1;
  }

  private static List<String> fields(String row, char delimiter) {
    var result = new ArrayList<String>();
    var current = new StringBuilder();
    boolean quoted = false;
    for (int i = 0; i < row.length(); i++) {
      char c = row.charAt(i);
      if (c == '"') {
        if (quoted && i + 1 < row.length() && row.charAt(i + 1) == '"') {
          current.append('"');
          i++;
        } else quoted = !quoted;
      } else if (c == delimiter && !quoted) {
        result.add(current.toString());
        current.setLength(0);
      } else current.append(c);
    }
    if (quoted) throw new IllegalArgumentException("Unclosed quoted field");
    result.add(current.toString());
    return result;
  }
}
