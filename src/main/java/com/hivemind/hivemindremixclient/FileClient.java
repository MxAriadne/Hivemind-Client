package com.hivemind.hivemindremixclient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileClient extends Application {

	//Console themeing for easy diag
	public static final String GRAY = "\033[1;90m";
	public static final String RED = "\033[1;91m";
	public static final String GREEN = "\033[1;92m";
	public static final String SUCCESS = GREEN + "SUCCESS: " + GRAY;
	public static final String FAILURE = RED + "FAILURE: " + GRAY;

	private String ip;
	private int port;
	public Label statusLabel;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws IOException {
		// Disable implicit exit, this makes it so the main thread stays open when stage.hide() is used.
		Platform.setImplicitExit(false);

		stage.setTitle("Hivemind Client");

		ImageView logo = new ImageView("hivemind.png");
		logo.setFitHeight(200);
		logo.setFitWidth(200);

		Text title = new Text("Hivemind Client");
		title.setFill(Paint.valueOf("#dfbb0a"));
		title.setFont(Font.font(title.getFont().getName(), FontWeight.BOLD, 20));

		TextField ipField = new TextField("Enter server IP address");
		ipField.setAlignment(Pos.CENTER);
		ipField.setMaxWidth(200);

		TextField portField = new TextField("Enter server port");
		portField.setAlignment(Pos.CENTER);
		portField.setMaxWidth(200);

		Button startClient = new Button("Start Client");
		startClient.setStyle("-fx-background-color: black;");
		startClient.setTextFill(Paint.valueOf("#dfbb0a"));

		Button hideClient = new Button("Hide Client");
		hideClient.setStyle("-fx-background-color: black;");
		hideClient.setTextFill(Paint.valueOf("#dfbb0a"));

		hideClient.setOnAction(e -> stage.hide());

		statusLabel = new Label("");
		statusLabel.setTextFill(Paint.valueOf("#dfbb0a"));
		statusLabel.setTextAlignment(TextAlignment.CENTER);

		VBox vbox = new VBox(30);
		vbox.setStyle("-fx-background-color: #301934;");
		vbox.setAlignment(Pos.CENTER);
		vbox.getChildren().addAll(logo, title, ipField, portField, startClient, hideClient, statusLabel);

		Scene scene = new Scene(vbox, 300, 550);
		stage.setScene(scene);
		// Set the taskbar and window icon to the logo.
		stage.getIcons().add(new Image(("hivemind.png")));
		// Set the title.
		stage.setTitle("Hivemind - Client");

		stage.setResizable(false);
		stage.centerOnScreen();
		stage.show();

		createTrayIcon(stage);

		stage.setOnCloseRequest(e -> System.exit(0));

		startClient.setOnAction(e -> {
			try {
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
							statusLabel.setText("Connection reset! Restart client!");
						}
					}
				}).start();
			} catch (NumberFormatException ex) {
				System.out.println(FAILURE + "Invalid IP or port! Try again!");
				statusLabel.setText("Invalid IP or port! Try again!");
			}
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
			statusLabel.setText("Failed to connect to server!\nCheck internet connection and ports?");
		}
	}

	/*
	 * createTrayIcon()
	 *
	 * stage                Stage               The window for the main program.
	 *
	 * This function creates the tray icon for the program and creates the listener
	 * event to reopen the program when the clay icon is clicked.
	 *
	 */
	public void createTrayIcon(final Stage stage) throws IOException {
		// TrayIcon self-explanatory, this is the object that displays in the system tray on launch.
		TrayIcon trayIcon;
		// Check if this is a Windows PC, if not just minimize to taskbar like normal.
		if (SystemTray.isSupported()) {
			// Get the SystemTray instance.
			SystemTray tray = SystemTray.getSystemTray();
			// Get Hivemind logo for the tray icon.
			java.awt.Image image = ImageIO.read(new File("src/main/resources/hivemindTray.png"));

			// Establish ActionListener for when the tray icon is clicked on
			ActionListener show = e -> Platform.runLater(() -> {
				// Check stage is still valid, start() and initialize() for controllers can cause stage to be null in special cases.
				// This stops the program from hanging.
				if (stage != null) {
					// Show stage
					stage.show();
				} else {
					System.out.println(FAILURE + "Stage is null!");
				}
			});

			// Initialize tray icon.
			trayIcon = new TrayIcon(image, "Hivemind - Server");
			// Add the listener
			trayIcon.addActionListener(show);

			try {
				// Attempt to add to the system tray.
				tray.add(trayIcon);
				System.out.println(SUCCESS + "Tray icon added!");
			} catch (AWTException e) {
				System.out.println(FAILURE + "Exception occured! Unable to add tray icon.");
			}
		} else {
			System.out.println("System tray is not supported on this platform.");
		}
	}
}
