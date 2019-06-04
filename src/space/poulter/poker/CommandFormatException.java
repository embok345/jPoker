/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package space.poulter.poker;

/**
 * @author Em Poulter
 */
public class CommandFormatException extends Exception {

    public Integer tableID; //-1 for global command
    public boolean wasGameCommand;
    public String command;

    public CommandFormatException(Integer tableID, boolean wasGameCommand, String command) {
        super();
        this.tableID = tableID;
        this.wasGameCommand = wasGameCommand;
        this.command = command;
    }
}
