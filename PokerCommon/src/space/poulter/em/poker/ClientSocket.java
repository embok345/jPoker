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

package space.poulter.em.poker;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 *
 * @author Em Poulter
 */
public abstract class ClientSocket implements Closeable {
    private final Socket s;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    public final ThreadedReader reader;
    private volatile boolean connectionComplete;
    
    public ClientSocket(Socket s) throws IOException {
        this.s = s;
        out = new ObjectOutputStream(s.getOutputStream());
        in = new ObjectInputStream(s.getInputStream());
        reader = new ThreadedReader();
        connectionComplete = false;
    }
    
    public void setConnectionComplete(boolean isComplete) {
        connectionComplete = isComplete;
    }
    public boolean isConnected() {
        return connectionComplete;
    }
    
    public void initiateClose() throws IOException {
        write("Exit");
        //close();
    }
    @Override
    public void close() {
        System.out.println("Closing");
        try {
            in.close();
            out.close();
            s.close();
        } catch(IOException e) {
            System.err.println("Exception when closing connection");
            System.err.println(e);
        }
    }
    
    public ThreadedReader getReader() {
        return reader;
    }
    
    public void startReader() {
        reader.start();
    }
    
    public Socket getSocket() {
        return s;
    }

    public void write(Object o) throws IOException {
        out.writeObject(o);
        out.reset();
        
    }

    public Object read() throws IOException, ClassNotFoundException {
        Object o = in.readObject();
        return o;
    }
    
    public abstract void processCommand(String str);
    
    public class ThreadedReader extends Thread{
        @Override 
        public void run() {
            Object o;
            try {
                while(!s.isClosed() && (o=read())!=null) {
                    if(o instanceof String) {
                        //System.out.println("Message from tube: "+(String)o);
                        ClientSocket.this.processCommand((String)o);
                    }
                }
            } catch(IOException | ClassNotFoundException e) {
                System.err.println("Exception occured when reading from stream");
                System.err.println(e);
            }
        }
    }
}
