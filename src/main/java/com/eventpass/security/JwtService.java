package com.eventpass.security;
import com.eventpass.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

     private final SecretKey signingKey = Keys.hmacShaKeyFor(SecurityConstants.JWT_SECRET.getBytes(StandardCharsets.UTF_8));

     public String generateToken(String email, Role role) {
          Date now = new Date();
          Date expiry = new Date(now.getTime() + SecurityConstants.JWT_EXPIRATION_MS);

          return Jwts.builder()
                    .subject(email)
                    .claim("role", role.name())
                    .issuedAt(now)
                    .expiration(expiry)
                    .signWith(signingKey)
                    .compact();
     }

     public Claims parseClaims(String token) {
          return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
     }
}
