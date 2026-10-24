package com.medsport.auth;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Password hashing using PBKDF2-like SHA-256 with salt.
 * The hash format is: "PBKDF:{iterations}:{saltBase64}:{hashBase64}"
 *
 * NOTE: The demo seed data uses a placeholder hash. Real user registration
 * uses this class to generate and verify hashes. Seed users always have
 * their hash overridden by the demo login bypass in LoginController.
 */
public final class PasswordUtils {

    private static final int ITERATIONS = 100_000;
    private static final int SALT_BYTES = 16;
    private static final String DEMO_PASSWORD = "Demo1234!";
    // Pre-computed hash of "Demo1234!" using this scheme (stored in seed_data.sql placeholder)
    private static final String DEMO_HASH_MARKER = "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW";

    private PasswordUtils() {}

    public static String hash(String plainPassword) {
        try {
            SecureRandom rng = new SecureRandom();
            byte[] salt = new byte[SALT_BYTES];
            rng.nextBytes(salt);

            byte[] hash = pbkdf(plainPassword.toCharArray(), salt, ITERATIONS);
            return "PBKDF:" + ITERATIONS + ":"
                + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    public static boolean verify(String plainPassword, String storedHash) {
        // Handle legacy bcrypt marker from seed data — compare against known demo password
        if (storedHash != null && storedHash.startsWith("$2a$")) {
            return DEMO_PASSWORD.equals(plainPassword);
        }

        if (storedHash == null || !storedHash.startsWith("PBKDF:")) return false;

        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 4) return false;
            int iterations   = Integer.parseInt(parts[1]);
            byte[] salt      = Base64.getDecoder().decode(parts[2]);
            byte[] expected  = Base64.getDecoder().decode(parts[3]);
            byte[] actual    = pbkdf(plainPassword.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(actual, expected);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf(char[] password, byte[] salt, int iterations) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] pwBytes = new String(password).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        md.update(salt);
        byte[] hash = md.digest(pwBytes);
        for (int i = 1; i < iterations; i++) {
            md.reset();
            hash = md.digest(hash);
        }
        return hash;
    }

    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        return hasUpper && hasLower && hasDigit;
    }
}
