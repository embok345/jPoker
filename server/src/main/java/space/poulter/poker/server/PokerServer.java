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

package space.poulter.poker.server;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import space.poulter.poker.ClientSocket;
import space.poulter.poker.PokerTableData;

/**
 *
 * @author Em Poulter <em@poulter.space>
 */
public class PokerServer extends Thread {
    
    private final String DB_USERNAME = "poker";
    private final String DB_PASSWORD = "***OOPS***";
    private final String DB_NAME = "poker";
    private final boolean USING_DB = false;
    
    Integer port;
    Integer numTables;
    
    Map<Integer, PokerTable> tables;
    //List<ClientSocket> sockets;
    BiMap<Integer, ClientSocket> sockets;
    ServerSocket serverSocket;
    
    public class ServerSideSocket extends ClientSocket {
        
        public ServerSideSocket(Socket s) throws IOException {
            super(s);
        }
        
        @Override
        public void processCommand(String str) {
            
            if(str.startsWith("auth:")) {
                
                if(!USING_DB) {
                    try {
                        write("auth:done");
                        if(!sockets.containsValue(this)) {
                            sockets.put(sockets.size(), this);
                        }
                    } catch(IOException ex) {
                        System.err.println("Could not send command");
                        System.err.println(ex);
                    }
                    return;
                }
                
                //<editor-fold defaultstate="collapsed" desc="authorise user, if using db">
                
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch(ClassNotFoundException ex) {
                    System.err.println("Could not find jdbc connector");
                    System.err.println(ex);
                    //return;
                }
                
                String username, password;
                
                try {
                    if(sockets.containsValue(this)) {
                        write("auth:fail:4");
                        return;
                    }
                    String upperString = str.substring(str.indexOf(":")+1); //user:$name:pass:$pwd
                    if(!upperString.startsWith("user:")) {
                        write("auth:fail:1");
                        return;
                    }
                    upperString = upperString.substring(upperString.indexOf(":")+1);//$name:pass:$pwd
                    if(!upperString.contains(":")) {
                        write("auth:fail:1");
                        return;
                    }
                    username = upperString.substring(0, upperString.indexOf(":"));//$name
                    upperString = upperString.substring(upperString.indexOf(":")+1);//pass:$pwd
                    if(!upperString.startsWith("pass:")) {
                        write("auth:fail:1");
                        return;
                    }
                    password = upperString.substring(upperString.indexOf(":")+1);
                    
                    System.out.println("username: "+username+", password: "+password);
                    
                } catch(IOException ex) {
                    System.err.println("Exception when failing authorisation");
                    System.err.println(ex);
                    return;
                }
                
                try(Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/"+DB_NAME+"?useSSL=no", DB_USERNAME, DB_PASSWORD)) {
                    
                    String selectStr = "select pkid, crypt, enabled, connected from user where name = ?";
                    
                    try (PreparedStatement pStmt = conn.prepareStatement(selectStr)) {
                        pStmt.setString(1, username);
                        
                        try(ResultSet rs = pStmt.executeQuery()) {
                            Integer pkid = -1;
                            char[] passwd = null;
                            boolean enabled = false, connected = true;
                            int noResults = 0;
                            while(rs.next()) {
                                noResults++;
                                pkid = rs.getInt("pkid");
                                passwd = rs.getString("crypt").toCharArray();
                                enabled = rs.getBoolean("enabled");
                                connected = rs.getBoolean("connected");
                            }
                            if(noResults!=1) {
                                System.err.println("Authorisation failed: user doesn't exist");
                                write("auth:fail:2");
                                return;
                            }
                            if(!enabled) {
                                System.err.println("Authorisation failed: acoount not enabled");
                                write("auth:fail:3");
                                return;
                            }
                            if(connected || sockets.containsKey(pkid)) {
                                System.err.println("Authorisation failed: already connected");
                                write("auth:fail:4");
                                return;
                            }
                            
                            BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), passwd);
                            
                            if(result.verified) {
                                System.out.println("user authorised");
                                write("auth:done");
                                
                                try(PreparedStatement updateStmt = conn.prepareStatement("update user set connected = ? where name = ?")) {
                                    updateStmt.setInt(1, 1);
                                    updateStmt.setString(2, username);
                                    int i = updateStmt.executeUpdate();
                                    if(i!=1) {
                                        System.err.println("The update didn't work");
                                    }
                                }
                                
                                sockets.put(pkid, this);
                                
                                return;
                                
                            } else {
                                System.err.println("Authorisation failed: wrong password");
                                write("auth:fail:5");
                                return;
                            }
                        }
                        
                    }
                    
                } catch(SQLException | IOException ex) {
                    System.err.println("Exception when authorising");
                    System.err.println(ex);
                }
//</editor-fold>
            
                
            }
            
