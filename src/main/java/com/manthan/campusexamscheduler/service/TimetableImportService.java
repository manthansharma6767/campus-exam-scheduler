package com.manthan.campusexamscheduler.service;

import com.manthan.campusexamscheduler.dto.TimetableImportResponse;
import com.manthan.campusexamscheduler.entity.Exam;
import com.manthan.campusexamscheduler.entity.Subject;
import com.manthan.campusexamscheduler.repository.ExamRepository;
import com.manthan.campusexamscheduler.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TimetableImportService {

    private static final Set<String> REQUIRED_HEADERS = Set.of("subjectcode", "date", "time", "building", "room");
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("d-M-uuuu"),
            DateTimeFormatter.ofPattern("M/d/uuuu")
    );
    private static final List<DateTimeFormatter> TIME_FORMATS = List.of(
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("H:mm:ss"),
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("h:mm:ss a", Locale.ENGLISH)
    );

    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;

    @Transactional
    public TimetableImportResponse importTimetable(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a non-empty timetable file.");
        }

        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        List<String[]> rows;
        try {
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                rows = readSpreadsheet(file);
            } else if (filename.endsWith(".pdf")) {
                rows = readPdf(file);
            } else {
                throw new IllegalArgumentException("Supported files are PDF, XLS, and XLSX.");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("The timetable file could not be read. Check that it is a valid, unprotected PDF or Excel file.");
        }

        int headerRow = findHeaderRow(rows);
        if (headerRow < 0 || rows.size() <= headerRow + 1) {
            throw new IllegalArgumentException("The timetable must contain a header row and at least one exam row.");
        }
        Map<String, Integer> columns = headerColumns(rows.get(headerRow));
        List<Exam> exams = new ArrayList<>();
        Set<String> seenInFile = new HashSet<>();
        int skippedDuplicates = 0;

        for (int index = headerRow + 1; index < rows.size(); index++) {
            String[] row = rows.get(index);
            if (Arrays.stream(row).allMatch(value -> value == null || value.isBlank())) continue;
            if (isHeaderRow(row)) continue;
            int rowNumber = index + 1;
            try {
                String subjectCode = cell(row, columns, "subjectCode").trim().toUpperCase(Locale.ROOT);
                String rawDate = cell(row, columns, "date").trim();
                String rawTime = cell(row, columns, "time").trim();
                String building = cell(row, columns, "building").trim();
                String room = cell(row, columns, "room").trim();
                if (subjectCode.isBlank() || building.isBlank() || room.isBlank()) {
                    throw new IllegalArgumentException("subject code, building, and room are required");
                }
                if (building.length() > 100 || room.length() > 20) {
                    throw new IllegalArgumentException("building must be at most 100 characters and room at most 20");
                }
                Subject subject = subjectRepository.findBySubjectCode(subjectCode)
                        .orElseThrow(() -> new IllegalArgumentException("no subject matches code " + subjectCode));
                LocalDate date = parseDate(rawDate);
                LocalTime time = parseTime(rawTime);
                String identity = subject.getSubjectId() + "|" + date + "|" + time;
                if (!seenInFile.add(identity) || examRepository.existsBySubject_SubjectIdAndExamDateAndExamTime(subject.getSubjectId(), date, time)) {
                    skippedDuplicates++;
                    continue;
                }
                exams.add(Exam.builder()
                        .subject(subject)
                        .examDate(date)
                        .examTime(time)
                        .building(building)
                        .roomNumber(room)
                        .build());
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Row " + rowNumber + ": " + exception.getMessage());
            }
        }

        if (exams.isEmpty() && skippedDuplicates == 0) {
            throw new IllegalArgumentException("No exam rows were found in the timetable.");
        }
        examRepository.saveAll(exams);
        return TimetableImportResponse.builder()
                .imported(exams.size())
                .skippedDuplicates(skippedDuplicates)
                .message("Imported " + exams.size() + " exam(s); skipped " + skippedDuplicates + " duplicate(s).")
                .build();
    }

    private List<String[]> readSpreadsheet(MultipartFile file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) return rows;
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Sheet sheet = workbook.getSheetAt(0);
            int dateColumn = -1;
            int timeColumn = -1;
            for (Row row : sheet) {
                int width = Math.max(row.getLastCellNum(), 0);
                String[] values = new String[width];
                for (int column = 0; column < width; column++) {
                    Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    values[column] = cell == null ? "" : formatter.formatCellValue(cell).trim();
                }
                try {
                    Map<String, Integer> headers = headerColumns(values);
                    dateColumn = headers.get("date");
                    timeColumn = headers.get("time");
                } catch (IllegalArgumentException ignored) {
                    // The sheet may have a title or notes before its actual header row.
                }
                for (int column : new int[] { dateColumn, timeColumn }) {
                    if (column < 0 || column >= width) continue;
                    Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (cell == null || cell.getCellType() != CellType.NUMERIC) continue;
                    // Excel stores dates and times as serial numbers. Decode them by their
                    // timetable column so a time-only cell is never mistaken for a date.
                    if (column == dateColumn) {
                        values[column] = cell.getLocalDateTimeCellValue().toLocalDate().toString();
                    } else {
                        values[column] = cell.getLocalDateTimeCellValue().toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME);
                    }
                }
                rows.add(values);
            }
        }
        return rows;
    }

    private List<String[]> readPdf(MultipartFile file) throws IOException {
        try (var document = Loader.loadPDF(file.getBytes())) {
            String text = new PDFTextStripper().getText(document);
            List<String[]> rows = new ArrayList<>();
            for (String line : text.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.isBlank()) continue;
                String[] cells;
                if (trimmed.contains("|")) cells = trimmed.split("\\s*\\|\\s*", -1);
                else if (trimmed.contains("\t")) cells = trimmed.split("\\t", -1);
                else if (trimmed.contains(";")) cells = trimmed.split("\\s*;\\s*", -1);
                else if (trimmed.contains(",")) cells = trimmed.split("\\s*,\\s*", -1);
                else {
                    // Text-based PDFs often represent table columns with aligned whitespace.
                    // Keep spaces inside cells (for example "Main Building") intact.
                    cells = trimmed.split("\\s{2,}");
                    if (cells.length < REQUIRED_HEADERS.size()) continue;
                }
                rows.add(Arrays.stream(cells).map(String::trim).toArray(String[]::new));
            }
            return rows;
        }
    }

    private int findHeaderRow(List<String[]> rows) {
        // PDF timetables commonly have a title or institution name before the table header.
        int scanLimit = Math.min(rows.size(), 30);
        for (int index = 0; index < scanLimit; index++) {
            try {
                headerColumns(rows.get(index));
                return index;
            } catch (IllegalArgumentException ignored) {
                // Keep scanning the introductory rows for the actual table header.
            }
        }
        return -1;
    }

    private boolean isHeaderRow(String[] row) {
        try {
            headerColumns(row);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private Map<String, Integer> headerColumns(String[] header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int index = 0; index < header.length; index++) {
            String normalized = header[index].toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
            String key = switch (normalized) {
                case "subject", "subjectcode", "course", "coursecode", "code" -> "subjectCode";
                case "date", "examdate" -> "date";
                case "time", "examtime", "starttime" -> "time";
                case "building", "venue", "location" -> "building";
                case "room", "roomnumber", "roomno" -> "room";
                default -> null;
            };
            if (key != null) columns.put(key, index);
        }
        if (columns.size() != REQUIRED_HEADERS.size()) {
            throw new IllegalArgumentException("Required headers: Subject Code, Date, Time, Building, Room. Use those labels or their common equivalents.");
        }
        return columns;
    }

    private String cell(String[] row, Map<String, Integer> columns, String name) {
        int index = columns.get(name);
        return index < row.length ? row[index] : "";
    }

    private LocalDate parseDate(String value) {
        for (DateTimeFormatter format : DATE_FORMATS) {
            try { return LocalDate.parse(value, format); }
            catch (DateTimeParseException ignored) { }
        }
        throw new IllegalArgumentException("invalid date '" + value + "' (use YYYY-MM-DD)");
    }

    private LocalTime parseTime(String value) {
        for (DateTimeFormatter format : TIME_FORMATS) {
            try { return LocalTime.parse(value.toUpperCase(Locale.ROOT), format); }
            catch (DateTimeParseException ignored) { }
        }
        throw new IllegalArgumentException("invalid time '" + value + "' (use HH:mm)");
    }
}
