package com.manthan.campusexamscheduler.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String STUDENT_VERIFICATION_TOKEN_TYPE = "student-verification";

    // Admin tokens are valid for 24 hours.
    private static final long EXPIRY_MS = 1000L * 60 * 60 * 24;

    // Student self-service verification is intentionally short lived.
    private static final long STUDENT_VERIFICATION_EXPIRY_MS = 1000L * 60 * 10;

    @Value("${JWT_SECRET}")
    private String secretKey;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                secretKey.getBytes(StandardCharsets.UTF_8)
        );
    }

    // Generate admin access JWT
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRY_MS))
                .signWith(getSigningKey())
                .compact();
    }

    // Generate a short-lived token proving enrollment + DOB ownership.
    public String generateStudentVerificationToken(String enrollmentNumber) {
        return Jwts.builder()
                .subject(enrollmentNumber)
                .claim(TOKEN_TYPE_CLAIM, STUDENT_VERIFICATION_TOKEN_TYPE)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + STUDENT_VERIFICATION_EXPIRY_MS))
                .signWith(getSigningKey())
                .compact();
    }

    public long getStudentVerificationExpirySeconds() {
        return STUDENT_VERIFICATION_EXPIRY_MS / 1000;
    }

    // Extract Username
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract Expiration
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Generic Claim Extractor
    public <T> T extractClaim(
            String token,
            Function<Claims, T> resolver) {

        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return resolver.apply(claims);
    }

    // Check Expiration
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Validate admin access token. Tokens issued before token types were added
    // are still treated as access tokens so existing sessions keep working.
    public boolean isTokenValid(String token, String email) {
        return extractUsername(token).equals(email)
                && !isTokenExpired(token)
                && isAccessToken(token);
    }

    private boolean isAccessToken(String token) {
        String tokenType = extractClaim(
                token,
                claims -> claims.get(TOKEN_TYPE_CLAIM, String.class)
        );
        return tokenType == null || ACCESS_TOKEN_TYPE.equals(tokenType);
    }

    /**
     * Returns the verified enrollment number from a student verification token.
     * Throws {@link JwtException} for malformed, expired, or wrong-purpose tokens.
     */
    public String extractStudentVerificationEnrollment(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!STUDENT_VERIFICATION_TOKEN_TYPE.equals(tokenType)) {
            throw new JwtException("Invalid token type");
        }
        return claims.getSubject();
    }
}
