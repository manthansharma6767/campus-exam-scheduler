package com.manthan.campusexamscheduler.service;

import com.manthan.campusexamscheduler.dto.StudentVerificationResponse;
import com.manthan.campusexamscheduler.entity.Student;
import com.manthan.campusexamscheduler.exception.InvalidCredentialsException;
import com.manthan.campusexamscheduler.repository.StudentRepository;
import com.manthan.campusexamscheduler.util.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentVerificationService {

    private final StudentRepository studentRepository;
    private final JwtService jwtService;
    private final RateLimitService rateLimitService;

    @Transactional(readOnly = true)
    public StudentVerificationResponse verify(String enrollmentNumber, LocalDate dateOfBirth) {
        String normalizedEnrollment = normalizeEnrollment(enrollmentNumber);
        String rateLimitKey = verificationKey(normalizedEnrollment);
        rateLimitService.check(rateLimitKey);

        Optional<Student> student = studentRepository
                .findByEnrollmentNumberIgnoreCase(normalizedEnrollment);
        boolean verified = student.isPresent()
                && student.get().getDateOfBirth() != null
                && student.get().getDateOfBirth().equals(dateOfBirth);

        if (!verified) {
            rateLimitService.recordFailure(rateLimitKey);
            // Keep the message generic so enrollment numbers cannot be probed.
            throw new InvalidCredentialsException(
                    "Enrollment number or date of birth is incorrect.");
        }

        rateLimitService.clear(rateLimitKey);
        String enrolledNumber = student.get().getEnrollmentNumber();
        return StudentVerificationResponse.builder()
                .token(jwtService.generateStudentVerificationToken(enrolledNumber))
                .expiresInSeconds(jwtService.getStudentVerificationExpirySeconds())
                .build();
    }

    /**
     * Validates the short-lived header token and returns the verified enrollment.
     */
    public String requireVerifiedEnrollment(String enrollmentNumber, String token) {
        try {
            String tokenEnrollment = jwtService.extractStudentVerificationEnrollment(token);
            String requestedEnrollment = normalizeEnrollment(enrollmentNumber);
            if (tokenEnrollment == null
                    || !tokenEnrollment.equalsIgnoreCase(requestedEnrollment)) {
                throw new InvalidCredentialsException(
                        "Your verification does not match this enrollment number.");
            }
            return tokenEnrollment;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidCredentialsException(
                    "Your verification has expired. Verify your enrollment number and date of birth again.");
        }
    }

    private String normalizeEnrollment(String enrollmentNumber) {
        return enrollmentNumber == null
                ? ""
                : enrollmentNumber.trim().toUpperCase(Locale.ROOT);
    }

    private String verificationKey(String enrollmentNumber) {
        return "student-verify:" + enrollmentNumber;
    }
}
