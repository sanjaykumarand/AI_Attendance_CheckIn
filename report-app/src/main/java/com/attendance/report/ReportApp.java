package com.attendance.report;

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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReportApp extends Application {

    private static final String API_BASE = "http://127.0.0.1:8001";
    private final HttpClient client = HttpClient.newHttpClient();

    private DatePicker fromPicker, toPicker;
    private TextArea reportArea;
    private Label statusLabel;
    private Button generateBtn;
    private TextField askField;
    private Button askBtn;
    private TextArea answerArea;

    @Override
    public void start(Stage stage) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #0f1117;");

        Label title = new Label("AI Attendance Report");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label subtitle = new Label("Generates a natural-language summary using Gemini, based on your attendance records.");
        subtitle.setStyle("-fx-text-fill: #9aa0b4; -fx-font-size: 12px;");
        subtitle.setWrapText(true);

        fromPicker = new DatePicker(LocalDate.now());
        toPicker = new DatePicker(LocalDate.now());
        styleDatePicker(fromPicker);
        styleDatePicker(toPicker);

        HBox quickButtons = new HBox(8);
        Button todayBtn = quickLink("Today", () -> {
            fromPicker.setValue(LocalDate.now());
            toPicker.setValue(LocalDate.now());
        });
        Button weekBtn = quickLink("Last 7 Days", () -> {
            fromPicker.setValue(LocalDate.now().minusDays(7));
            toPicker.setValue(LocalDate.now());
        });
        Button monthBtn = quickLink("Last 30 Days", () -> {
            fromPicker.setValue(LocalDate.now().minusDays(30));
            toPicker.setValue(LocalDate.now());
        });
        quickButtons.getChildren().addAll(todayBtn, weekBtn, monthBtn);

        GridPane dateForm = new GridPane();
        dateForm.setHgap(12);
        dateForm.setVgap(10);
        dateForm.addRow(0, label("From:"), fromPicker, label("To:"), toPicker);

        generateBtn = new Button("Generate AI Report");
        generateBtn.setStyle("-fx-background-color: #ea580c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8;");
        generateBtn.setOnAction(e -> generateReport());

        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #9aa0b4; -fx-font-size: 12px;");

        reportArea = new TextArea();
        reportArea.setEditable(false);
        reportArea.setWrapText(true);
        reportArea.setPrefHeight(400);
        reportArea.setStyle("-fx-control-inner-background: #14161f; -fx-text-fill: #e6e8f0; -fx-font-family: 'Consolas', monospace; -fx-font-size: 13px;");

        Button saveBtn = new Button("Save Report to File");
        saveBtn.setStyle("-fx-background-color: #1b1e2b; -fx-text-fill: #cfd2e0; -fx-background-radius: 8; -fx-padding: 8 16;");
        saveBtn.setOnAction(e -> saveReport());

        Separator askSep = new Separator();

        Label askTitle = new Label("Ask a question about attendance");
        askTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        askField = new TextField();
        askField.setPromptText("e.g. Who was absent the most this week? / Who created this app?");
        askField.setStyle("-fx-background-color: #1b1e2b; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 14; -fx-font-size: 13px;");
        askField.setOnAction(e -> askQuestion());
        HBox.setHgrow(askField, Priority.ALWAYS);

        askBtn = new Button("Ask");
        askBtn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20;");
        askBtn.setOnAction(e -> askQuestion());

        HBox askRow = new HBox(10, askField, askBtn);
        askRow.setAlignment(Pos.CENTER_LEFT);

        answerArea = new TextArea();
        answerArea.setEditable(false);
        answerArea.setWrapText(true);
        answerArea.setPrefHeight(140);
        answerArea.setStyle("-fx-control-inner-background: #14161f; -fx-text-fill: #8be28b; -fx-font-size: 13px;");

        root.getChildren().addAll(title, subtitle, dateForm, quickButtons, generateBtn, statusLabel, reportArea, saveBtn,
                askSep, askTitle, askRow, answerArea);

        Scene scene = new Scene(root, 700, 900);
        stage.setTitle("SK Attendance — AI Report");
        Image icon = new Image(getClass().getResourceAsStream("/images/icon.png"));
        stage.getIcons().add(icon);
        stage.setScene(scene);
        stage.show();
    }

    private Button quickLink(String text, Runnable action) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #1b1e2b; -fx-text-fill: #cfd2e0; -fx-background-radius: 6; -fx-padding: 6 12; -fx-font-size: 11px;");
        b.setOnAction(e -> action.run());
        return b;
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #cfd2e0;");
        return l;
    }

    private void styleDatePicker(DatePicker dp) {
        dp.setStyle("-fx-background-color: #1b1e2b;");
    }

    private void generateReport() {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        String fromDate = fromPicker.getValue().format(fmt);
        String toDate = toPicker.getValue().format(fmt);

        generateBtn.setDisable(true);
        statusLabel.setText("Generating report via Gemini...");
        reportArea.setText("");

        String json = "{\"date_from\":\"" + fromDate + "\",\"date_to\":\"" + toDate + "\"}";

        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE + "/report"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    generateBtn.setDisable(false);
                    if (response.statusCode() == 200) {
                        String report = extractJsonField(response.body(), "report");
                        reportArea.setText(report);
                        statusLabel.setText("Report generated for " + fromDate + " to " + toDate);
                    } else {
                        statusLabel.setText("Error: " + response.body());
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    generateBtn.setDisable(false);
                    statusLabel.setText("Connection error: " + ex.getMessage() + " — is attendance_api_server.py running?");
                });
            }
        }).start();
    }

    private String extractJsonField(String json, String field) {
        Pattern p = Pattern.compile("\"" + field + "\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        return "(Could not parse report from response)";
    }

    private void saveReport() {
        String content = reportArea.getText();
        if (content.isBlank()) {
            statusLabel.setText("Nothing to save yet — generate a report first.");
            return;
        }
        String filename = "attendance_report_" + System.currentTimeMillis() + ".txt";
        try (java.io.FileWriter fw = new java.io.FileWriter(filename)) {
            fw.write(content);
            statusLabel.setText("Saved to " + filename);
        } catch (Exception ex) {
            statusLabel.setText("Failed to save: " + ex.getMessage());
        }
    }

    private void askQuestion() {
        String question = askField.getText().trim();
        if (question.isEmpty()) return;

        askBtn.setDisable(true);
        answerArea.setText("Thinking...");

        String fromDate = fromPicker.getValue() != null ? fromPicker.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE) : "";
        String toDate = toPicker.getValue() != null ? toPicker.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE) : "";

        String json = String.format(
                "{\"question\":\"%s\",\"date_from\":\"%s\",\"date_to\":\"%s\"}",
                escapeJson(question), fromDate, toDate);

        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE + "/ask"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    askBtn.setDisable(false);
                    if (response.statusCode() == 200) {
                        String answer = extractJsonField(response.body(), "answer");
                        answerArea.setText(answer);
                        askField.clear();
                    } else {
                        answerArea.setText("Error: " + response.body());
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    askBtn.setDisable(false);
                    answerArea.setText("Connection error: " + ex.getMessage());
                });
            }
        }).start();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
