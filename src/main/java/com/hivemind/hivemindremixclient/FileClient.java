package com.hivemind.hivemindremixclient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class FileClient extends Application {

	//Console themeing for easy diag
	public static final String GRAY = "\033[1;90m";
	public static final String RED = "\033[1;91m";
	public static final String GREEN = "\033[1;92m";
	public static final String SUCCESS = GREEN + "SUCCESS: " + GRAY;
	public static final String FAILURE = RED + "FAILURE: " + GRAY;

	private String ip;
	private int port;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) {
		stage.setTitle("Hivemind Client");

		TextField ipField = new TextField("Enter server IP address");
		ipField.setAlignment(Pos.CENTER);
		ipField.setMaxWidth(200);

		TextField portField = new TextField("Enter server port");
		portField.setAlignment(Pos.CENTER);
		portField.setMaxWidth(200);

		Button startClient = new Button("Start Client");
		Label statusLabel = new Label("");

		VBox vbox = new VBox(30);
		vbox.setStyle("-fx-background-color: #301934;");
		vbox.setAlignment(Pos.CENTER);
		vbox.getChildren().addAll(ipField, portField, startClient, statusLabel);

		Scene scene = new Scene(vbox, 300, 300);
		stage.setScene(scene);
		stage.show();

		startClient.setOnAction(e -> {
			this.ip = ipField.getText();
			this.port = Integer.parseInt(portField.getText());
			statusLabel.setText("Now receiving files!");

			// Start the continuous file receiving loop in a separate thread
			new Thread(() -> {
				while (true) {
					receiveFile(ip, port);
					try {
						// Sleep for a while before checking for new files again
						Thread.sleep(5000); // 5 seconds (adjust as needed)
					} catch (InterruptedException exception) {
						System.out.println(FAILURE + "The thread died!");
					}
				}
			}).start();
		});
	}

	private void deleteFile(byte[] fileNameBytes) {
		String deletedFileName = new String(fileNameBytes, StandardCharsets.UTF_8);
		File deletedFile = new File(deletedFileName);
		if (deletedFile.delete()) {
			System.out.println(SUCCESS + "File '" + deletedFileName + "' deleted on client!");
		} else {
			System.out.println(FAILURE + "Failed to delete file '" + deletedFileName + "' on client. (File likely does not exist!)");
		}

	}

	private void receiveFile(String ipAddress, int port) {
		try (Socket socket = new Socket(ipAddress, port);
			 DataInputStream dataInputStream = new DataInputStream(socket.getInputStream())) {

			boolean isDelete = false;
			boolean isFolder = false;

			switch (dataInputStream.readByte()) {
				case 1 -> isDelete = true;
				case 2 -> isFolder = true;
				default -> { System.out.println(FAILURE + "Weird error? No flag sent by server."); }
			}

			// Read the file name length
			int fileNameLength = dataInputStream.read();
			byte[] fileNameBytes = new byte[fileNameLength];
			dataInputStream.readFully(fileNameBytes);
			String receivedFileName = new String(fileNameBytes, StandardCharsets.UTF_8);

			if (isDelete) {
				deleteFile(fileNameBytes);
			} else if (isFolder) {
				Path newDirectory = Paths.get(receivedFileName);

				// This also creates parent dirs if not exists
				Files.createDirectories(newDirectory);
				System.out.println(SUCCESS + "Directory " + receivedFileName + " was created!");
			} else {
				FileOutputStream fileOutputStream = new FileOutputStream(receivedFileName);
				BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

				// Read the file content
				int bytesRead;
				byte[] buffer = new byte[1024];

				while ((bytesRead = dataInputStream.read(buffer)) != -1) {
					bufferedOutputStream.write(buffer, 0, bytesRead);
				}

				bufferedOutputStream.close();

				System.out.println(SUCCESS + "File " + receivedFileName + " was saved!");

			}
		} catch (IOException e) {
			System.out.println(FAILURE + "Failed to connect to server!");

		}
	}


}
