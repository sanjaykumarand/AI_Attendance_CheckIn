package com.attendance.register;

import javafx.application.Application;
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

public class RegisterApp extends Application {

    private static final String API_BASE = "http://127.0.0.1:8001";
    private final HttpClient client = HttpClient.newHttpClient();

    private TextField rollField, nameField, phoneField, emailField;
    private Label statusLabel;
    private TableView<String[]> table;

    @Override
    public void start(Stage stage) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #0f1117;");

        Label title = new Label("Student Registration — Smart Attendance");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);

        rollField = styledField("Roll Number");
        nameField = styledField("Full Name");
        phoneField = styledField("Phone Number");
        emailField = styledField("Email (OTP sent here)");

        form.addRow(0, label("Roll Number:"), rollField);
        form.addRow(1, label("Name:"), nameField);
        form.addRow(2, label("Phone:"), phoneField);
        form.addRow(3, label("Email:"), emailField);

        Button registerBtn = new Button("Register Student");
        registerBtn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8;");
        registerBtn.setOnAction(e -> registerStudent());

        Separator formSep = new Separator();

        Label deleteTitle = new Label("Remove a Student");
        deleteTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e5484d;");

        TextField deleteRollField = styledField("Roll Number to delete");
        this.deleteRollField = deleteRollField;

        Button deleteBtn = new Button("Delete Student");
        deleteBtn.setStyle("-fx-background-color: #7a1f22; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8;");
        deleteBtn.setOnAction(e -> confirmAndDelete());

        HBox deleteRow = new HBox(10, deleteRollField, deleteBtn);
        deleteRow.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #8be28b;");

        table = new TableView<>();
        setupTable();

        Button refreshBtn = new Button("Refresh List");
        refreshBtn.setStyle("-fx-background-color: #1b1e2b; -fx-text-fill: #cfd2e0; -fx-background-radius: 8; -fx-padding: 8 16;");
        refreshBtn.setOnAction(e -> loadStudents());

        root.getChildren().addAll(title, form, registerBtn, formSep, deleteTitle, deleteRow, statusLabel, new Separator(), refreshBtn, table);

        Scene scene = new Scene(root, 640, 700);
        stage.setTitle("SK Attendance — Register");
        Image icon = new Image(getClass().getResourceAsStream("/images/icon.png"));
        stage.getIcons().add(icon);
        stage.setScene(scene);
        stage.show();

        loadStudents();
    }

    private TextField deleteRollField;

    private void confirmAndDelete() {
        String roll = deleteRollField.getText().trim();
        if (roll.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #e5484d;");
            statusLabel.setText("Enter a roll number to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete student " + roll + "?");
        confirm.setContentText("This permanently removes their registration and all attendance records. This cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteStudent(roll);
            }
        });
    }

    private void deleteStudent(String roll) {
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE + "/students/" + roll))
                        .DELETE()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                javafx.application.Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        statusLabel.setStyle("-fx-text-fill: #8be28b;");
                        statusLabel.setText("Deleted: " + roll);
                        deleteRollField.clear();
                        loadStudents();
                    } else {
                        statusLabel.setStyle("-fx-text-fill: #e5484d;");
                        statusLabel.setText("Error: " + response.body());
                    }
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: #e5484d;");
                    statusLabel.setText("Connection error: " + ex.getMessage());
                });
            }
        }).start();
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #cfd2e0;");
        return l;
    }

    private TextField styledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color: #1b1e2b; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 10;");
        return f;
    }

    @SuppressWarnings("unchecked")
    private void setupTable() {
        TableColumn<String[], String> rollCol = new TableColumn<>("Roll No");
        rollCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[0]));
        TableColumn<String[], String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[1]));
        TableColumn<String[], String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[2]));
        TableColumn<String[], String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[3]));

        table.getColumns().addAll(rollCol, nameCol, phoneCol, emailCol);
        table.setPrefHeight(300);
    }

    private void registerStudent() {
        String roll = rollField.getText().trim();
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();

        if (roll.isEmpty() || name.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #e5484d;");
            statusLabel.setText("All fields are required.");
            return;
        }

        String json = String.format(
                "{\"roll_number\":\"%s\",\"name\":\"%s\",\"phone_number\":\"%s\",\"email\":\"%s\"}",
                escape(roll), escape(name), escape(phone), escape(email));

        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE + "/register"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                javafx.application.Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        statusLabel.setStyle("-fx-text-fill: #8be28b;");
                        statusLabel.setText("Registered: " + name);
                        rollField.clear(); nameField.clear(); phoneField.clear(); emailField.clear();
                        loadStudents();
                    } else {
                        statusLabel.setStyle("-fx-text-fill: #e5484d;");
                        statusLabel.setText("Error: " + response.body());
                    }
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: #e5484d;");
                    statusLabel.setText("Connection error: " + ex.getMessage() + " — is attendance_api_server.py running?");
                });
            }
        }).start();
    }

    private void loadStudents() {
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(API_BASE + "/students")).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String body = response.body();

                javafx.application.Platform.runLater(() -> {
                    table.getItems().clear();
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                            "\"roll_number\":\"(.*?)\",\"name\":\"(.*?)\",\"phone_number\":\"(.*?)\",\"email\":\"(.*?)\""
                    ).matcher(body);
                    while (m.find()) {
                        table.getItems().add(new String[]{m.group(1), m.group(2), m.group(3), m.group(4)});
                    }
                });
            } catch (Exception ignored) {
            }
        }).start();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
