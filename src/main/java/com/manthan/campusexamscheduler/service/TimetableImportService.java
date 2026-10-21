package com.manthan.campusexamscheduler.service;

import com.manthan.campusexamscheduler.dto.TimetableImportResponse;
import com.manthan.campusexamscheduler.entity.Exam;
import com.manthan.campusexamscheduler.entity.Subject;
import com.manthan.campusexamscheduler.repository.ExamRepository;
import com.manthan.campusexamscheduler.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TimetableImportService {

    private static final Set<String> REQUIRED_HEADERS = Set.of("subjectcode", "date", "time", "building", "room");
    private static final Pattern MATRIX_DATE = Pattern.compile("(?i)^\\d{1,2}-[a-z]{3}-\\d{2,4}$");
    private static final Pattern MATRIX_SUBJECT_CODE = Pattern.compile(
            "(?i)^(?=.*[a-z])(?=.*\\d)[a-z0-9]{8,}(?:-[a-z0-9]+)*$");
    private static final float MATRIX_CODE_COLUMN_TOLERANCE = 40f;
    private static final Pattern MATRIX_TIME_LABEL = Pattern.compile("(?i)\\btime\\s*:?\\s*(.+)");

    private static DateTimeFormatter englishDate(String pattern) {
        return new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(Locale.ENGLISH);
    }

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            englishDate("d/M/uuuu"),
            englishDate("d-M-uuuu"),
            englishDate("d-MMM-yy"),
            englishDate("d-MMM-uuuu"),
            englishDate("d.M.uuuu"),
            englishDate("M/d/uuuu"),
            englishDate("uuuu/M/d"),
            englishDate("d MMM uuuu"),
            englishDate("d MMM, uuuu"),
            englishDate("d MMMM uuuu"),
            englishDate("d MMMM, uuuu"),
            englishDate("MMM d, uuuu"),
            englishDate("MMMM d, uuuu")
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
                String subjectCode = normalizeSubjectCode(cell(row, columns, "subjectCode"));
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
            Map<PdfLineKey, List<TextPosition>> positionedLines = new LinkedHashMap<>();
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> textPositions) {
                    if (textPositions == null || textPositions.isEmpty()) return;
                    int page = getCurrentPageNo();
                    float y = (float) textPositions.stream().mapToDouble(TextPosition::getYDirAdj).average().orElse(0);
                    PdfLineKey existingLine = positionedLines.keySet().stream()
                            .filter(key -> key.page() == page && Math.abs(key.y() - y) <= 1.5f)
                            .min(Comparator.comparingDouble(key -> Math.abs(key.y() - y)))
                            .orElse(null);
                    positionedLines.computeIfAbsent(existingLine == null ? new PdfLineKey(page, y) : existingLine, ignored -> new ArrayList<>())
                            .addAll(textPositions);
                }
            };
            stripper.setSortByPosition(true);
            stripper.getText(document);
            List<PdfLine> pdfLines = positionedLines.entrySet().stream()
                    .sorted(Comparator.comparingInt((Map.Entry<PdfLineKey, List<TextPosition>> entry) -> entry.getKey().page())
                            .thenComparingDouble(entry -> entry.getKey().y()))
                    .map(entry -> {
                        List<PdfWord> words = toPdfWords(entry.getValue());
                        String text = words.stream().map(PdfWord::text).reduce((left, right) -> left + " " + right).orElse("");
                        return new PdfLine(entry.getKey().page(), entry.getKey().y(), text, words);
                    })
                    .filter(line -> !line.words().isEmpty())
                    .toList();

            if (pdfLines.isEmpty()) {
                throw new IllegalArgumentException("This PDF contains no selectable text. Scanned/image-only PDFs need OCR before import; try exporting the timetable as a text-based PDF or Excel file.");
            }

            List<String[]> matrixRows = parseMatrixPdf(pdfLines);
            if (matrixRows != null) return matrixRows;

            List<String[]> rows = new ArrayList<>();
            int headerLine = -1;
            List<PdfCell> headerCells = List.of();
            String[] headerRow = null;

            for (int index = 0; index < pdfLines.size(); index++) {
                PdfLine line = pdfLines.get(index);
                String[] delimited = splitPdfCells(line.text());
                if (delimited != null && isHeaderRow(delimited)) {
                    headerLine = index;
                    headerCells = groupPdfCells(line.words());
                    headerRow = delimited;
                    break;
                }

                List<PdfCell> positioned = groupPdfCells(line.words());
                String[] positionedHeader = positioned.stream().map(PdfCell::text).toArray(String[]::new);
                if (isHeaderRow(positionedHeader)) {
                    headerLine = index;
                    headerCells = positioned;
                    headerRow = positionedHeader;
                    break;
                }
            }

            if (headerLine < 0) {
                return pdfLines.stream()
                        .map(line -> Optional.ofNullable(splitPdfCells(line.text())).orElseGet(() -> new String[] { line.text().trim() }))
                        .filter(row -> Arrays.stream(row).anyMatch(value -> value != null && !value.isBlank()))
                        .toList();
            }

            for (int index = 0; index < headerLine; index++) {
                PdfLine line = pdfLines.get(index);
                String[] cells = splitPdfCells(line.text());
                rows.add(cells == null ? new String[] { line.text().trim() } : cells);
            }
            rows.add(headerRow);

            for (int index = 0; index < pdfLines.size(); index++) {
                if (index <= headerLine) continue;
                PdfLine line = pdfLines.get(index);
                String[] cells = splitPdfCells(line.text());
                if (cells == null || cells.length != headerRow.length) {
                    cells = mapPdfWordsToColumns(line.words(), headerCells);
                }
                if (cells != null && Arrays.stream(cells).anyMatch(value -> value != null && !value.isBlank())) {
                    rows.add(cells);
                }
            }
            return rows;
        }
    }

    /**
     * Handles university timetables arranged as a date-by-course matrix. Each date has
     * paired Course Code/Course Name columns, while separate Time headings divide shifts.
     */
    private List<String[]> parseMatrixPdf(List<PdfLine> lines) {
        List<PdfTimeHeader> timeHeaders = lines.stream()
                .map(line -> new PdfTimeHeader(line.page(), line.y(), matrixTime(line.text())))
                .filter(header -> header.time() != null)
                .sorted(Comparator.comparingInt(PdfTimeHeader::page).thenComparingDouble(PdfTimeHeader::y))
                .toList();
        if (timeHeaders.isEmpty()) return null;

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] { "Subject Code", "Date", "Time", "Building", "Room" });
        boolean foundMatrix = false;
        for (int index = 0; index < timeHeaders.size(); index++) {
            PdfTimeHeader session = timeHeaders.get(index);
            float endY = index + 1 < timeHeaders.size() && timeHeaders.get(index + 1).page() == session.page()
                    ? timeHeaders.get(index + 1).y()
                    : Float.POSITIVE_INFINITY;
            List<PdfLine> sessionLines = lines.stream()
                    .filter(line -> line.page() == session.page() && line.y() > session.y() && line.y() < endY)
                    .toList();
            PdfLine dateHeader = sessionLines.stream()
                    .filter(line -> dateTokens(line.words()).size() >= 2)
                    .findFirst()
                    .orElse(null);
            if (dateHeader == null) continue;
            PdfLine courseHeader = sessionLines.stream()
                    .filter(line -> line.y() > dateHeader.y())
                    .filter(line -> courseCodeColumns(line.words()).size() >= 2)
                    .findFirst()
                    .orElse(null);
            if (courseHeader == null) continue;

            List<PdfWord> dates = dateTokens(dateHeader.words());
            List<Float> courseColumns = courseCodeColumns(courseHeader.words());
            if (dates.size() != courseColumns.size()) continue;
            foundMatrix = true;

            for (PdfLine line : sessionLines) {
                if (line.y() <= courseHeader.y()) continue;
                for (PdfWord word : line.words()) {
                    String subjectCode = matrixSubjectCode(word.text());
                    if (subjectCode == null) continue;
                    int column = nearestColumn(word.startX(), courseColumns);
                    if (Math.abs(word.startX() - courseColumns.get(column)) > MATRIX_CODE_COLUMN_TOLERANCE) continue;
                    LocalDate date = parseDate(dates.get(column).text());
                    rows.add(new String[] {
                            subjectCode,
                            date.toString(),
                            session.time().toString(),
                            "Not assigned",
                            "Not assigned"
                    });
                }
            }
        }
        return foundMatrix ? rows : null;
    }

    private LocalTime matrixTime(String line) {
        Matcher matcher = MATRIX_TIME_LABEL.matcher(line);
        if (!matcher.find()) return null;
        String value = matcher.group(1).replaceAll("(?i)\\s+(?:MDM|HM)\\b.*$", "").trim();
        try {
            return parseTime(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private List<PdfWord> dateTokens(List<PdfWord> words) {
        return words.stream()
                .filter(word -> MATRIX_DATE.matcher(word.text()).matches())
                .sorted(Comparator.comparing(PdfWord::startX))
                .toList();
    }

    private List<Float> courseCodeColumns(List<PdfWord> words) {
        List<PdfWord> ordered = words.stream().sorted(Comparator.comparing(PdfWord::startX)).toList();
        List<Float> columns = new ArrayList<>();
        for (int index = 0; index + 1 < ordered.size(); index++) {
            String first = ordered.get(index).text().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
            String second = ordered.get(index + 1).text().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
            if (first.equals("course") && second.equals("code")) columns.add(ordered.get(index).startX());
        }
        return columns;
    }

    private int nearestColumn(float x, List<Float> columns) {
        int nearest = 0;
        float distance = Float.MAX_VALUE;
        for (int index = 0; index < columns.size(); index++) {
            float candidate = Math.abs(x - columns.get(index));
            if (candidate < distance) {
                nearest = index;
                distance = candidate;
            }
        }
        return nearest;
    }

    private String matrixSubjectCode(String value) {
        String normalized = normalizeSubjectCode(value);
        return MATRIX_SUBJECT_CODE.matcher(normalized).matches() ? normalized : null;
    }

    private String normalizeSubjectCode(String value) {
        return value.replaceAll("\\s+", "").replaceAll("[*†‡]+$", "").trim().toUpperCase(Locale.ROOT);
    }

    private String[] splitPdfCells(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isBlank()) return null;

        String[] cells = null;
        if (trimmed.contains("|")) cells = trimmed.split("\\s*\\|\\s*", -1);
        else if (trimmed.contains("\t")) cells = trimmed.split("\\t", -1);
        else if (trimmed.contains(";")) cells = trimmed.split("\\s*;\\s*", -1);
        else if (trimmed.contains(",")) cells = trimmed.split("\\s*,\\s*", -1);
        else {
            String[] whitespaceCells = trimmed.split("\\s{2,}");
            if (whitespaceCells.length >= REQUIRED_HEADERS.size()) cells = whitespaceCells;
        }

        if (cells == null || cells.length < REQUIRED_HEADERS.size()) return null;
        return Arrays.stream(cells).map(String::trim).toArray(String[]::new);
    }

    private List<PdfWord> toPdfWords(List<TextPosition> positions) {
        if (positions == null || positions.isEmpty()) return List.of();

        List<TextPosition> ordered = positions.stream()
                .sorted(Comparator.comparing(TextPosition::getXDirAdj))
                .toList();
        List<Double> characterWidths = ordered.stream()
                .filter(position -> !position.getUnicode().isBlank())
                .mapToDouble(TextPosition::getWidthDirAdj)
                .filter(width -> width > 0)
                .sorted()
                .boxed()
                .toList();
        double medianCharacterWidth = characterWidths.isEmpty()
                ? 5.0
                : characterWidths.get(characterWidths.size() / 2);
        float characterGap = (float) Math.max(1.0, medianCharacterWidth * 0.35);

        List<PdfWord> words = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        float wordStart = 0;
        float wordEnd = 0;
        for (TextPosition position : ordered) {
            String unicode = position.getUnicode();
            if (unicode == null || unicode.isBlank()) {
                addPdfWord(words, current, wordStart, wordEnd);
                continue;
            }

            float x = position.getXDirAdj();
            float end = x + position.getWidthDirAdj();
            if (!current.isEmpty() && x - wordEnd > characterGap) {
                addPdfWord(words, current, wordStart, wordEnd);
            }
            if (current.isEmpty()) wordStart = x;
            current.append(unicode);
            wordEnd = end;
        }
        addPdfWord(words, current, wordStart, wordEnd);
        return words;
    }

    private void addPdfWord(List<PdfWord> words, StringBuilder current, float startX, float endX) {
        if (!current.isEmpty()) {
            words.add(new PdfWord(current.toString(), startX, endX));
            current.setLength(0);
        }
    }

    private List<PdfCell> groupPdfCells(List<PdfWord> words) {
        if (words.isEmpty()) return List.of();

        List<Float> gaps = new ArrayList<>();
        for (int index = 1; index < words.size(); index++) {
            float gap = words.get(index).startX() - words.get(index - 1).endX();
            if (gap > 0) gaps.add(gap);
        }
        gaps.sort(Float::compareTo);
        float medianGap = gaps.isEmpty() ? 0 : gaps.get(gaps.size() / 2);
        float columnGap = Math.max(5f, medianGap * 0.45f);

        List<PdfCell> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        float cellStart = words.get(0).startX();
        float cellEnd = words.get(0).endX();
        PdfWord previous = null;
        for (PdfWord word : words) {
            if (previous != null && word.startX() - previous.endX() > columnGap) {
                cells.add(new PdfCell(current.toString().trim(), cellStart, cellEnd));
                current.setLength(0);
                cellStart = word.startX();
            } else if (!current.isEmpty()) {
                current.append(' ');
            }
            current.append(word.text());
            cellEnd = word.endX();
            previous = word;
        }
        if (!current.isEmpty()) cells.add(new PdfCell(current.toString().trim(), cellStart, cellEnd));
        return cells;
    }

    private String[] mapPdfWordsToColumns(List<PdfWord> words, List<PdfCell> headers) {
        if (words.isEmpty() || headers.size() < REQUIRED_HEADERS.size()) return null;

        List<PdfCell> groupedCells = groupPdfCells(words);
        if (groupedCells.size() == headers.size()) {
            return groupedCells.stream().map(PdfCell::text).toArray(String[]::new);
        }

        String[] cells = new String[headers.size()];
        Arrays.fill(cells, "");
        float[] boundaries = new float[headers.size() - 1];
        for (int index = 0; index < boundaries.length; index++) {
            boundaries[index] = (headers.get(index).endX() + headers.get(index + 1).startX()) / 2f;
        }

        // Map words by their left edge. This keeps long values such as "12 November 2026"
        // together even when the final word extends beyond a short heading like "Date".
        for (PdfWord word : words) {
            float x = word.startX();
            int column = 0;
            while (column < boundaries.length && x > boundaries[column]) column++;
            cells[column] = cells[column].isBlank() ? word.text() : cells[column] + " " + word.text();
        }
        return cells;
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
                case "subject", "subjectcode", "subjectcodename", "subjectid", "papercode", "course", "coursecode", "courseid", "code" -> "subjectCode";
                case "date", "examdate", "dateofexam", "examinationdate" -> "date";
                case "time", "examtime", "examinationtime", "starttime", "timeslot", "timing" -> "time";
                case "building", "venue", "examvenue", "examcenter", "examcentre", "location", "campus", "block" -> "building";
                case "room", "roomnumber", "roomno", "classroom", "hall", "hallnumber" -> "room";
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
        String normalized = value.trim().replaceAll("(?i)(\\d{1,2})(st|nd|rd|th)\\b", "$1");
        for (DateTimeFormatter format : DATE_FORMATS) {
            try { return LocalDate.parse(normalized, format); }
            catch (DateTimeParseException ignored) { }
        }
        throw new IllegalArgumentException("invalid date '" + value + "' (use YYYY-MM-DD)");
    }

    private LocalTime parseTime(String value) {
        String startTime = value.trim()
                .replaceAll("^[\\[(]+|[\\])]+$", "")
                .split("\\s*(?:-|–|—|\\bto\\b)\\s*", 2)[0];
        String normalized = startTime.toUpperCase(Locale.ROOT)
                .replaceAll("A\\.?M\\.?", "AM")
                .replaceAll("P\\.?M\\.?", "PM")
                .replace('.', ':')
                .replaceAll("(?<=\\d)(AM|PM)$", " $1")
                .replaceAll("\\s+", " ");
        if (normalized.matches("\\d{1,2} [AP]M")) {
            normalized = normalized.replaceAll("(\\d{1,2}) ([AP]M)$", "$1:00 $2");
        }
        for (DateTimeFormatter format : TIME_FORMATS) {
            try { return LocalTime.parse(normalized, format); }
            catch (DateTimeParseException ignored) { }
        }
        throw new IllegalArgumentException("invalid time '" + value + "' (use HH:mm or h:mm AM/PM)");
    }

    private record PdfLineKey(int page, float y) { }
    private record PdfLine(int page, float y, String text, List<PdfWord> words) { }
    private record PdfTimeHeader(int page, float y, LocalTime time) { }
    private record PdfWord(String text, float startX, float endX) { }
    private record PdfCell(String text, float startX, float endX) { }
}
