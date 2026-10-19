package com.manthan.campusexamscheduler;

import com.manthan.campusexamscheduler.entity.Exam;
import com.manthan.campusexamscheduler.entity.Subject;
import com.manthan.campusexamscheduler.repository.ExamRepository;
import com.manthan.campusexamscheduler.repository.SubjectRepository;
import com.manthan.campusexamscheduler.service.TimetableImportService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private TimetableImportService serviceFor(Subject subject, List<Exam> savedExams) {
        SubjectRepository subjectRepository = proxy(SubjectRepository.class, (method, args) -> {
            if (method.equals("findBySubjectCode")) return Optional.of(subject);
            throw new UnsupportedOperationException(method);
        });
        ExamRepository examRepository = proxy(ExamRepository.class, (method, args) -> {
            if (method.equals("existsBySubject_SubjectIdAndExamDateAndExamTime")) return false;
            if (method.equals("saveAll")) {
                for (Object exam : (Iterable<?>) args[0]) savedExams.add((Exam) exam);
                return savedExams;
            }
            throw new UnsupportedOperationException(method);
        });
        return new TimetableImportService(examRepository, subjectRepository);
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
