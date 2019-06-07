package space.poulter.poker.client;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

/**
 * Create the dialog to authenticate with the server. Contains a username and password field, and returns
 * their getRanks in a Pair.
 * This probably could be changed to an anonymous inner class, but I'm not sure which method would want overriding,
 * so we just deal with the constructor.
 *
 * @author Em Poulter <em@poulter.space>
 */
class LoginDialog extends Dialog<Pair<String, String>> {

    /**
     * Creates the dialog.
     */
    LoginDialog() {

        /* Set the title and the login and cancel buttons */
        setTitle("Login to server");
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        /* Setup the layout of the dialog */
        GridPane grid = new GridPane();
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);


        /* Retrieve the login button, and set it disabled to start with */
        Node loginButton = getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        /* If either the username or password is empty, or composed just of
         * white space, disable the login button.
         * TODO it seems the button would be enabled if either are non empty, rather
         *  than both non empty*/
        username.textProperty().addListener((observable, oldValue, newValue) ->
                loginButton.setDisable(newValue.trim().isEmpty())
        );
        password.textProperty().addListener((observable, oldValue, newValue) ->
                loginButton.setDisable(newValue.trim().isEmpty())
        );

        /* Set the dialog layout */
        getDialogPane().setContent(grid);

        /* Get the focus for the username field */
        Platform.runLater(username::requestFocus);

        /* Get the result from the dialog */
        setResultConverter(dialogButton ->
                /* If login was selected, return the Pair consisting of the username & password, otherwise return null */
                (dialogButton == loginButtonType) ? new Pair<>(username.getText(), password.getText()) : null
        );
    }
}
