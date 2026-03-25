package com.example.authserver;

import com.example.authserver.service.CryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour {@link CryptoService}.
 * Couvre : chiffrement AES réversible, déchiffrement, unicité IV.
 */
class CryptoServiceTest {

    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new CryptoService("0123456789abcdef0123456789abcdef");
    }

    @Test
    @DisplayName("encrypt() retourne une valeur non nulle différente du mot de passe en clair")
    void encrypt_returnsCiphertext() {
        String encrypted = cryptoService.encrypt("mypassword");
        assertThat(encrypted).isNotNull().isNotEqualTo("mypassword");
    }

    @Test
    @DisplayName("decrypt(encrypt(x)) == x — chiffrement réversible")
    void encryptDecrypt_roundTrip() {
        String original = "SuperSecret123!";
        String encrypted = cryptoService.encrypt(original);
        String decrypted = cryptoService.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    @DisplayName("Deux chiffrements du même mot de passe produisent des résultats différents (IV aléatoire)")
    void encrypt_producesDifferentCiphertexts() {
        String e1 = cryptoService.encrypt("samepassword");
        String e2 = cryptoService.encrypt("samepassword");
        // Les IV sont aléatoires donc les ciphertexts doivent être différents
        assertThat(e1).isNotEqualTo(e2);
        // Mais les deux déchiffrent vers le même texte
        assertThat(cryptoService.decrypt(e1)).isEqualTo("samepassword");
        assertThat(cryptoService.decrypt(e2)).isEqualTo("samepassword");
    }

    @Test
    @DisplayName("decrypt() fonctionne avec des caractères spéciaux")
    void encryptDecrypt_withSpecialChars() {
        String password = "p@$$w0rd!éàü";
        assertThat(cryptoService.decrypt(cryptoService.encrypt(password))).isEqualTo(password);
    }
}
