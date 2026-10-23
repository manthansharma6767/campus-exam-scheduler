package com.manthan.campusexamscheduler.service;

import com.manthan.campusexamscheduler.dto.SeatLookupResponse;
import com.manthan.campusexamscheduler.dto.SeatingPlanImportResponse;
import com.manthan.campusexamscheduler.dto.SeatingPlanResponse;
import com.manthan.campusexamscheduler.entity.SeatAssignment;
import com.manthan.campusexamscheduler.entity.SeatingPlan;
import com.manthan.campusexamscheduler.exception.ResourceNotFoundException;
import com.manthan.campusexamscheduler.repository.SeatAssignmentRepository;
import com.manthan.campusexamscheduler.repository.SeatingPlanRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SeatingPlanService {

    private static final Pattern ROLL_NUMBER = Pattern.compile("(?i)^([A-Z]\\d{1,2})-(\\d{1,3})$");
    private static final Pattern ROOM_CODE = Pattern.compile("(?i)\\b((?:LAB\\s*[- ]*\\s*)?DT\\s*[- ]*\\s*\\d{2,4})\\b");
    private static final Pattern SEMESTER = Pattern.compile("(?i)\\b(\\d{1,2})(?:st|nd|rd|th)\\s*[- ]*\\s*sem(?:ester)?\\b");
    private static final float LINE_Y_TOLERANCE = 2.2f;
    private static final float COLUMN_X_TOLERANCE = 34f;

    private final SeatingPlanRepository seatingPlanRepository;
    private final SeatAssignmentRepository seatAssignmentRepository;

    @Transactional
    public SeatingPlanImportResponse importPdf(MultipartFile file, String requestedTitle) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a non-empty seating plan PDF.");
        }
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".pdf")) {
            throw new IllegalArgumentException("Seating arrangement import currently supports PDF files.");
        }

        ParsedDocument parsed;
        try {
            parsed = parsePdf(file.getBytes());
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof IllegalArgumentException illegalArgumentException) throw illegalArgumentException;
            throw new IllegalArgumentException("The seating plan PDF could not be read. Check that it is a valid, unprotected PDF.");
        }
        if (parsed.seats().isEmpty()) {
            throw new IllegalArgumentException("No seat assignments were found. Use a text-based PDF with room headings and roll numbers such as A1-16.");
        }

        String title = requestedTitle == null || requestedTitle.isBlank() ? parsed.title() : requestedTitle.trim();
        if (title.length() > 200) throw new IllegalArgumentException("Plan name must be at most 200 characters.");
        title = title == null || title.isBlank() ? "Seating Arrangement" : title;
        String departmentName = parsed.departmentName() == null || parsed.departmentName().isBlank()
                ? "Not specified"
                : parsed.departmentName();

        int semester = parsed.semester() == null ? 0 : parsed.semester();
        Optional<SeatingPlan> existing = seatingPlanRepository.findFirstByTitleAndDepartmentNameAndSemester(
                title, departmentName, semester);
        SeatingPlan plan;
        boolean replaced = existing.isPresent();
        if (replaced) {
            plan = existing.get();
            seatAssignmentRepository.deleteAllBySeatingPlan_SeatingPlanId(plan.getSeatingPlanId());
            seatAssignmentRepository.flush();
            plan.setImportedAt(Instant.now());
        } else {
            plan = SeatingPlan.builder()
                    .title(title)
                    .departmentName(departmentName)
                    .semester(semester)
                    .importedAt(Instant.now())
                    .build();
        }
        SeatingPlan savedPlan = seatingPlanRepository.save(plan);

        List<SeatAssignment> assignments = parsed.seats().stream()
                .map(seat -> SeatAssignment.builder()
                        .seatingPlan(savedPlan)
                        .rollNumber(seat.rollNumber())
                        .section(seat.section())
                        .roomNumber(seat.roomNumber())
                        .rowNumber(seat.rowNumber())
                        .columnNumber(seat.columnNumber())
                        .sourcePage(seat.pageNumber())
                        .build())
                .toList();
        seatAssignmentRepository.saveAll(assignments);
        seatAssignmentRepository.flush();

        SeatingPlanResponse response = toPlanResponse(savedPlan);
        return SeatingPlanImportResponse.builder()
                .plan(response)
                .importedSeats(assignments.size())
                .duplicateSeatsIgnored(parsed.duplicatesIgnored())
                .replacedExistingPlan(replaced)
                .message((replaced ? "Updated" : "Imported") + " " + assignments.size() + " seat assignment(s) across "
                        + response.getRoomCount() + " room(s).")
                .build();
    }

    @Transactional(readOnly = true)
    public List<SeatingPlanResponse> getPlans() {
        return seatingPlanRepository.findAllByOrderByImportedAtDesc().stream()
                .map(this::toPlanResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SeatLookupResponse findSeat(Long planId, String rawRollNumber) {
        String rollNumber = normalizeAndValidateRollNumber(rawRollNumber);
        SeatingPlan plan = seatingPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Seating plan not found."));
        SeatAssignment assignment = seatAssignmentRepository
                .findBySeatingPlan_SeatingPlanIdAndRollNumberIgnoreCase(planId, rollNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No seat was found for roll number " + rollNumber + " in this seating plan."));
        return toSeatResponse(plan, assignment);
    }

    @Transactional
    public void deletePlan(Long planId) {
        SeatingPlan plan = seatingPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Seating plan not found."));
        seatAssignmentRepository.deleteAllBySeatingPlan_SeatingPlanId(planId);
        seatingPlanRepository.delete(plan);
    }

    private SeatingPlanResponse toPlanResponse(SeatingPlan plan) {
        return SeatingPlanResponse.builder()
                .seatingPlanId(plan.getSeatingPlanId())
                .title(plan.getTitle())
                .departmentName(plan.getDepartmentName())
                .semester(plan.getSemester() == null || plan.getSemester() == 0 ? null : plan.getSemester())
                .importedAt(plan.getImportedAt())
                .seatCount(seatAssignmentRepository.countBySeatingPlan_SeatingPlanId(plan.getSeatingPlanId()))
                .roomCount(seatAssignmentRepository.countDistinctRooms(plan.getSeatingPlanId()))
                .build();
    }

    private SeatLookupResponse toSeatResponse(SeatingPlan plan, SeatAssignment assignment) {
        return SeatLookupResponse.builder()
                .seatingPlanId(plan.getSeatingPlanId())
                .planTitle(plan.getTitle())
                .departmentName(plan.getDepartmentName())
                .semester(plan.getSemester() == null || plan.getSemester() == 0 ? null : plan.getSemester())
                .rollNumber(assignment.getRollNumber())
                .section(assignment.getSection())
                .roomNumber(assignment.getRoomNumber())
                .rowNumber(assignment.getRowNumber())
                .columnNumber(assignment.getColumnNumber())
                .sourcePage(assignment.getSourcePage())
                .build();
    }

    private String normalizeAndValidateRollNumber(String rawRollNumber) {
        String normalized = rawRollNumber == null ? "" : rawRollNumber.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        if (!ROLL_NUMBER.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Enter a roll number in the format A1-16.");
        }
        return normalized;
    }

    private ParsedDocument parsePdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            Map<Integer, List<TextPosition>> positionedPages = new TreeMap<>();
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> textPositions) {
                    if (textPositions != null && !textPositions.isEmpty()) {
                        positionedPages.computeIfAbsent(getCurrentPageNo(), ignored -> new ArrayList<>()).addAll(textPositions);
                    }
                }
            };
            stripper.setSortByPosition(true);
            stripper.getText(document);

            List<ParsedLine> allLines = new ArrayList<>();
            Map<Integer, List<ParsedLine>> pages = new TreeMap<>();
            positionedPages.forEach((pageNumber, positions) -> {
                List<ParsedLine> lines = toLines(positions);
                pages.put(pageNumber, lines);
                allLines.addAll(lines);
            });
            if (allLines.isEmpty()) {
                throw new IllegalArgumentException("This PDF contains no selectable text. Scanned/image-only PDFs need OCR before their seating chart can be imported.");
            }

            String documentText = allLines.stream().map(ParsedLine::text).reduce((left, right) -> left + "\n" + right).orElse("");
            String title = extractPlanTitle(allLines);
            String department = extractDepartment(allLines);
            Integer semester = extractSemester(documentText);
            List<ParsedSeat> seats = new ArrayList<>();
            Map<String, ParsedSeat> byRollNumber = new LinkedHashMap<>();
            int duplicatesIgnored = 0;

            for (Map.Entry<Integer, List<ParsedLine>> page : pages.entrySet()) {
                String pageText = page.getValue().stream().map(ParsedLine::text).reduce((left, right) -> left + " "+ right).orElse("");
                if (pageText.toLowerCase(Locale.ROOT).contains("master seating arrangement chart")) continue;

                String roomNumber = extractRoom(page.getValue());
                if (roomNumber == null) continue;
                List<LocatedRoll> pageRolls = page.getValue().stream()
                        .flatMap(line -> extractRolls(line).stream())
                        .toList();
                if (pageRolls.isEmpty()) continue;

                List<Float> xColumns = cluster(pageRolls.stream().map(LocatedRoll::centerX).toList(), COLUMN_X_TOLERANCE, false);
                List<Float> yRows = cluster(pageRolls.stream().map(LocatedRoll::y).toList(), LINE_Y_TOLERANCE, true);
                for (LocatedRoll roll : pageRolls) {
                    int column = nearestIndex(xColumns, roll.centerX()) + 1;
                    // PDF coordinates grow down the page; the lowest chart row is nearest the blackboard.
                    int row = nearestIndex(yRows, roll.y()) + 1;
                    ParsedSeat seat = new ParsedSeat(roll.rollNumber(), roll.section(), roomNumber, row, column, page.getKey());
                    ParsedSeat previous = byRollNumber.putIfAbsent(seat.rollNumber(), seat);
                    if (previous == null) {
                        seats.add(seat);
                    } else if (previous.rollNumber().equals(seat.rollNumber())
                            && previous.roomNumber().equals(seat.roomNumber())
                            && previous.rowNumber() == seat.rowNumber()
                            && previous.columnNumber() == seat.columnNumber()) {
                        duplicatesIgnored++;
                    } else {
                        throw new IllegalArgumentException("Roll number " + seat.rollNumber() + " appears more than once with different room or seat positions.");
                    }
                }
            }
            return new ParsedDocument(title, department, semester, seats, duplicatesIgnored);
        }
    }

    private List<ParsedLine> toLines(List<TextPosition> positions) {
        List<TextPosition> ordered = positions.stream()
                .filter(position -> position.getUnicode() != null)
                .sorted(Comparator.comparing(TextPosition::getYDirAdj).thenComparing(TextPosition::getXDirAdj))
                .toList();
        List<List<TextPosition>> grouped = new ArrayList<>();
        List<TextPosition> current = new ArrayList<>();
        float currentY = 0;
        for (TextPosition position : ordered) {
            float y = position.getYDirAdj();
            if (!current.isEmpty() && Math.abs(y - currentY) > LINE_Y_TOLERANCE) {
                grouped.add(current);
                current = new ArrayList<>();
            }
            current.add(position);
            currentY = current.stream().map(TextPosition::getYDirAdj).reduce(0f, Float::sum) / current.size();
        }
        if (!current.isEmpty()) grouped.add(current);

        List<ParsedLine> lines = new ArrayList<>();
        for (List<TextPosition> line : grouped) {
            List<LocatedWord> words = toWords(line);
            if (words.isEmpty()) continue;
            float y = (float) line.stream().mapToDouble(TextPosition::getYDirAdj).average().orElse(0);
            lines.add(new ParsedLine(y, words, words.stream().map(LocatedWord::text).reduce((left, right) -> left + " " + right).orElse("")));
        }
        return lines;
    }

    private List<LocatedWord> toWords(List<TextPosition> positions) {
        List<TextPosition> ordered = positions.stream().sorted(Comparator.comparing(TextPosition::getXDirAdj)).toList();
        List<Float> widths = ordered.stream().map(TextPosition::getWidthDirAdj).filter(width -> width > 0).sorted().toList();
        float gapTolerance = (float) Math.max(1.0, widths.isEmpty() ? 2.0 : widths.get(widths.size() / 2) * 0.4);
        List<LocatedWord> words = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        float startX = 0;
        float endX = 0;
        for (TextPosition position : ordered) {
            String unicode = position.getUnicode();
            if (unicode == null || unicode.isBlank()) {
                addWord(words, text, startX, endX);
                continue;
            }
            float x = position.getXDirAdj();
            float end = x + position.getWidthDirAdj();
            if (!text.isEmpty() && x - endX > gapTolerance) addWord(words, text, startX, endX);
            if (text.isEmpty()) startX = x;
            text.append(unicode);
            endX = end;
        }
        addWord(words, text, startX, endX);
        return words;
    }

    private void addWord(List<LocatedWord> words, StringBuilder text, float startX, float endX) {
        if (text.isEmpty()) return;
        words.add(new LocatedWord(text.toString(), startX, endX));
        text.setLength(0);
    }

    private List<LocatedRoll> extractRolls(ParsedLine line) {
        List<LocatedRoll> rolls = new ArrayList<>();
        List<LocatedWord> words = line.words();
        for (int start = 0; start < words.size(); start++) {
            for (int length = 1; length <= 4 && start + length <= words.size(); length++) {
                String candidate = words.subList(start, start + length).stream()
                        .map(LocatedWord::text).reduce(String::concat).orElse("")
                        .replaceAll("[^A-Za-z0-9-]", "").toUpperCase(Locale.ROOT);
                Matcher matcher = ROLL_NUMBER.matcher(candidate);
                if (!matcher.matches()) continue;
                float centerX = (words.get(start).startX() + words.get(start + length - 1).endX()) / 2f;
                rolls.add(new LocatedRoll(candidate, matcher.group(1).toUpperCase(Locale.ROOT), centerX, line.y()));
                start += length - 1;
                break;
            }
        }
        return rolls;
    }

    private String extractRoom(List<ParsedLine> lines) {
        for (ParsedLine line : lines) {
            String text = line.text();
            String lower = text.toLowerCase(Locale.ROOT);
            if (!lower.contains("class room") && !lower.contains("laboratory") && !lower.contains("lab")) continue;
            Matcher matcher = ROOM_CODE.matcher(text);
            if (matcher.find()) return normalizeRoom(matcher.group(1));
        }
        return null;
    }

    private String normalizeRoom(String raw) {
        String compact = raw.toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        Matcher lab = Pattern.compile("^LAB-?DT-?(\\d{2,4})$").matcher(compact);
        if (lab.matches()) return "LAB-DT-" + lab.group(1);
        Matcher classroom = Pattern.compile("^DT-?(\\d{2,4})$").matcher(compact);
        if (classroom.matches()) return "DT-" + classroom.group(1);
        return compact;
    }

    private String extractPlanTitle(List<ParsedLine> lines) {
        return lines.stream().map(ParsedLine::text)
                .filter(line -> line.toLowerCase(Locale.ROOT).contains("session")
                        && (line.toLowerCase(Locale.ROOT).contains("test") || line.toLowerCase(Locale.ROOT).contains("exam")))
                .findFirst()
                .map(line -> line.replaceAll("\\s+", " ").trim())
                .orElse("Seating Arrangement");
    }

    private String extractDepartment(List<ParsedLine> lines) {
        return lines.stream().map(ParsedLine::text)
                .filter(line -> line.toLowerCase(Locale.ROOT).contains("department of"))
                .findFirst()
                .map(line -> line.replaceAll("\\s+", " ").trim())
                .orElse("Not specified");
    }

    private Integer extractSemester(String text) {
        Matcher matcher = SEMESTER.matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private List<Float> cluster(List<Float> values, float tolerance, boolean descending) {
        List<Float> sorted = values.stream().distinct().sorted(descending ? Comparator.reverseOrder() : Comparator.naturalOrder()).toList();
        List<Float> centers = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Float value : sorted) {
            int last = centers.size() - 1;
            if (last >= 0 && Math.abs(value - centers.get(last)) <= tolerance) {
                int count = counts.get(last) + 1;
                centers.set(last, (centers.get(last) * counts.get(last) + value) / count);
                counts.set(last, count);
            } else {
                centers.add(value);
                counts.add(1);
            }
        }
        return centers;
    }

    private int nearestIndex(List<Float> centers, float value) {
        int nearest = 0;
        float distance = Float.MAX_VALUE;
        for (int index = 0; index < centers.size(); index++) {
            float candidate = Math.abs(centers.get(index) - value);
            if (candidate < distance) {
                nearest = index;
                distance = candidate;
            }
        }
        return nearest;
    }

    private record LocatedWord(String text, float startX, float endX) { }
    private record ParsedLine(float y, List<LocatedWord> words, String text) { }
    private record LocatedRoll(String rollNumber, String section, float centerX, float y) { }
    private record ParsedSeat(String rollNumber, String section, String roomNumber, int rowNumber, int columnNumber, int pageNumber) { }
    private record ParsedDocument(String title, String departmentName, Integer semester, List<ParsedSeat> seats, int duplicatesIgnored) { }
}
