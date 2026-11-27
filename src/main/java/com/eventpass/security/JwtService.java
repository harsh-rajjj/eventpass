package com.eventpass.security;

import com.eventpass.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

     private final SecretKey signingKey;
     private final JwtProperties jwtProperties;

     public JwtService(JwtProperties jwtProperties) {
          this.jwtProperties = jwtProperties;
          this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
     }

     public String generateToken(String email, Role role) {
          Instant now = Instant.now();
          Instant expiry = now.plus(jwtProperties.expiration());

          return Jwts.builder()
                    .subject(email)
                    .claim("role", role.name())
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(expiry))
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
