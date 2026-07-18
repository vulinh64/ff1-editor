package com.ff1.editor.view.ui;

import java.io.PrintWriter;
import java.io.StringWriter;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;

public final class FxDialogs {

  private FxDialogs() {}

  public static void showError(String title, Throwable error) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(error.getMessage());
    TextArea details = new TextArea(stackTrace(error));
    details.setEditable(false);
    details.setWrapText(false);
    details.setPrefRowCount(12);
    details.setPrefColumnCount(90);
    alert.getDialogPane().setExpandableContent(details);
    alert.showAndWait();
  }

  private static String stackTrace(Throwable error) {
    StringWriter writer = new StringWriter();
    error.printStackTrace(new PrintWriter(writer));
    return writer.toString();
  }
}
