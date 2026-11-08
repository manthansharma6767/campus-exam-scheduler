package com.manthan.campusexamscheduler.service;

import com.manthan.campusexamscheduler.dto.ImportRowError;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads image-only timetable PDFs by rendering each page with PDFBox and using the
 * Tesseract command-line engine to locate the date, time, and course-code cells.
 *
 * <p>The scan used by the university timetable has no table lines and its header is
 * unreliable to OCR, so column geometry is derived from recurring content patterns
 * instead: dates form a left column, times sit in the middle, and course codes form
 * the right column. Each row is anchored on a date (or a short serial-number token
 * that has a code beside it) and each cell is re-OCR'd from a cropped, upscaled image.</p>
 */
final class ScannedPdfTimetableReader {

    private static final int RENDER_DPI = 300;
    private static final int OCR_BORDER = 25;
    private static final int ROW_TEXT_HEIGHT = 240;
    private static final int ANCHOR_MERGE_DISTANCE = 60;
    private static final String CODE_WHITELIST = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-";
    private static final String DATE_WHITELIST = "0123456789/";

    private static final Pattern DATE_TOKEN = Pattern.compile("^(\\d{1,2})[/.](\\d{1,2})[/.](\\d{2,4})$");
    private static final Pattern DATE_VALUE = Pattern.compile("(\\d{1,2})[/.](\\d{1,2})[/.](\\d{2,4})");
    private static final Pattern TIME_TOKEN = Pattern.compile("^\\d{1,2}[.:]\\d{2}$");
    private static final Pattern TIME_WORD = Pattern.compile("^(?i)(AM|PM|TO|NOON|MIDNIGHT)$");
    private static final Pattern CODE_TOKEN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z0-9-]{5,}$");
    private static final Pattern CODE_PREFIX = Pattern.compile(".*(TH|MT)$");
    private static final Pattern CODE_FRAGMENT = Pattern.compile("^\\d{2}[A-Z0-9-]*(TH|MT)[A-Z0-9-]{4,}$");
    private static final Pattern CODE_NUMBER = Pattern.compile("^0[1-9]\\d{2}$");
    private static final Pattern VALID_CODE = Pattern.compile("^\\d{2}[A-Z]{2,5}\\d{2}(TH|MT)0[1-9]\\d{2}(-\\d)?$");
    private static final Pattern TIME_RANGE = Pattern.compile(
            "(\\d{1,2}:\\d{2})\\s*(AM|PM)?\\s*(?:to|-)\\s*(\\d{1,2}:\\d{2})\\s*(AM|PM)?",
            Pattern.CASE_INSENSITIVE);

    private static final Map<Character, Character> DIGIT_MAP = digitMap();
    private static final Map<Character, Character> LETTER_MAP = letterMap();

    private ScannedPdfTimetableReader() { }

    record ScanResult(List<String[]> rows, List<ImportRowError> errors) { }

    static ScanResult read(PDDocument document) {
        String tesseract = findTesseract();
        Path workDirectory = null;
        try {
            workDirectory = Files.createTempDirectory("campus-exam-ocr-");
            PDFRenderer renderer = new PDFRenderer(document);
            List<String[]> dataRows = new ArrayList<>();
            List<ImportRowError> errors = new ArrayList<>();
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, RENDER_DPI, ImageType.GRAY);
                Path pageImage = workDirectory.resolve("page-" + (page + 1) + ".png");
                ImageIO.write(image, "png", pageImage.toFile());
                List<OcrWord> words = readTsv(tesseract, pageImage, workDirectory);
                PageResult pageResult = analyzePage(image, words, tesseract, workDirectory, dataRows.size());
                dataRows.addAll(pageResult.rows());
                errors.addAll(pageResult.errors());
            }

            if (dataRows.isEmpty()) {
                throw new IllegalArgumentException("This PDF contains no selectable text. OCR was attempted, "
                        + "but no timetable rows could be detected. Try exporting the timetable as a text-based "
                        + "PDF or Excel file.");
            }

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[] { "Subject Code", "Date", "Time", "Building", "Room" });
            rows.addAll(dataRows);
            return new ScanResult(rows, errors);
        } catch (IOException exception) {
            throw new IllegalArgumentException("This PDF contains no selectable text and OCR could not read it: "
                    + exception.getMessage(), exception);
        } finally {
            deleteRecursively(workDirectory);
        }
    }

    private static PageResult analyzePage(
            BufferedImage image,
            List<OcrWord> words,
            String tesseract,
            Path workDirectory,
            int existingDataRows) {
        List<OcrWord> dateWords = words.stream()
                .filter(word -> DATE_TOKEN.matcher(word.text()).matches())
                .toList();
        if (dateWords.isEmpty()) return PageResult.empty();

        List<OcrWord> dateCluster = List.of();
        for (OcrWord candidate : dateWords) {
            List<OcrWord> cluster = dateWords.stream()
                    .filter(word -> Math.abs(word.left() - candidate.left()) <= 60)
                    .toList();
            if (cluster.size() > dateCluster.size()) dateCluster = cluster;
        }

        int windowTop = Math.max(0, dateCluster.stream().mapToInt(OcrWord::top).min().orElse(0) - 120);
        int windowBottom = Math.min(image.getHeight(),
                dateCluster.stream().mapToInt(OcrWord::top).max().orElse(0) + 700);
        List<OcrWord> window = words.stream()
                .filter(word -> word.top() >= windowTop && word.top() <= windowBottom)
                .toList();
        List<OcrWord> dates = window.stream()
                .filter(word -> DATE_TOKEN.matcher(word.text()).matches())
                .toList();
        if (dates.isEmpty()) return PageResult.empty();

        int dateMin = dates.stream().mapToInt(OcrWord::left).min().orElse(0);
        List<OcrWord> times = window.stream()
                .filter(word -> TIME_TOKEN.matcher(word.text()).matches())
                .toList();
        if (times.isEmpty()) return PageResult.empty();
        int timeMin = times.stream().mapToInt(OcrWord::left).min().orElse(0);
        int timeMax = times.stream().mapToInt(OcrWord::right).max().orElse(0);
        List<OcrWord> timeWords = window.stream()
                .filter(word -> TIME_WORD.matcher(word.text()).matches())
                .filter(word -> word.left() >= timeMin - 200 && word.left() <= timeMax + 350)
                .toList();
        List<OcrWord> allTimeWords = new ArrayList<>(times);
        allTimeWords.addAll(timeWords);
        int allTimeMin = allTimeWords.stream().mapToInt(OcrWord::left).min().orElse(timeMin);
        int allTimeMax = allTimeWords.stream().mapToInt(OcrWord::right).max().orElse(timeMax);

        List<OcrWord> codes = window.stream()
                .filter(word -> CODE_TOKEN.matcher(word.text()).matches())
                .filter(word -> !DATE_TOKEN.matcher(word.text()).matches())
                .filter(word -> !TIME_TOKEN.matcher(word.text()).matches())
                .toList();
        if (codes.isEmpty()) return PageResult.empty();
        int codeMin = codes.stream().mapToInt(OcrWord::left).min().orElse(0);
        int codeMax = codes.stream().mapToInt(OcrWord::right).max().orElse(0);

        List<Anchor> anchors = new ArrayList<>();
        for (OcrWord word : window) {
            if (DATE_TOKEN.matcher(word.text()).matches()) {
                anchors.add(new Anchor(word.top(), "date:" + word.text()));
                continue;
            }
            double center = (word.left() + word.right()) / 2.0;
            boolean nearCode = codes.stream()
                    .anyMatch(code -> Math.abs(code.top() - word.top()) <= 100);
            if (center < dateMin - 40 && word.text().length() <= 4 && nearCode) {
                anchors.add(new Anchor(word.top(), "sno:" + word.text()));
            }
        }
        if (anchors.isEmpty()) return PageResult.empty();
        anchors.sort(Comparator.comparingInt(Anchor::top).thenComparing(Anchor::label));

        List<Anchor> merged = new ArrayList<>();
        for (Anchor anchor : anchors) {
            if (!merged.isEmpty() && anchor.top() - merged.get(merged.size() - 1).top() < ANCHOR_MERGE_DISTANCE) {
                continue;
            }
            merged.add(anchor);
        }

        int dateX0 = Math.max(0, dateMin - 12);
        int dateX1 = Math.min(Math.min(allTimeMin - 45, dateMin + 330), image.getWidth());
        int timeX0 = Math.max(0, allTimeMin - 15);
        int timeX1 = Math.min(Math.min(codeMin - 40, allTimeMax + 60), image.getWidth());
        int codeX0 = Math.max(0, codeMin - 12);
        int codeX1 = Math.min(codeMax + 35, image.getWidth());
        if (dateX1 <= dateX0 || timeX1 <= timeX0 || codeX1 <= codeX0) return PageResult.empty();

        List<String[]> pageRows = new ArrayList<>();
        List<ImportRowError> pageErrors = new ArrayList<>();
        for (int index = 0; index < merged.size(); index++) {
            Anchor anchor = merged.get(index);
            boolean last = index == merged.size() - 1;
            int start = Math.max(0, anchor.top() - 10);
            int codeTop = Math.max(0, anchor.top() - (last ? 5 : 60));
            int end = index + 1 < merged.size()
                    ? Math.min(windowBottom, merged.get(index + 1).top() - 10)
                    : windowBottom;
            if (end - start < 30) continue;
            int textBottom = Math.min(end, start + ROW_TEXT_HEIGHT);
            if (textBottom <= start) continue;

            BufferedImage dateImage = prepareCell(image, dateX0, start, dateX1, textBottom, 4, false);
            Set<String> dateValues = new LinkedHashSet<>();
            dateValues.addAll(dateCandidates(ocrImage(tesseract, dateImage, 7, DATE_WHITELIST, workDirectory)));
            dateValues.addAll(dateCandidates(ocrImage(tesseract, dateImage, 6, DATE_WHITELIST, workDirectory)));

            BufferedImage timeImage3 = prepareCell(image, timeX0, start, timeX1, textBottom, 3, false);
            BufferedImage timeImage4 = prepareCell(image, timeX0, start, timeX1, textBottom, 4, false);
            Set<String> timeValues = new LinkedHashSet<>();
            addIfPresent(timeValues, parseTimeRange(ocrImage(tesseract, timeImage3, 6, null, workDirectory)));
            addIfPresent(timeValues, parseTimeRange(ocrImage(tesseract, timeImage4, 6, null, workDirectory)));

            BufferedImage codeImage3 = prepareCell(image, codeX0, codeTop, codeX1, end, 3, false);
            BufferedImage codeImage4 = prepareCell(image, codeX0, codeTop, codeX1, end, 4, false);
            BufferedImage codeImageThreshold = prepareCell(image, codeX0, codeTop, codeX1, end, 3, true);
            List<List<Set<String>>> codeVariants = new ArrayList<>();
            codeVariants.add(codeCandidates(ocrImage(tesseract, codeImage3, 6, CODE_WHITELIST, workDirectory)));
            codeVariants.add(codeCandidates(ocrImage(tesseract, codeImage4, 6, CODE_WHITELIST, workDirectory)));
            codeVariants.add(codeCandidates(ocrImage(tesseract, codeImageThreshold, 6, CODE_WHITELIST, workDirectory)));
            codeVariants.add(codeCandidates(ocrImage(tesseract, codeImage4, 4, CODE_WHITELIST, workDirectory)));
            codeVariants.add(codeCandidates(ocrImage(tesseract, codeImageThreshold, 4, CODE_WHITELIST, workDirectory)));

            Set<String> chosenCodes = new LinkedHashSet<>();
            boolean uncertainCode = false;
            int maxGroups = codeVariants.stream().mapToInt(List::size).max().orElse(0);
            for (int group = 0; group < maxGroups; group++) {
                Map<String, Integer> votes = new LinkedHashMap<>();
                for (List<Set<String>> variant : codeVariants) {
                    if (group >= variant.size()) continue;
                    for (String code : variant.get(group)) {
                        votes.merge(code, 1, Integer::sum);
                    }
                }
                if (votes.isEmpty()) continue;
                int topVotes = votes.values().stream().mapToInt(Integer::intValue).max().orElse(0);
                List<String> winners = votes.entrySet().stream()
                        .filter(entry -> entry.getValue() == topVotes)
                        .map(Map.Entry::getKey)
                        .toList();
                if (winners.size() == 1 && topVotes >= 2) {
                    chosenCodes.add(winners.get(0));
                } else {
                    uncertainCode = true;
                }
            }

            boolean dateCertain = dateValues.size() == 1;
            boolean timeCertain = timeValues.size() == 1;
            if (dateCertain && timeCertain) {
                String date = dateValues.iterator().next();
                String time = timeValues.iterator().next();
                for (String code : chosenCodes) {
                    pageRows.add(new String[] { code, date, time, "Not assigned", "Not assigned" });
                }
            }

            int rowNumber = existingDataRows + pageRows.size() + 2;
            if (!dateCertain) {
                pageErrors.add(new ImportRowError(rowNumber,
                        "OCR could not reliably read the exam date for this row. Review the scanned timetable "
                                + "or export a text-based PDF."));
            }
            if (!timeCertain) {
                pageErrors.add(new ImportRowError(rowNumber,
                        "OCR could not reliably read the exam time for this row. Review the scanned timetable "
                                + "or export a text-based PDF."));
            }
            if (uncertainCode || (dateCertain && timeCertain && chosenCodes.isEmpty())) {
                pageErrors.add(new ImportRowError(rowNumber,
                        "OCR could not reliably read a subject code for this row. Review the scanned timetable "
                                + "or export a text-based PDF."));
            }
        }
        return new PageResult(pageRows, pageErrors);
    }

    private static void addIfPresent(Set<String> values, String value) {
        if (value != null) values.add(value);
    }

    private static Set<String> dateCandidates(List<String> lines) {
        Set<String> values = new LinkedHashSet<>();
        for (String line : lines) {
            Matcher matcher = DATE_VALUE.matcher(line);
            while (matcher.find()) {
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                if (year < 100) year += 2000;
                if (month >= 1 && month <= 12 && day >= 1 && day <= 31 && year >= 2000 && year <= 2100) {
                    values.add(String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day));
                }
            }
        }
        return values;
    }

    private static String parseTimeRange(List<String> lines) {
        String text = String.join(" ", lines)
                .replaceAll("(?i)\\bnoon\\b", "PM")
                .replaceAll("(?i)\\bmidnight\\b", "AM")
                .replaceAll("(\\d{1,2})\\.(\\d{2})", "$1:$2")
                .replaceAll("[^A-Za-z0-9:.\\- ]", " ");
        Matcher matcher = TIME_RANGE.matcher(text);
        if (!matcher.find()) return null;
        String startValue = matcher.group(1);
        String startMeridiem = matcher.group(2);
        String endValue = matcher.group(3);
        String endMeridiem = matcher.group(4);
        if (startMeridiem == null || endMeridiem == null) return null;

        int start = minutes(startValue, startMeridiem);
        int end = minutes(endValue, endMeridiem);
        if (start < 0 || end < 0 || start >= 1440 || end >= 1440 || end <= start) return null;
        return String.format(Locale.ROOT, "%02d:%02d-%02d:%02d", start / 60, start % 60, end / 60, end % 60);
    }

    private static int minutes(String value, String meridiem) {
        String[] parts = value.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        if (hour == 12) hour = 0;
        if ("PM".equalsIgnoreCase(meridiem)) hour += 12;
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return -1;
        return hour * 60 + minute;
    }

    private static List<Set<String>> codeCandidates(List<String> lines) {
        List<Set<String>> candidates = new ArrayList<>();
        for (String group : groupCodes(lines)) {
            Set<String> parsed = parseCode(group);
            if (!parsed.isEmpty()) candidates.add(parsed);
        }
        return candidates;
    }

    private static List<String> groupCodes(List<String> lines) {
        List<String> groups = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            String fragment = cleanFragment(line);
            if (fragment == null) continue;
            if (CODE_FRAGMENT.matcher(fragment).matches()) {
                if (!current.isEmpty()) {
                    groups.add(current.toString());
                    current.setLength(0);
                }
                groups.add(fragment);
            } else if (CODE_PREFIX.matcher(fragment).matches()) {
                if (!current.isEmpty()) groups.add(current.toString());
                current.setLength(0);
                current.append(fragment);
            } else if (!current.isEmpty()) {
                current.append(fragment);
            } else {
                groups.add(fragment);
            }
        }
        if (!current.isEmpty()) groups.add(current.toString());
        return groups;
    }

    private static String cleanFragment(String line) {
        String text = line == null ? "" : line.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9-]", "");
        if (text.length() < 4 || text.length() > 16 || !text.matches(".*\\d.*")) return null;
        return text;
    }

    private static Set<String> parseCode(String raw) {
        Set<String> results = new LinkedHashSet<>();
        String value = raw == null ? "" : raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9-]", "");
        String sectionRaw = null;
        int dash = value.lastIndexOf('-');
        if (dash >= 0) {
            String section = value.substring(dash + 1);
            if (section.length() >= 1 && section.length() <= 2) {
                sectionRaw = section;
                value = value.substring(0, dash);
            }
        }
        if (value.length() < 8 || !Character.isDigit(value.charAt(0)) || !Character.isDigit(value.charAt(1))) {
            return results;
        }

        String year = value.substring(0, 2);
        int maxLetterEnd = Math.min(7, value.length());
        for (int letterEnd = 4; letterEnd <= maxLetterEnd; letterEnd++) {
            StringBuilder department = new StringBuilder();
            boolean valid = true;
            for (int index = 2; index < letterEnd; index++) {
                Character letter = LETTER_MAP.get(value.charAt(index));
                if (letter == null) {
                    valid = false;
                    break;
                }
                department.append(letter);
            }
            if (!valid || department.length() < 2 || department.length() > 5) continue;
            if (letterEnd + 2 > value.length()) continue;

            StringBuilder semester = new StringBuilder();
            for (int index = letterEnd; index < letterEnd + 2; index++) {
                Character digit = DIGIT_MAP.get(value.charAt(index));
                if (digit == null) {
                    valid = false;
                    break;
                }
                semester.append(digit);
            }
            if (!valid) continue;

            String rest = value.substring(letterEnd + 2);
            for (int skip = 0; skip <= 1; skip++) {
                if (skip + 2 > rest.length()) continue;
                String type = rest.substring(skip, skip + 2);
                if (!type.equals("TH") && !type.equals("MT")) continue;

                StringBuilder digits = new StringBuilder();
                for (int index = skip + 2; index < rest.length(); index++) {
                    char character = rest.charAt(index);
                    Character digit = DIGIT_MAP.get(character);
                    if (digit != null) {
                        digits.append(digit);
                    } else if (Character.isDigit(character)) {
                        digits.append(character);
                    }
                }
                if (digits.length() < 4) continue;

                Set<String> windows = new LinkedHashSet<>();
                for (int index = 0; index + 4 <= digits.length(); index++) {
                    String window = digits.substring(index, index + 4);
                    if (CODE_NUMBER.matcher(window).matches()) windows.add(window);
                }
                List<String> validNumbers = new ArrayList<>(windows);
                validNumbers.sort(String::compareTo);
                List<String> preferred = validNumbers.stream().filter(number -> number.startsWith("05")).toList();
                if (preferred.size() == 1) validNumbers = preferred;
                if (validNumbers.size() != 1) continue;

                String section = null;
                if (sectionRaw != null) {
                    StringBuilder sectionDigits = new StringBuilder();
                    for (char character : sectionRaw.toCharArray()) {
                        Character digit = DIGIT_MAP.get(character);
                        if (digit != null) sectionDigits.append(digit);
                    }
                    if (sectionDigits.length() != 1) continue;
                    section = sectionDigits.toString();
                }

                String code = year + department + semester + type + validNumbers.get(0)
                        + (section == null ? "" : "-" + section);
                if (VALID_CODE.matcher(code).matches()) results.add(code);
            }
        }
        return results;
    }

    private static List<String> ocrImage(
            String tesseract,
            BufferedImage image,
            int psm,
            String whitelist,
            Path workDirectory) {
        Path cellImage = null;
        try {
            cellImage = Files.createTempFile(workDirectory, "cell-", ".png");
            ImageIO.write(image, "png", cellImage.toFile());
            List<String> arguments = new ArrayList<>();
            arguments.add(cellImage.toString());
            arguments.add("stdout");
            arguments.add("--psm");
            arguments.add(Integer.toString(psm));
            arguments.add("-l");
            arguments.add("eng");
            if (whitelist != null) {
                arguments.add("-c");
                arguments.add("tessedit_char_whitelist=" + whitelist);
            }
            String output = runTesseract(tesseract, arguments, workDirectory);
            return output.lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty())
                    .toList();
        } catch (IOException exception) {
            throw new IllegalArgumentException("OCR failed while reading a timetable cell: " + exception.getMessage(),
                    exception);
        } finally {
            if (cellImage != null) {
                try {
                    Files.deleteIfExists(cellImage);
                } catch (IOException ignored) {
                    // The whole OCR work directory is removed after the import.
                }
            }
        }
    }

    private static List<OcrWord> readTsv(String tesseract, Path pageImage, Path workDirectory) throws IOException {
        String output = runTesseract(tesseract,
                List.of(pageImage.toString(), "stdout", "--psm", "6", "-l", "eng", "tsv"),
                workDirectory);
        List<OcrWord> words = new ArrayList<>();
        for (String line : output.split("\\R")) {
            String[] columns = line.split("\t", -1);
            if (columns.length < 12 || !"5".equals(columns[0])) continue;
            String text = columns[11].strip();
            if (text.isEmpty()) continue;
            try {
                Double.parseDouble(columns[10]);
                int left = Integer.parseInt(columns[6]);
                int top = Integer.parseInt(columns[7]);
                int width = Integer.parseInt(columns[8]);
                int height = Integer.parseInt(columns[9]);
                if (width <= 0 || height <= 0) continue;
                words.add(new OcrWord(text, left, top, width, left + width));
            } catch (NumberFormatException ignored) {
                // Ignore malformed rows such as Tesseract's non-TSV diagnostics.
            }
        }
        return words;
    }

    private static String runTesseract(String tesseract, List<String> arguments, Path workDirectory)
            throws IOException {
        List<String> command = new ArrayList<>();
        command.add(tesseract);
        command.addAll(arguments);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workDirectory.toFile());
        Process process = builder.start();
        byte[] standardOutput;
        try (InputStream input = process.getInputStream()) {
            standardOutput = input.readAllBytes();
        }
        byte[] standardError;
        try (InputStream input = process.getErrorStream()) {
            standardError = input.readAllBytes();
        }
        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for Tesseract.", exception);
        }
        if (exitCode != 0) {
            String message = new String(standardError, StandardCharsets.UTF_8).strip();
            throw new IOException(message.isEmpty() ? "Tesseract exited with code " + exitCode : message);
        }
        return new String(standardOutput, StandardCharsets.UTF_8);
    }

    private static String findTesseract() {
        String configured = System.getenv("TESSERACT_CMD");
        if (configured != null && !configured.isBlank()) return configured;

        String path = System.getenv("PATH");
        if (path != null) {
            for (String directory : path.split(Pattern.quote(File.pathSeparator))) {
                if (directory.isBlank()) continue;
                Path executable = Path.of(directory, "tesseract.exe");
                if (Files.isRegularFile(executable)) return executable.toString();
                Path command = Path.of(directory, "tesseract");
                if (Files.isRegularFile(command)) return command.toString();
            }
        }

        Path standard = Path.of("C:\\Program Files\\Tesseract-OCR\\tesseract.exe");
        if (Files.isRegularFile(standard)) return standard.toString();
        throw new IllegalArgumentException("This PDF contains no selectable text. OCR requires the Tesseract "
                + "engine, but it was not found. Install Tesseract or set the TESSERACT_CMD environment variable.");
    }

    private static BufferedImage prepareCell(
            BufferedImage source,
            int x0,
            int y0,
            int x1,
            int y1,
            int scale,
            boolean threshold) {
        int left = Math.max(0, x0);
        int top = Math.max(0, y0);
        int right = Math.min(source.getWidth(), x1);
        int bottom = Math.min(source.getHeight(), y1);
        if (right <= left || bottom <= top) {
            throw new IllegalArgumentException("OCR cell is outside the rendered PDF page.");
        }

        BufferedImage crop = source.getSubimage(left, top, right - left, bottom - top);
        if (threshold) crop = autocontrastAndThreshold(crop);

        int width = Math.max(1, crop.getWidth() * scale);
        int height = Math.max(1, crop.getHeight() * scale);
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(crop, 0, 0, width, height, null);
        graphics.dispose();

        BufferedImage bordered = new BufferedImage(width + OCR_BORDER * 2, height + OCR_BORDER * 2,
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D border = bordered.createGraphics();
        border.setColor(Color.WHITE);
        border.fillRect(0, 0, bordered.getWidth(), bordered.getHeight());
        border.drawImage(scaled, OCR_BORDER, OCR_BORDER, null);
        border.dispose();
        return bordered;
    }

    private static BufferedImage autocontrastAndThreshold(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        WritableRaster raster = source.getRaster();
        int[] values = new int[width * height];
        int minimum = 255;
        int maximum = 0;
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = raster.getSample(x, y, 0);
                values[index++] = value;
                minimum = Math.min(minimum, value);
                maximum = Math.max(maximum, value);
            }
        }

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster output = result.getRaster();
        int range = maximum - minimum;
        index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = range == 0 ? values[index++] : (values[index++] - minimum) * 255 / range;
                output.setSample(x, y, 0, value > 175 ? 255 : 0);
            }
        }
        return result;
    }

    private static void deleteRecursively(Path directory) {
        if (directory == null || !Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Best-effort cleanup of temporary OCR files.
                }
            });
        } catch (IOException ignored) {
            // Best-effort cleanup of temporary OCR files.
        }
    }

    private static Map<Character, Character> digitMap() {
        Map<Character, Character> map = new LinkedHashMap<>();
        for (char digit = '0'; digit <= '9'; digit++) map.put(digit, digit);
        map.put('O', '0');
        map.put('Q', '0');
        map.put('D', '0');
        map.put('U', '0');
        map.put('I', '1');
        map.put('L', '1');
        map.put('S', '5');
        map.put('B', '8');
        map.put('Z', '2');
        map.put('G', '6');
        return Map.copyOf(map);
    }

    private static Map<Character, Character> letterMap() {
        Map<Character, Character> map = new LinkedHashMap<>();
        for (char letter = 'A'; letter <= 'Z'; letter++) map.put(letter, letter);
        map.put('0', 'O');
        map.put('1', 'I');
        map.put('5', 'S');
        map.put('8', 'B');
        map.put('2', 'Z');
        map.put('6', 'G');
        return Map.copyOf(map);
    }

    private record OcrWord(String text, int left, int top, int width, int right) { }
    private record Anchor(int top, String label) { }
    private record PageResult(List<String[]> rows, List<ImportRowError> errors) {
        static PageResult empty() {
            return new PageResult(List.of(), List.of());
        }
    }
}
