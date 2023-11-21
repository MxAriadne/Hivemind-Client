package com.hivemind.hivemindremixclient;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class FileClient extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("File Client");

		TextField ipTextField = new TextField("127.0.0.1");
		TextField portTextField = new TextField("8080");
		Button connectButton = new Button("Connect and Receive File");

		connectButton.setOnAction(e -> {
			String ipAddress = ipTextField.getText();
			int port = Integer.parseInt(portTextField.getText());

			receiveFile(ipAddress, port);
		});

		VBox vbox = new VBox(10);
		vbox.getChildren().addAll(new Label("IP Address:"), ipTextField, new Label("Port:"), portTextField, connectButton);

		Scene scene = new Scene(vbox, 300, 150);
		primaryStage.setScene(scene);

		primaryStage.show();
	}

	private void receiveFile(String ipAddress, int port) {
		new Thread(() -> {
			try (Socket socket = new Socket(ipAddress, port);
				 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

				String receivedFileName = "received_hello.txt";
				try (PrintWriter writer = new PrintWriter(receivedFileName)) {
					String line;
					while ((line = in.readLine()) != null) {
						writer.println(line);
					}
				}

				System.out.println("File received and saved as " + receivedFileName);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}
}
