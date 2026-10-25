package com.manthan.campusexamscheduler;

import com.manthan.campusexamscheduler.entity.Exam;
import com.manthan.campusexamscheduler.entity.Subject;
import com.manthan.campusexamscheduler.repository.ExamRepository;
import com.manthan.campusexamscheduler.repository.SubjectRepository;
import com.manthan.campusexamscheduler.service.TimetableImportService;
import com.manthan.campusexamscheduler.service.ScheduleConflictService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimetableImportServiceTest {

    static {
        try {
            var fontCache = java.nio.file.Path.of("target", "pdfbox-font-cache").toAbsolutePath();
            java.nio.file.Files.createDirectories(fontCache);
            System.setProperty("pdfbox.fontcache", fontCache.toString());
        } catch (java.io.IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @Test
    void importsExcelDatesAndTimesWithCustomCellFormatsAndLeadingTitle() throws Exception {
        Subject subject = Subject.builder()
                .subjectId(7L)
                .subjectCode("CS101")
                .build();
        List<Exam> savedExams = new ArrayList<>();
        TimetableImportService timetableImportService = serviceFor(subject, savedExams);

        byte[] workbookBytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Exams");
            sheet.createRow(0).createCell(0).setCellValue("RBU Examination Schedule");

            var header = sheet.createRow(1);
            String[] headers = { "Subject Code", "Date", "Time", "Building", "Room" };
            for (int index = 0; index < headers.length; index++) {
                header.createCell(index).setCellValue(headers[index]);
            }

            var row = sheet.createRow(2);
            row.createCell(0).setCellValue("CS101");
            var dateCell = row.createCell(1);
            dateCell.setCellValue(LocalDate.of(2026, 11, 12));
            var timeCell = row.createCell(2);
            timeCell.setCellValue(0.375);
            DataFormat dataFormat = workbook.createDataFormat();
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(dataFormat.getFormat("dd-mmm-yyyy"));
            dateCell.setCellStyle(dateStyle);
            CellStyle timeStyle = workbook.createCellStyle();
            timeStyle.setDataFormat(dataFormat.getFormat("h:mm AM/PM"));
            timeCell.setCellStyle(timeStyle);
            row.createCell(3).setCellValue("Main Building");
            row.createCell(4).setCellValue("A-205");

            workbook.write(output);
            workbookBytes = output.toByteArray();
        }

        var file = new MockMultipartFile(
                "file", "timetable.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes);

        var response = timetableImportService.importTimetable(file);

        assertEquals(1, response.getImported());
        assertEquals(LocalDate.of(2026, 11, 12), savedExams.get(0).getExamDate());
        assertEquals(LocalTime.of(9, 0), savedExams.get(0).getExamTime());
        assertEquals("Main Building", savedExams.get(0).getBuilding());
        assertEquals("A-205", savedExams.get(0).getRoomNumber());
    }

    @Test
    void importsTextPdfWithAlignedWhitespaceColumnsAndTitle() throws Exception {
        Subject subject = Subject.builder()
                .subjectId(7L)
                .subjectCode("CS101")
                .build();
        List<Exam> savedExams = new ArrayList<>();
        TimetableImportService timetableImportService = serviceFor(subject, savedExams);

        byte[] pdfBytes;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            try (PDPageContentStream content = new PDPageContentStream(document, document.getPage(0))) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText("RBU exam timetable");
                content.newLineAtOffset(0, -20);
                content.showText("Subject Code  Date  Time  Building  Room");
                content.newLineAtOffset(0, -20);
                content.showText("CS101  2026-11-12  09:00  Main Building  A-205");
                content.endText();
            }
            document.save(output);
            pdfBytes = output.toByteArray();
        }

        var file = new MockMultipartFile("file", "timetable.pdf", "application/pdf", pdfBytes);
        var response = timetableImportService.importTimetable(file);

        assertEquals(1, response.getImported());
        assertEquals("Main Building", savedExams.get(0).getBuilding());
        assertEquals("A-205", savedExams.get(0).getRoomNumber());
    }

    @Test
    void importsPdfWithPositionedTableColumnsAndHumanReadableDateAndTime() throws Exception {
        Subject subject = Subject.builder().subjectId(7L).subjectCode("CS101").build();
        List<Exam> savedExams = new ArrayList<>();
        TimetableImportService timetableImportService = serviceFor(subject, savedExams);

        byte[] pdfBytes;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            try (PDPageContentStream content = new PDPageContentStream(document, document.getPage(0))) {
                writePositionedLine(content, 700, new String[] { "Exam Timetable" }, new float[] { 50 });
                writePositionedLine(content, 670,
                        new String[] { "Subject Code", "Exam Date", "Start Time", "Building", "Room No" },
                        new float[] { 50, 180, 290, 390, 520 });
                writePositionedLine(content, 640,
                        new String[] { "CS101", "12 November 2026", "9 AM", "Main Building", "A-205" },
                        new float[] { 50, 180, 290, 390, 520 });
            }
            PDPage secondPage = new PDPage();
            document.addPage(secondPage);
            try (PDPageContentStream content = new PDPageContentStream(document, secondPage)) {
                writePositionedLine(content, 700,
                        new String[] { "Subject Code", "Exam Date", "Start Time", "Building", "Room No" },
                        new float[] { 50, 180, 290, 390, 520 });
                writePositionedLine(content, 670,
                        new String[] { "CS101", "12 November 2026", "9 AM", "Main Building", "A-205" },
                        new float[] { 50, 180, 290, 390, 520 });
            }
            document.save(output);
            pdfBytes = output.toByteArray();
        }

        var response = timetableImportService.importTimetable(
                new MockMultipartFile("file", "positioned.pdf", "application/pdf", pdfBytes));

        assertEquals(1, response.getImported());
        assertEquals(1, response.getSkippedDuplicates());
        assertEquals(LocalDate.of(2026, 11, 12), savedExams.get(0).getExamDate());
        assertEquals(LocalTime.of(9, 0), savedExams.get(0).getExamTime());
        assertEquals("Main Building", savedExams.get(0).getBuilding());
        assertEquals("A-205", savedExams.get(0).getRoomNumber());
    }

    @Test
    void importsPipeDelimitedPdfWithCommaDateAndAmPmTimeRange() throws Exception {
        Subject subject = Subject.builder().subjectId(7L).subjectCode("CS101").build();
        List<Exam> savedExams = new ArrayList<>();
        TimetableImportService timetableImportService = serviceFor(subject, savedExams);

        byte[] pdfBytes;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            try (PDPageContentStream content = new PDPageContentStream(document, document.getPage(0))) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText("Subject ID | Date of Exam | Timing | Exam Centre | Classroom");
                content.newLineAtOffset(0, -20);
                content.showText("CS101 | November 12, 2026 | 9.00 a.m. to 12.00 p.m. | Main Building | A-205");
                content.endText();
            }
            document.save(output);
            pdfBytes = output.toByteArray();
        }

        var response = timetableImportService.importTimetable(
                new MockMultipartFile("file", "comma-date.pdf", "application/pdf", pdfBytes));

        assertEquals(1, response.getImported());
        assertEquals(LocalDate.of(2026, 11, 12), savedExams.get(0).getExamDate());
        assertEquals(LocalTime.of(9, 0), savedExams.get(0).getExamTime());
        assertEquals("Main Building", savedExams.get(0).getBuilding());
        assertEquals("A-205", savedExams.get(0).getRoomNumber());
    }

    @Test
    void importsDateByCourseMatrixPdfAcrossTwoExamSessions() throws Exception {
        Map<String, Subject> subjects = Map.of(
                "24CS01TH0501", Subject.builder().subjectId(7L).subjectCode("24CS01TH0501").build(),
                "24CS01TH0502", Subject.builder().subjectId(8L).subjectCode("24CS01TH0502").build(),
                "24EE01TH0501", Subject.builder().subjectId(9L).subjectCode("24EE01TH0501").build());
        List<Exam> savedExams = new ArrayList<>();
        SubjectRepository subjectRepository = proxy(SubjectRepository.class, (method, args) -> {
            if (method.equals("findBySubjectCode")) return Optional.ofNullable(subjects.get(args[0]));
            throw new UnsupportedOperationException(method);
        });
        ExamRepository examRepository = proxy(ExamRepository.class, (method, args) -> {
            if (method.equals("existsBySubject_SubjectIdAndExamDateAndExamTime")) return false;
            if (method.equals("findAllWithSubject")) return new ArrayList<>(savedExams);
            if (method.equals("saveAll")) {
                for (Object exam : (Iterable<?>) args[0]) savedExams.add((Exam) exam);
                return savedExams;
            }
            throw new UnsupportedOperationException(method);
        });
        TimetableImportService timetableImportService = new TimetableImportService(
                examRepository, subjectRepository, new ScheduleConflictService());

        byte[] pdfBytes;
        String[] dates = { "18-Nov-26", "20-Nov-26", "23-Nov-26", "25-Nov-26", "27-Nov-26", "30-Nov-26", "02-Dec-26" };
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(new PDRectangle(1190, 842));
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                writeAt(content, 370, 755, "Time : 02:00 pm to 05:00 pm");
                writeMatrixHeaders(content, dates, 735, 724);
                writeAt(content, 90, 700, "24CS01TH0501*");
                writeAt(content, 158.2f, 700, "COMPUTER1");
                writeAt(content, 197, 685, "24CS01TH0502");
                writeAt(content, 90, 420, "Time : 09:30 am to 12:30 pm");
                writeMatrixHeaders(content, dates, 400, 389);
                writeAt(content, 90, 365, "24EE01TH0501");
            }
            // A second copy of the first course under the same date and session is a duplicate.
            try (PDPageContentStream content = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true)) {
                writeAt(content, 90, 675, "24CS01TH0501*");
            }
            document.save(output);
            pdfBytes = output.toByteArray();
        }

        var response = timetableImportService.importTimetable(
                new MockMultipartFile("file", "matrix-timetable.pdf", "application/pdf", pdfBytes));

        assertEquals(3, response.getImported());
        assertEquals(1, response.getSkippedDuplicates());
        assertEquals(LocalDate.of(2026, 11, 18), savedExams.get(0).getExamDate());
        assertEquals(LocalTime.of(14, 0), savedExams.get(0).getExamTime());
        assertEquals(LocalDate.of(2026, 11, 20), savedExams.get(1).getExamDate());
        assertEquals(LocalDate.of(2026, 11, 18), savedExams.get(2).getExamDate());
        assertEquals(LocalTime.of(9, 30), savedExams.get(2).getExamTime());
        assertEquals("Not assigned", savedExams.get(0).getBuilding());
        assertEquals("Not assigned", savedExams.get(0).getRoomNumber());
    }

    @Test
    void explainsThatScannedPdfNeedsOcrInsteadOfReturningAnEmptyTableError() throws Exception {
        Subject subject = Subject.builder().subjectId(7L).subjectCode("CS101").build();
        TimetableImportService timetableImportService = serviceFor(subject, new ArrayList<>());

        byte[] pdfBytes;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            pdfBytes = output.toByteArray();
        }

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> timetableImportService.importTimetable(
                        new MockMultipartFile("file", "scanned.pdf", "application/pdf", pdfBytes)));

        assertTrue(error.getMessage().contains("no selectable text"));
        assertTrue(error.getMessage().contains("OCR"));
    }

    private void writePositionedLine(PDPageContentStream content, float y, String[] values, float[] xPositions) throws Exception {
        for (int index = 0; index < values.length; index++) {
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            content.newLineAtOffset(xPositions[index], y);
            content.showText(values[index]);
            content.endText();
        }
    }

    private void writeMatrixHeaders(PDPageContentStream content, String[] dates, float dateY, float courseHeaderY) throws Exception {
        float[] codeColumns = { 105.2f, 212.3f, 319.3f, 426.3f, 533.4f, 640.4f, 747.5f };
        float[] dateColumns = { 134.4f, 241.5f, 348.5f, 455.5f, 562.6f, 669.6f, 776.9f };
        for (int index = 0; index < dates.length; index++) {
            writeAt(content, dateColumns[index], dateY, dates[index]);
            writeAt(content, codeColumns[index], courseHeaderY, "Course Code");
            writeAt(content, codeColumns[index] + 53, courseHeaderY, "Course Name");
        }
    }

    private void writeAt(PDPageContentStream content, float x, float y, String value) throws Exception {
        content.beginText();
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
        content.newLineAtOffset(x, y);
        content.showText(value);
        content.endText();
    }

    private TimetableImportService serviceFor(Subject subject, List<Exam> savedExams) {
        SubjectRepository subjectRepository = proxy(SubjectRepository.class, (method, args) -> {
            if (method.equals("findBySubjectCode")) return Optional.of(subject);
            throw new UnsupportedOperationException(method);
        });
        ExamRepository examRepository = proxy(ExamRepository.class, (method, args) -> {
            if (method.equals("existsBySubject_SubjectIdAndExamDateAndExamTime")) return false;
            if (method.equals("findAllWithSubject")) return new ArrayList<>(savedExams);
            if (method.equals("saveAll")) {
                for (Object exam : (Iterable<?>) args[0]) savedExams.add((Exam) exam);
                return savedExams;
            }
            throw new UnsupportedOperationException(method);
        });
        return new TimetableImportService(examRepository, subjectRepository, new ScheduleConflictService());
    }

    private static <T> T proxy(Class<T> type, RepositoryCall call) {
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (instance, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> type.getSimpleName() + " test double";
                    case "hashCode" -> System.identityHashCode(instance);
                    case "equals" -> instance == args[0];
                    default -> null;
                };
            }
            return call.invoke(method.getName(), args == null ? new Object[0] : args);
        });
        return type.cast(proxy);
    }

    @FunctionalInterface
    private interface RepositoryCall {
        Object invoke(String method, Object[] args);
    }
}
