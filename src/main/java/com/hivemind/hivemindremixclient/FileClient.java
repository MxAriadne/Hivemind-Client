package com.hivemind.hivemindremixclient;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class FileClient extends Application {

	private static final String SERVER_IP = "192.168.68.103";
	private static final int SERVER_PORT = 8080;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("File Client");

		Label statusLabel = new Label("Waiting for files...");

		VBox vbox = new VBox(10);
		vbox.getChildren().addAll(statusLabel);

		Scene scene = new Scene(vbox, 300, 150);
		primaryStage.setScene(scene);

		primaryStage.show();

		// Start the continuous file receiving loop in a separate thread
		new Thread(() -> {
			while (true) {
				receiveFile(SERVER_IP, SERVER_PORT, statusLabel);
				try {
					// Sleep for a while before checking for new files again
					Thread.sleep(5000); // 5 seconds (adjust as needed)
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private void receiveFile(String ipAddress, int port, Label statusLabel) {
		try (Socket socket = new Socket(ipAddress, port);
			 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

			// Read the file name from the server
			String receivedFileName = in.readLine();
			if (receivedFileName != null) {
				System.out.println("Receiving file: " + receivedFileName);

				try (PrintWriter writer = new PrintWriter(receivedFileName);
					 BufferedWriter bufferedWriter = new BufferedWriter(writer)) {

					String line;
					while ((line = in.readLine()) != null) {
						bufferedWriter.write(line);
						bufferedWriter.newLine();
					}

					System.out.println("File received and saved as " + receivedFileName);

					// Update UI or perform any other actions as needed
					statusLabel.setText("Received file: " + receivedFileName);
				}
			} else {
				// No new files at the moment
				System.out.println("No new files.");
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
