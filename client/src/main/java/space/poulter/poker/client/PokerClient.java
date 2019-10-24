/*
 * Copyright (C) 2018 Em Poulter <em@poulter.space>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package space.poulter.poker.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Pair;
import space.poulter.poker.ClientSocket;
import space.poulter.poker.CommandFormatException;
import space.poulter.poker.PokerTableData;

/**
 *
 * @author Em Poulter
 */
public class PokerClient extends Application {
    
    private Stage primaryView;
    
    private Map<Integer, PokerTableStage> windows;
    int tableCount;
    ClientSideSocket socket;
    Map<Integer, PokerTableDataClient> tablesData;
    
    public class ClientSideSocket extends ClientSocket {

        public ClientSideSocket(Socket s) throws IOException {
            super(s);
        }
        
        protected void startup() throws IOException {
            getReader().setDaemon(true);
            startReader();
            //write("Hello server");
        }
        
        @Override
        public void processCommand(String str) {
            //System.out.println("got command "+ str);
            
            if(str.equals("auth:req")) {
                Platform.runLater(() -> {
                    LoginDialog login = new LoginDialog();
                    Optional<Pair<String, String>> result = login.showAndWait();
                    result.ifPresent(usernamePassword -> {
                        //System.out.println("Username="+usernamePassword.getKey()+", Password="+usernamePassword.getValue());
                        try {
                            socket.write("auth:user:"+usernamePassword.getKey()+":pass:"+usernamePassword.getValue());
                        } catch(IOException ex) {
                            System.err.println("Exception when sending username and password");
                            System.err.println(ex);
                        }
                    });
                });
                return;
            }
            
            if(str.equals("auth:done")) {
                try {
                    setConnectionComplete(true);
                    synchronized(ClientSideSocket.this) {
                        ClientSideSocket.this.notify();
                    }
                    //System.out.println("Connection complete");
                } catch(IllegalMonitorStateException ex) {
                    System.err.println("Exception when connecting");
                    System.err.println(ex);
                }
            }
            
            if(str.startsWith("ID:")) {
                Integer index = Integer.parseInt(str.substring(3, str.indexOf(':', 3)));
                if(!windows.containsKey(index)) return;
                PokerTableStage table = windows.get(index);
                //System.out.println(str);
                try {
                    table.processCommand(str.substring(str.indexOf(':', 3)+1));
                } catch(CommandFormatException ex) {
                    ex.printStackTrace();
                    //TODO do something better if we get a command format exception
                }
                return;
            }
            if(str.equals("Table List:")) {
                try {
                    Object o = read();
                    if(!(o instanceof Integer)) throw new ClassNotFoundException();
                    Integer noTables = (Integer)o;
                    //System.out.println(noTables);
                    for(int i = 0; i<noTables; i++) {
                        o = read();
                        if(!(o instanceof PokerTableData)) throw new ClassNotFoundException();
                        PokerTableDataClient data = new PokerTableDataClient((PokerTableData)o);
                        data.updateProperties();
                        tablesData.put(data.getTableID(), data);
                        //System.out.println(data.getTableID());
                    }
                } catch(IOException | ClassNotFoundException ex) {
                    System.err.println("Exception occured when receiving table list");
                    System.err.println(ex);
                }
                //System.out.println("Showing table list");
                Platform.runLater(() -> {
                    ((VBox)primaryView.getScene().getRoot()).getChildren().add(formatTableList());
                });
                return;
            }
            
            if(str.equals("Exit")) {
                synchronized(ClientSideSocket.this) {
                    ClientSideSocket.this.notify();
                }
            }
        }
        
    }
    public static void main(String[] args) {

        launch(args);
        
    }
    
