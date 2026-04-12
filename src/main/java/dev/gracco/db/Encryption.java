package dev.gracco.db;

import dev.gracco.ui.Alert;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

public class Encryption {
    /**
     * Generates a secure hash of the provided password using PBKDF2 with HmacSHA256.
     *
     * A random 16-byte salt is generated for each password, and the hash is derived
     * using 10,000 iterations with a 256-bit key length. The resulting output is
     * encoded in Base64 and returned as a single string in the format:
     *
     *     salt$hash
     *
     * where both the salt and hash are Base64-encoded.
     *
     * This method is intended for securely storing passwords, not for encryption or
     * reversible transformations.
     *
     * @param password the plain-text password to hash
     * @return a Base64-encoded string containing the salt and hash separated by "$",
     *         or null if an error occurs during hashing
     */
    public static String encrypt(String password) {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        int iterations = 10000;
        int keyLength = 256;

        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
        SecretKeyFactory factory;
        byte[] hash;

        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            hash = factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            Alert.fatalError(e.getMessage());
            return null;
        }

        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = Base64.getEncoder().encodeToString(hash);
        return saltBase64 + "$" + hashBase64;
    }

    /**
     * Verifies whether a given plain-text password matches a previously stored hash.
     *
     * The stored hash must be in the format:
     *
     *     salt$hash
     *
     * where both values are Base64-encoded. The method extracts the salt, recomputes
     * the hash using the same PBKDF2 parameters (HmacSHA256, 10,000 iterations,
     * 256-bit key length), and compares it with the stored hash.
     *
     * This method does not perform decryption; it validates passwords by comparing
     * hash outputs.
     *
     * @param password the plain-text password to verify
     * @param hash the stored password hash in "salt$hash" format
     * @return true if the password matches the stored hash, false otherwise
     */
    public static boolean decrypt(String password, String hash) {
        String[] parts = hash.split("\\$");
        if (parts.length != 2) return false;

        String saltBase64 = parts[0];
        String storedPasswordHashBase64 = parts[1];

        byte[] salt = Base64.getDecoder().decode(saltBase64);
        byte[] storedPasswordHash = Base64.getDecoder().decode(storedPasswordHashBase64);

        int iterations = 10000;
        int keyLength = 256;

        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
        SecretKeyFactory factory;
        byte[] enteredPasswordHash;

        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            enteredPasswordHash = factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            Alert.fatalError(e.getMessage());
            return false;
        }
        return java.util.Arrays.equals(storedPasswordHash, enteredPasswordHash);
    }
}
