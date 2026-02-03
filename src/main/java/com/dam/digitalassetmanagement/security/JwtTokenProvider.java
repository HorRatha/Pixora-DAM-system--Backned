package com.dam.digitalassetmanagement.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * ✅ CRITICAL FIX: Must include userId in token!
     * This is called during login/authentication
     */
    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        String username = userPrincipal.getUsername();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        // ✅ Extract roles from authorities
        String roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        // ✅ CRITICAL: Try to extract userId from UserDetails
        Long userId = extractUserIdFromPrincipal(userPrincipal);

        System.out.println("🔐 JWT Generation:");
        System.out.println("   Username: " + username);
        System.out.println("   UserId: " + userId);
        System.out.println("   Roles: " + roles);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)  // ✅ ADD userId HERE!
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * ✅ Alternative: Generate token with explicit userId (better for login)
     * Call this from your login endpoint when you have the user ID from database
     */
    public String generateTokenWithUserId(String username, Long userId, String roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        System.out.println("🔐 JWT Generation (with explicit userId):");
        System.out.println("   Username: " + username);
        System.out.println("   UserId: " + userId);
        System.out.println("   Roles: " + roles);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * ✅ Extract userId from JWT token (USED FOR COMMENTS!)
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            System.out.println("📋 JWT Claims in token:");
            claims.forEach((key, value) -> {
                System.out.println("   " + key + ": " + value);
            });

            if (claims.containsKey("userId")) {
                Object userIdObj = claims.get("userId");

                if (userIdObj instanceof Number) {
                    Long userId = ((Number) userIdObj).longValue();
                    System.out.println("✅ Found userId: " + userId);
                    return userId;
                } else if (userIdObj instanceof String && userIdObj != null) {
                    try {
                        Long userId = Long.parseLong((String) userIdObj);
                        System.out.println("✅ Parsed userId: " + userId);
                        return userId;
                    } catch (NumberFormatException e) {
                        System.out.println("❌ Cannot parse userId: " + userIdObj);
                    }
                }
            } else {
                System.out.println("❌ No userId claim in token!");
            }

            return null;

        } catch (Exception e) {
            System.out.println("❌ Error extracting userId: " + e.getMessage());
            return null;
        }
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public String getRolesFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("roles", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            System.out.println("❌ Token validation failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ Extract userId from UserDetails (tries multiple ways)
     */
    private Long extractUserIdFromPrincipal(UserDetails userPrincipal) {
        try {
            // Try method 1: getId()
            try {
                var method = userPrincipal.getClass().getMethod("getId");
                Object result = method.invoke(userPrincipal);
                if (result instanceof Number) {
                    return ((Number) result).longValue();
                }
            } catch (NoSuchMethodException e) {
                // Try next method
            }

            // Try method 2: getUser().getId()
            try {
                var getUserMethod = userPrincipal.getClass().getMethod("getUser");
                Object userObj = getUserMethod.invoke(userPrincipal);
                if (userObj != null) {
                    var getIdMethod = userObj.getClass().getMethod("getId");
                    Object result = getIdMethod.invoke(userObj);
                    if (result instanceof Number) {
                        return ((Number) result).longValue();
                    }
                }
            } catch (NoSuchMethodException e) {
                // Try next method
            }

            // Try method 3: username as ID
            try {
                return Long.parseLong(userPrincipal.getUsername());
            } catch (NumberFormatException e) {
                // Not numeric
            }

        } catch (Exception e) {
            System.out.println("⚠️ Could not extract userId from UserDetails: " + e.getMessage());
        }

        return null;
    }
}