package com.manthan.campusexamscheduler.service;

import com.manthan.campusexamscheduler.dto.StudentImportResponse;
import com.manthan.campusexamscheduler.entity.Department;
import com.manthan.campusexamscheduler.entity.Student;
import com.manthan.campusexamscheduler.repository.DepartmentRepository;
import com.manthan.campusexamscheduler.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StudentImportService {

    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "name", "email", "password", "enrollmentNumber", "semester");
    private static final int HEADER_SCAN_LIMIT = 20;

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public StudentImportResponse importStudents(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a non-empty student file.");
        }

        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        List<String[]> rows;
        try {
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                rows = readSpreadsheet(file);
            } else if (filename.endsWith(".csv")) {
                rows = readCsv(file);
            } else {
                throw new IllegalArgumentException("Supported student files are CSV, XLS, and XLSX.");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("The student file could not be read. Check that it is a valid, unprotected CSV or Excel file.");
        }

        int headerRow = findHeaderRow(rows);
        if (headerRow < 0 || rows.size() <= headerRow + 1) {
            throw new IllegalArgumentException("The student file must have a header row and at least one student row.");
        }
        Map<String, Integer> columns = headerColumns(rows.get(headerRow));
        List<Student> students = new ArrayList<>();
        Set<String> fileEmails = new HashSet<>();
        Set<String> fileEnrollmentNumbers = new HashSet<>();
        int skippedDuplicates = 0;

        for (int index = headerRow + 1; index < rows.size(); index++) {
            String[] row = rows.get(index);
            if (Arrays.stream(row).allMatch(value -> value == null || value.isBlank())) continue;
            int rowNumber = index + 1;

            try {
                String name = cell(row, columns, "name").trim();
                String email = cell(row, columns, "email").trim().toLowerCase(Locale.ROOT);
                String password = cell(row, columns, "password");
                String enrollmentNumber = cell(row, columns, "enrollmentNumber").trim().toUpperCase(Locale.ROOT);
                String semesterText = cell(row, columns, "semester").trim();

                validateStudentFields(name, email, password, enrollmentNumber, semesterText);
                Department department = resolveDepartment(row, columns);
                int semester = Integer.parseInt(semesterText);

                boolean duplicate = studentRepository.existsByEmail(email)
                        || studentRepository.existsByEnrollmentNumber(enrollmentNumber)
                        || fileEmails.contains(email)
                        || fileEnrollmentNumbers.contains(enrollmentNumber);
                if (duplicate) {
                    skippedDuplicates++;
                    continue;
                }
                fileEmails.add(email);
                fileEnrollmentNumbers.add(enrollmentNumber);

                students.add(Student.builder()
                        .name(name)
                        .email(email)
                        .password(passwordEncoder.encode(password))
                        .enrollmentNumber(enrollmentNumber)
                        .semester(semester)
                        .department(department)
                        .build());
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Row " + rowNumber + ": " + exception.getMessage());
            }
        }

        if (students.isEmpty() && skippedDuplicates == 0) {
            throw new IllegalArgumentException("No student rows were found in the file.");
        }

        studentRepository.saveAll(students);
        String message = "Imported " + students.size() + " student(s); skipped " + skippedDuplicates + " duplicate(s).";
        return StudentImportResponse.builder()
                .imported(students.size())
                .skippedDuplicates(skippedDuplicates)
                .message(message)
                .build();
    }

    private void validateStudentFields(String name, String email, String password,
                                       String enrollmentNumber, String semesterText) {
        if (name.length() < 2 || name.length() > 100) {
            throw new IllegalArgumentException("name must be between 2 and 100 characters");
        }
        if (email.length() > 100 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("enter a valid email address (maximum 100 characters)");
        }
        if (password.length() < 8 || password.length() > 72) {
            throw new IllegalArgumentException("password must be between 8 and 72 characters");
        }
        if (enrollmentNumber.isBlank() || enrollmentNumber.length() > 255) {
            throw new IllegalArgumentException("roll/enrollment number is required and must be at most 255 characters");
        }
        int semester;
        try {
            semester = Integer.parseInt(semesterText);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("semester must be a number from 1 to 8");
        }
        if (semester < 1 || semester > 8) {
            throw new IllegalArgumentException("semester must be a number from 1 to 8");
        }
    }

    private Department resolveDepartment(String[] row, Map<String, Integer> columns) {
        String departmentId = cell(row, columns, "departmentId").trim();
        if (!departmentId.isBlank()) {
            try {
                return departmentRepository.findById(Long.parseLong(departmentId))
                        .orElseThrow(() -> new IllegalArgumentException("department ID " + departmentId + " does not exist"));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("department ID must be numeric");
            }
        }

        String departmentName = cell(row, columns, "departmentName").trim();
        if (departmentName.isBlank()) {
            throw new IllegalArgumentException("department name or department ID is required");
        }
        return departmentRepository.findByDepartmentNameIgnoreCase(departmentName)
                .orElseThrow(() -> new IllegalArgumentException("department '" + departmentName + "' does not exist"));
    }

    private List<String[]> readSpreadsheet(MultipartFile file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) return rows;
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                int width = Math.max(row.getLastCellNum(), 0);
                String[] values = new String[width];
                for (int column = 0; column < width; column++) {
                    Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    values[column] = cell == null ? "" : formatter.formatCellValue(cell).trim();
                }
                rows.add(values);
            }
        }
        return rows;
    }

    private List<String[]> readCsv(MultipartFile file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int rowNumber = 0;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (rowNumber == 1 && line.startsWith("\uFEFF")) line = line.substring(1);
                try {
                    rows.add(parseCsvLine(line));
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException("Row " + rowNumber + ": " + exception.getMessage());
                }
            }
        }
        return rows;
    }

    private String[] parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                cells.add(value.toString().trim());
                value.setLength(0);
            } else {
                value.append(character);
            }
        }
        if (quoted) throw new IllegalArgumentException("CSV row has an unmatched quote.");
        cells.add(value.toString().trim());
        return cells.toArray(String[]::new);
    }

    private int findHeaderRow(List<String[]> rows) {
        for (int index = 0; index < Math.min(rows.size(), HEADER_SCAN_LIMIT); index++) {
            try {
                headerColumns(rows.get(index));
                return index;
            } catch (IllegalArgumentException ignored) {
                // Allow a title or note before the column headers.
            }
        }
        return -1;
    }

    private Map<String, Integer> headerColumns(String[] header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int index = 0; index < header.length; index++) {
            String normalized = header[index].toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
            String key = switch (normalized) {
                case "name", "fullname", "studentname" -> "name";
                case "email", "emailaddress" -> "email";
                case "password", "initialpassword" -> "password";
                case "roll", "rollno", "rollnumber", "enrollment", "enrollmentno", "enrollmentnumber", "registrationnumber", "registrationno" -> "enrollmentNumber";
                case "semester", "sem" -> "semester";
                case "department", "departmentname", "dept", "deptname" -> "departmentName";
                case "departmentid", "deptid" -> "departmentId";
                default -> null;
            };
            if (key != null) columns.put(key, index);
        }
        if (!columns.keySet().containsAll(REQUIRED_HEADERS)
                || (!columns.containsKey("departmentName") && !columns.containsKey("departmentId"))) {
            throw new IllegalArgumentException("Required columns: Name, Email, Password, Roll/Enrollment Number, Semester, and Department (name or ID).");
        }
        return columns;
    }

    private String cell(String[] row, Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        return index == null || index >= row.length ? "" : Objects.toString(row[index], "");
    }
}
