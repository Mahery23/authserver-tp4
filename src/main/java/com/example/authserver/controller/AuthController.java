package com.example.authserver.controller;

import com.example.authserver.dto.LoginRequest;
import com.example.authserver.dto.LoginResponse;
import com.example.authserver.service.AuthService;
import com.example.authserver.service.CryptoException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour l'authentification forte.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>POST /api/auth/login — authentification par HMAC</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Authentification forte par preuve HMAC.
     *
     * <p>Le client envoie {@code {email, nonce, timestamp, hmac}} sans jamais
     * transmettre le mot de passe. Le serveur recalcule le HMAC et compare
     * en temps constant.</p>
     *
     * @param request le payload JSON de login
     * @return 200 + {accessToken, expiresAt} si valide, 401 sinon
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) throws CryptoException {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}