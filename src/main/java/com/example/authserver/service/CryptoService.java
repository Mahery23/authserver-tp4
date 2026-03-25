package com.example.authserver.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service de chiffrement/déchiffrement AES réversible.
 *
 * <h2>Usage pédagogique</h2>
 * <p>Ce service chiffre les mots de passe en AES-128/CBC avec la Server Master Key (SMK).
 * Cela permet au serveur de récupérer le mot de passe en clair pour recalculer le HMAC.</p>
 *
 * <h2>Algorithme</h2>
 * <ul>
 *   <li>AES-128 (clé 16 octets)</li>
 *   <li>Mode CBC avec IV aléatoire de 16 octets</li>
 *   <li>Padding PKCS5</li>
 *   <li>Stockage : Base64(IV + ciphertext)</li>
 * </ul>
 *
 * <h2>Limites de sécurité</h2>
 * <p>Si la SMK ({@code app.smk}) est compromise ou volée, tous les mots de passe
 * stockés en base peuvent être déchiffrés. En production, on préférerait un hash
 * non réversible (argon2id) et un protocole SRP.</p>
 */
@Service
public class CryptoService {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16;

    private final SecretKeySpec secretKey;

    /**
     * Construit le service avec la Server Master Key depuis la configuration.
     *
     * @param smkHex la clé hexadécimale de 32 caractères (128 bits) depuis {@code app.smk}
     */
    public CryptoService(@Value("${app.smk}") String smkHex) {
        byte[] keyBytes = hexToBytes(smkHex.substring(0, 32));
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Chiffre un mot de passe en clair.
     *
     * @param plainPassword le mot de passe en clair
     * @return chaîne Base64 contenant IV (16 octets) + ciphertext
     */
    public String encrypt(String plainPassword) {
        try {
            byte[] iv = new byte[IV_SIZE];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encrypted = cipher.doFinal(plainPassword.getBytes(StandardCharsets.UTF_8));

            // Stocker IV + ciphertext ensemble
            byte[] combined = new byte[IV_SIZE + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_SIZE);
            System.arraycopy(encrypted, 0, combined, IV_SIZE, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chiffrement", e);
        }
    }

    /**
     * Déchiffre un mot de passe chiffré stocké en base.
     *
     * @param encryptedBase64 la valeur Base64 stockée en base (IV + ciphertext)
     * @return le mot de passe en clair
     */
    public String decrypt(String encryptedBase64) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);

            byte[] iv = new byte[IV_SIZE];
            byte[] ciphertext = new byte[combined.length - IV_SIZE];
            System.arraycopy(combined, 0, iv, 0, IV_SIZE);
            System.arraycopy(combined, IV_SIZE, ciphertext, 0, ciphertext.length);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du déchiffrement", e);
        }
    }

    /**
     * Convertit une chaîne hexadécimale en tableau d'octets.
     *
     * @param hex chaîne hexadécimale
     * @return tableau d'octets
     */
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