            if(str.equals("Get Tables")) {
                //System.out.println(ServerSideSocket.this);
                try {
                    write("Table List:");
                    write(tables.size());
                    for(Map.Entry<Integer, PokerTable> table : tables.entrySet()) {
                        PokerTableData dat = ((PokerTableData)table.getValue().getData());
                        write(dat);
                    }
                } catch(IOException e) {
                    System.err.println("Exception occured when sending table list");
                    System.err.println(e);
                }
                return;
            }
            
            if(str.startsWith("View:")) {
                Integer index = Integer.parseInt(str.substring(5));
                if(tables.get(index).hasSocket(this)) return;
                tables.get(index).addSocket(this);
                return;
            }
            
            if(str.startsWith("ID:")) {
                Integer index = Integer.parseInt(str.substring(3, str.indexOf(':', 3)));
                tables.get(index).processCommand(str.substring(str.indexOf(':', 3)+1), this);
                return;
            }
            if(str.equals("Exit")) {
                System.out.println("Socket closing");
                
                if(USING_DB) {
                    try(Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/"+DB_NAME+"?useSSL=no", DB_USERNAME, DB_PASSWORD)) {
                    
                        String selectStr = "update user set connected = ? where pkid = ?";
                    
                        try (PreparedStatement pStmt = conn.prepareStatement(selectStr)) {
                            pStmt.setInt(1, 0);
                            pStmt.setInt(2, sockets.inverse().get(this));
                        
                            pStmt.executeUpdate();
                        }
                    } catch(SQLException ex) {
                        System.err.println(ex);
                    }
                }
                try {
                    write("Exit");
                } catch(IOException ex) {
                    System.err.println("Exception when telling client to exit");
                    System.err.println(ex);
                }
                
                sockets.inverse().remove(this);
                
                close();
                //return;
            }
        }
    }
    
    private static void sendHelp() {
        System.out.println("help");

        System.exit(-1);
    }
    
    private void init() {
        
        tables = new HashMap<>();
        if(numTables == -1) {
            Random rand = new Random();
            numTables = rand.nextInt(100)+2;
        }
        for(int i = 0; i<numTables/2; i++) {
            PokerTable newTable = new PokerTable();
            newTable.init(i+1, 6);
            tables.put(i+1, newTable);
            newTable = new PokerTable();
            newTable.init((i+numTables/2)+1, 8);
            tables.put((i+numTables/2)+1, newTable);
        }
        if(port==-1) {
            port = 1111;
        }
        try {
            serverSocket = new ServerSocket(port); //TODO: change from normal socket to ssl socket
            //sockets = new ArrayList<>();
            sockets = HashBiMap.create();
        } catch (IOException ex) {
            //serverSocket = null;
            System.err.println("Exception occured when creating server socket");
            System.err.println(ex);
            System.exit(-1);
        } 
    }
    
    public PokerServer(String args[]) {
        
        port = -1;
        numTables = -1;
        
        List<String> listArgs = Arrays.asList(args);
        Iterator<String> it = listArgs.iterator();
        while(it.hasNext()) {
            String s = it.next();
            if(!s.startsWith("-")) {
                System.err.println("The command line option '"+s+"' was not recognised");
                sendHelp();
            }
            switch(s) {
              
                case "-n":
                case "-numtables":
                        String val = it.next();
                        try {
                            numTables = Integer.parseInt(val);
                            if(numTables < 1 || numTables > 100)
                                throw new NumberFormatException();
                        } catch(NumberFormatException e) {
                            System.err.println("The number of tables '"+val+"' was not a valid amount.");
                            sendHelp();
                            System.exit(-1);
                        }
                        break;
                
                case "-p": 
                case "-port":
                        val = it.next();
                        try {
                            port = Integer.parseInt(val);
                        } catch(NumberFormatException e) {
                            System.err.println("The port "+val+" was not a valid port");
                            sendHelp();
                            System.exit(-1);
                        }
                        break;
                case "-h":
                case "-help": 
                        sendHelp();
                        break;
                default: System.err.println("The command line option '"+s+"' was not recognised");
                         sendHelp();
            } 
        }

        init();
    }
    
    @Override
    public void run() {
        while(true) {
            try {
                ServerSideSocket newSock = new ServerSideSocket(serverSocket.accept());
                newSock.startReader();
                if(USING_DB) 
                    newSock.write("auth:req");
                else {
                    newSock.write("auth:done");
                    sockets.put(sockets.size(), newSock);
                }
                
            } catch(IOException ex) {
                System.err.println("Exception occured when trying to accept a new connection. Was the socket closed?");
                System.err.println(ex);
                //System.exit(-1);
            }
        }
    }
    
}
