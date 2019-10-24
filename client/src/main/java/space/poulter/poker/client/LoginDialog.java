/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package space.poulter.poker.client;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

/**
 *
 * @author em
 */
public class LoginDialog extends Dialog<Pair<String, String>>{
    public LoginDialog() {
        //System.out.println("Creating dialog");
        setTitle("Login to server");
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
        
        //System.out.println("Creating dialog1");
        
        GridPane grid = new GridPane();
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        
        //System.out.println("Creating dialog2");
        
        
        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);
        
        //System.out.println("Creating dialog3");
        
        Node loginButton = getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);
        
        //System.out.println("Creating dialog4");
        
        username.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });
        password.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });
        
        //System.out.println("Creating dialog5");
        
        getDialogPane().setContent(grid);
        
        Platform.runLater(() -> username.requestFocus());
        
        //System.out.println("Creating dialog6");
        
        setResultConverter(dialogButton -> {
            if(dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), password.getText());
            }
            return null;
        });
        
        //System.out.println("Creating dialog7");
    }
}
