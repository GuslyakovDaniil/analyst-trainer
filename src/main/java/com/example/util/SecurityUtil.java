package com.example.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.mindrot.jbcrypt.BCrypt;
import javax.crypto.SecretKey;
import java.util.Date;

public class SecurityUtil {
    // В реальном проекте вынести в переменные окружения
    private static final String SECRET = "your-super-secret-key-must-be-very-long-and-secure-12345";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean checkPassword(String password, String hashed) {
        return org.mindrot.jbcrypt.BCrypt.checkpw(password, hashed);
    }

    public static String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000)) // 1 день
                .signWith(KEY)
                .compact();
    }

    public static String validateToken(String token) {
        try {
            return Jwts.parser().verifyWith(KEY).build()
                    .parseSignedClaims(token).getPayload().getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}
