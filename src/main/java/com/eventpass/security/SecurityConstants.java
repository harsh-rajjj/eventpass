package com.eventpass.security;

public final class SecurityConstants {
     private SecurityConstants() {}

     public static final String JWT_SECRET = "eventpass-dev-only-secret-key-must-be-at-least-32-bytes-long";

     public static final long JWT_EXPIRATION_MS = 3_600_000;
}
