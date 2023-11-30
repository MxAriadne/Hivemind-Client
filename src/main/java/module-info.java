module com.hivemind.hivemindremixclient {
	requires javafx.controls;
	requires javafx.fxml;
	requires java.desktop;
		exports com.hivemind.hivemindremixclient;

	opens com.hivemind.hivemindremixclient to javafx.fxml;
}