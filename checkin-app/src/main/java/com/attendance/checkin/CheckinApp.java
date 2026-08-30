package com.attendance.checkin;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CheckinApp extends Application {

    private static final String API_BASE = "http://127.0.0.1:8001";
    private final HttpClient client = HttpClient.newHttpClient();

    private TextField rollField;
    private TextField otpField;
    private Button requestOtpBtn, verifyBtn;
    private Label statusLabel;
    private String pendingRoll = null;

    @Override
    public void start(Stage stage) {
        VBox root = new VBox(18);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0f1117;");

        Label title = new Label("Attendance Check-in");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label subtitle = new Label("Enter your roll number, then verify with the OTP sent to your email.");
        subtitle.setStyle("-fx-text-fill: #9aa0b4; -fx-font-size: 12px;");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(360);
        subtitle.setAlignment(Pos.CENTER);

        rollField = new TextField();
        rollField.setPromptText("Roll Number");
        rollField.setMaxWidth(300);
        rollField.setStyle(fieldStyle());

        requestOtpBtn = new Button("Send OTP");
        requestOtpBtn.setStyle(primaryBtnStyle());
        requestOtpBtn.setOnAction(e -> requestOtp());

        otpField = new TextField();
        otpField.setPromptText("Enter 6-digit OTP");
        otpField.setMaxWidth(300);
        otpField.setStyle(fieldStyle());
        otpField.setDisable(true);

        verifyBtn = new Button("Verify & Check In");
        verifyBtn.setStyle(primaryBtnStyle());
        verifyBtn.setDisable(true);
        verifyBtn.setOnAction(e -> verifyOtp());

        statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(360);
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setStyle("-fx-text-fill: #8be28b; -fx-font-size: 13px;");

        VBox card = new VBox(14, title, subtitle, rollField, requestOtpBtn, otpField, verifyBtn, statusLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(30));
        card.setMaxWidth(420);
        card.setStyle("-fx-background-color: #14161f; -fx-background-radius: 16; -fx-border-color: #1f2230; -fx-border-radius: 16;");

        root.getChildren().add(card);

        Scene scene = new Scene(root, 500, 480);
        stage.setTitle("SK Attendance — Check-in");
        Image icon = new Image(getClass().getResourceAsStream("/images/icon.png"));
        stage.getIcons().add(icon);
        stage.setScene(scene);
        stage.show();
    }

    private String fieldStyle() {
        return "-fx-background-color: #1b1e2b; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 14; -fx-font-size: 13px;";
    }

    private String primaryBtnStyle() {
        return "-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20;";
    }

    private void requestOtp() {
        String roll = rollField.getText().trim();
        if (roll.isEmpty()) {
            setStatus("Enter your roll number first.", false);
            return;
        }

        pendingRoll = roll;
        requestOtpBtn.setDisable(true);
        setStatus("Sending OTP to your registered email...", true);

        String json = "{\"roll_number\":\"" + escape(roll) + "\"}";

        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE + "/checkin/request-otp"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    requestOtpBtn.setDisable(false);
                    if (response.statusCode() == 200) {
                        setStatus("OTP sent! Check your email (valid for 5 minutes).", true);
                        otpField.setDisable(false);
                        verifyBtn.setDisable(false);
                    } else {
                        setStatus("Error: " + response.body(), false);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    requestOtpBtn.setDisable(false);
                    setStatus("Connection error: " + ex.getMessage(), false);
                });
            }
        }).start();
    }

    private void verifyOtp() {
        String otp = otpField.getText().trim();
        if (otp.isEmpty() || pendingRoll == null) {
            setStatus("Request an OTP first.", false);
            return;
        }

        verifyBtn.setDisable(true);
        setStatus("Verifying...", true);

        String json = "{\"roll_number\":\"" + escape(pendingRoll) + "\",\"otp\":\"" + escape(otp) + "\"}";

        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE + "/checkin/verify-otp"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    verifyBtn.setDisable(false);
                    if (response.statusCode() == 200) {
                        setStatus("✓ Attendance marked successfully!", true);
                        otpField.clear();
                        rollField.clear();
                        otpField.setDisable(true);
                        verifyBtn.setDisable(true);
                        pendingRoll = null;
                    } else {
                        setStatus("Error: " + response.body(), false);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    verifyBtn.setDisable(false);
                    setStatus("Connection error: " + ex.getMessage(), false);
                });
            }
        }).start();
    }

    private void setStatus(String text, boolean ok) {
        statusLabel.setStyle("-fx-text-fill: " + (ok ? "#8be28b" : "#e5484d") + "; -fx-font-size: 13px;");
        statusLabel.setText(text);
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
