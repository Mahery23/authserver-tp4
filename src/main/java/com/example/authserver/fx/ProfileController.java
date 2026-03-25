package com.example.authserver.fx;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Contrôleur de l'écran profil.
 * Affiche les informations de l'utilisateur connecté et son token JWT.
 */
public class ProfileController {

    @FXML private Label welcomeLabel;
    @FXML private Label emailLabel;
    @FXML private TextArea tokenArea;
    @FXML private Label expiresLabel;
    @FXML private Label copyLabel;
    @FXML private Label statusLabel;

    private String token;

    /**
     * Initialise l'écran profil avec les données reçues après login.
     */
    public void setData(String email, String token, long expiresAt) {
        this.token = token;

        welcomeLabel.setText("Bienvenue !");
        emailLabel.setText("Connecté en tant que : " + email);
        tokenArea.setText(token);

        LocalDateTime expiry = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(expiresAt), ZoneId.systemDefault());
        String formatted = expiry.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        expiresLabel.setText("⏱ Token valide jusqu'à : " + formatted);
    }

    @FXML
    public void copyToken() {
        ClipboardContent content = new ClipboardContent();
        content.putString(token);
        Clipboard.getSystemClipboard().setContent(content);
        copyLabel.setText("✅ Copié !");
        copyLabel.setStyle("-fx-text-fill: #27ae60;");

        new Thread(() -> {
            try {
                Thread.sleep(2000);
                javafx.application.Platform.runLater(() -> {
                    copyLabel.setText("");
                });
            } catch (InterruptedException ignored) {}
        }).start();
    }

    @FXML
    public void handleLogout() {
        try {
            MainApp.showLogin();
        } catch (Exception e) {
            statusLabel.setText("Erreur de déconnexion.");
        }
    }
}
