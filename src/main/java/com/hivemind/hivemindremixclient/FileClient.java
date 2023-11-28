package com.hivemind.hivemindremixclient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

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
				receiveFile(SERVER_IP, SERVER_PORT);
				try {
					// Sleep for a while before checking for new files again
					Thread.sleep(5000); // 5 seconds (adjust as needed)
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private void deleteFile(byte[] fileNameBytes) {
		String deletedFileName = new String(fileNameBytes, StandardCharsets.UTF_8);
		File deletedFile = new File(deletedFileName);
		if (deletedFile.delete()) {
			System.out.println("File '" + deletedFileName + "' deleted on client.");
		} else {
			System.out.println("Failed to delete file '" + deletedFileName + "' on client.");
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
				default -> { System.out.println("Weird error? No flag sent by server."); }
			}

			System.out.println("isDelete: " + isDelete);
			System.out.println("isFolder: " + isFolder);

			// Read the file name length
			int fileNameLength = dataInputStream.read();
			System.out.println("Length: " + fileNameLength);
			byte[] fileNameBytes = new byte[fileNameLength];
			System.out.println("Filename Bytes: " + Arrays.toString(fileNameBytes));
			dataInputStream.readFully(fileNameBytes);
			String receivedFileName = new String(fileNameBytes, StandardCharsets.UTF_8);
			System.out.println("Received Filename: " + receivedFileName);

			if (isDelete) {
				deleteFile(fileNameBytes);
			} else if (isFolder) {
				System.out.println("Creating dir: " + receivedFileName);
				Path newDirectory = Paths.get(receivedFileName);

				// This also creates parent dirs if not exists
				Files.createDirectories(newDirectory);
			} else {

				System.out.println("Receiving file: " + receivedFileName);

				FileOutputStream fileOutputStream = new FileOutputStream(receivedFileName);
				BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

				// Read the file content
				int bytesRead;
				byte[] buffer = new byte[1024];

				while ((bytesRead = dataInputStream.read(buffer)) != -1) {
					bufferedOutputStream.write(buffer, 0, bytesRead);
				}

				bufferedOutputStream.close();

				System.out.println("File received and saved as " + receivedFileName);
			}
		} catch (IOException e) {
			Stage stage = new Stage();
			start(stage);
			//e.printStackTrace();
		}
	}


}
