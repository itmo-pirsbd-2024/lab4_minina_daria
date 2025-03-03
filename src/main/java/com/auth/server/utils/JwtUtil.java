package com.auth.server.utils;

import io.jsonwebtoken.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;

public class JwtUtil {
    private static final String SECRET = Config.get("jwt.secret");
    private static final long ACCESS_EXPIRATION = Config.getLong("jwt.access.expiration");
    private static final long REFRESH_EXPIRATION = Config.getLong("jwt.refresh.expiration");

    public static String generateAccessToken(String username, String role) {
        return buildToken(username, role, ACCESS_EXPIRATION);
    }

    public static String generateRefreshToken(String username) {
        return buildToken(username, "REFRESH", REFRESH_EXPIRATION);
    }

    private static String buildToken(String subject, String role, long expiration) {
        Key key = new SecretKeySpec(SECRET.getBytes(), SignatureAlgorithm.HS256.getJcaName());

        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public static boolean validateToken(String token, String expectedRole) {
        try {
            Key key = new SecretKeySpec(SECRET.getBytes(), SignatureAlgorithm.HS256.getJcaName());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.get("role").equals(expectedRole) &&
                    claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}