package com.example.authserver.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service de chiffrement/déchiffrement AES-GCM réversible.
 *
 * <h2>Algorithme</h2>
 * <ul>
 *   <li>AES-128 (clé 16 octets)</li>
 *   <li>Mode GCM (authentifié, pas de padding nécessaire)</li>
 *   <li>IV aléatoire de 12 octets (recommandé pour GCM)</li>
 *   <li>Tag d'authentification de 128 bits</li>
 *   <li>Stockage : Base64(IV + ciphertext)</li>
 * </ul>
 */
@Service
public class CryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12;
    private static final int TAG_LENGTH_BIT = 128;

    private final SecretKeySpec secretKey;

    public CryptoService(@Value("${app.smk}") String smkHex) {
        byte[] keyBytes = hexToBytes(smkHex.substring(0, 32));
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Chiffre un mot de passe en clair avec AES-GCM.
     *
     * @param plainPassword le mot de passe en clair
     * @return chaîne Base64 contenant IV (12 octets) + ciphertext + tag GCM
     */
    public String encrypt(String plainPassword) {
        try {
            byte[] iv = new byte[IV_SIZE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey,
                    new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] encrypted = cipher.doFinal(
                    plainPassword.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[IV_SIZE + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_SIZE);
            System.arraycopy(encrypted, 0, combined, IV_SIZE, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chiffrement", e);
        }
    }

    /**
     * Déchiffre un mot de passe chiffré avec AES-GCM.
     *
     * @param encryptedBase64 la valeur Base64 stockée (IV + ciphertext + tag)
     * @return le mot de passe en clair
     */
    public String decrypt(String encryptedBase64) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);

            byte[] iv = new byte[IV_SIZE];
            byte[] ciphertext = new byte[combined.length - IV_SIZE];
            System.arraycopy(combined, 0, iv, 0, IV_SIZE);
            System.arraycopy(combined, IV_SIZE, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey,
                    new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du déchiffrement", e);
        }
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}