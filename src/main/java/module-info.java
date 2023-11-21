module com.hivemind.hivemindremixclient {
	requires javafx.controls;
	requires javafx.fxml;


	opens com.hivemind.hivemindremixclient to javafx.fxml;
	exports com.hivemind.hivemindremixclient;
}