    private void init(Stage primaryStage) {
        
        this.primaryView = primaryStage;
        
        socket = null;
        tablesData = new HashMap<>();
        
        //System.out.println("Test");
        
        windows = new HashMap<>();
        primaryStage.setOnCloseRequest((WindowEvent event) -> {
            if(windows.size() > 0) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "you have other windows open. Still exit?");
                Optional<ButtonType> result = alert.showAndWait();
                if(result.isPresent() && result.get() == ButtonType.OK) {
                    try {
                        windows.entrySet().forEach((window) -> {
                            window.getValue().quit();
                        });
                        if(socket!=null) 
                            socket.initiateClose();
                        synchronized(socket) {
                            socket.wait();
                            System.out.println("Done waiting");
                            socket.close();
                            Platform.exit();
                        }
                        
                    } catch (IOException | InterruptedException ex) {
                        System.err.println("Exception occured when closing");
                        System.err.println(ex);
                    }
                    
                    
                } else{
                    event.consume();
                }
            } else {
                try {
                    windows.entrySet().forEach((window) -> {
                        window.getValue().quit();
                    });
                    if(socket!=null) {
                        socket.initiateClose();
                        synchronized(socket) { //TODO I'm sure I had a better way of getting a smooth closure
                            socket.wait();
                            socket.close();
                            Platform.exit();
                        }
                    }
                } catch (IOException | InterruptedException ex) {
                    System.err.println("Exception occured when closing");
                    System.err.println(ex);
                }
                Platform.exit();
            }
        });

        
        Scene scene = new Scene(new VBox(), 400, 350);

        MenuBar menuBar = new MenuBar();
 
        // --- Menu File
        Menu menuFile = new Menu("File");
        MenuItem menuFileConnectToServer = new MenuItem("Connect to server");
        menuFileConnectToServer.setOnAction((e) -> {
            TextInputDialog dialog = new TextInputDialog("");
            dialog.setTitle("Connect to server");
            dialog.setHeaderText("Enter server address");
            dialog.setContentText("Server address:");
            
            Optional<String> result = null;
            boolean done = false;
            do {
                result = dialog.showAndWait();
                if(result.isPresent() && !result.get().equals("")) {
                    String response = result.get();
                    try {
                        Integer port = 1111;
                        if(response.contains(":")) {
                            port = Integer.parseInt(response.substring(response.indexOf(":")+1));
                            response = response.substring(0, response.indexOf(":"));
                        }
                        InetAddress addr = InetAddress.getByName(response);
                        //InetAddress.
                        Socket s = new Socket(addr, port);
                        socket = new ClientSideSocket(s);
                        
                        done = true;

                        Thread t = new Thread(() -> {
                            try {
                                synchronized(socket) {
                                    socket.startup();
                                    socket.wait();
                                }
                                //System.out.println("Connection completed");
                                socket.write("Get Tables");
                            } catch(IOException | InterruptedException | IllegalMonitorStateException ex) {
                                System.err.println("Exception when waiting for connection to comeple");
                                System.err.println(ex);
                            }
                        });
                        t.setDaemon(true);
                        t.start();
                        
                    } catch(NumberFormatException | UnknownHostException ex) {
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle("Error when connecting to server");
                        alert.setHeaderText("The address entered is not a valid address");
                        alert.showAndWait();
                        System.err.println("Couldn't convert to ip address");
                        System.err.println(ex);
                        done = false;
                    } catch(IOException ex) {
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle("Error when connecting to server");
                        alert.setHeaderText("Could not connect to the server");
                        alert.showAndWait();
                        System.err.println("Couldn't connect to socket");
                        System.err.println(ex);
                        done = false;
                    }
                } else {
                    done = true;
                }
            } while(!done);
            
        });
        MenuItem menuFileExit = new MenuItem("Exit");
        menuFile.getItems().addAll(menuFileConnectToServer, menuFileExit);
 
        menuBar.getMenus().add(menuFile);
 
        ((VBox) scene.getRoot()).getChildren().addAll(menuBar);
 
        primaryStage.setScene(scene);
        
        primaryStage.show();
    }
    
    public void requestView(PokerTableDataClient dat) {
        if(windows.containsKey(dat.getTableID())) return;
        try {
            socket.write("View:"+dat.getTableID());
            
            PokerTableStage newTable = new PokerTableStage(dat, socket);
            newTable.setOnCloseRequest((WindowEvent e) -> {
                //System.out.println("Quitting");
                newTable.quit();
                windows.remove(newTable.getID());
            });
            windows.put(dat.getTableID(), newTable);
            newTable.sendCommand("update");
            newTable.show();
            
        } catch(IOException e) {
            System.err.println("Exception occured when viewing new table");
            System.err.println(e);
        }
    }
    
    public Accordion formatTableList() {
        TableView<PokerTableDataClient> table = new TableView<>();
        ObservableList<PokerTableDataClient> pokerTables = FXCollections.observableArrayList(tablesData.values());
        
        table.setItems(pokerTables);
        
        TableColumn<PokerTableDataClient, Integer> tableIDCol = new TableColumn<>("Table ID");
        TableColumn<PokerTableDataClient, Integer> maxHandsCol = new TableColumn<>("Max hands");
        TableColumn<PokerTableDataClient, PokerTableDataClient> buttonCol = new TableColumn<>("");
        
        
        tableIDCol.setCellValueFactory(new PropertyValueFactory("tableID"));
        maxHandsCol.setCellValueFactory(new PropertyValueFactory("maxHands"));
        
        buttonCol.setCellValueFactory((TableColumn.CellDataFeatures<PokerTableDataClient, PokerTableDataClient> features) -> new ReadOnlyObjectWrapper(features.getValue()));
        
        buttonCol.setCellFactory((TableColumn<PokerTableDataClient, PokerTableDataClient> btnCol) -> new TableCell<PokerTableDataClient, PokerTableDataClient>() {
            Button b = new Button("View");
            @Override
            public void updateItem(PokerTableDataClient dat, boolean empty) {
                super.updateItem(dat, empty);
                setGraphic(b);
                b.setOnMouseClicked((MouseEvent e) -> {
                    requestView(dat);
                });
            }
        });
  
        table.getColumns().setAll(tableIDCol, maxHandsCol, buttonCol);
        
        TitledPane t1 = new TitledPane("Tables", table);
        Accordion accordion = new Accordion();
        
        accordion.getPanes().add(t1);
        
        return accordion;
    }
    
    @Override
    public void start(Stage primaryStage) {
        init(primaryStage);
    }
}
