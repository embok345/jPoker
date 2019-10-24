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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import space.poulter.poker.Card;
import space.poulter.poker.Cards;
import space.poulter.poker.ClientSocket;
import space.poulter.poker.Deck;
import space.poulter.poker.Deck.DrawCardException;
import space.poulter.poker.PlayerData;
import space.poulter.poker.Poker;
import space.poulter.poker.Poker.PokerAction;
import space.poulter.poker.Poker.ScoredBoard;
import space.poulter.poker.PokerTableData;

/**
 *
 * @author Em Poulter <em@poulter.space>
 */
public class PokerTable {
    private PokerTableData dat;
    private List<ClientSocket> connectedSockets;
    private Thread gameThread;
    
    public void init(int tableID, int noHands) {
        dat = new PokerTableData();
        dat.init(tableID, noHands);
        connectedSockets = new ArrayList<>();
        gameThread = new Thread() {
            @Override
            public void run() {
                runGame();
            }
        };
        gameThread.start();
        
    }
    
    void processCommand(String command, ClientSocket socket) {
        if(!getSockets().contains(socket)) {
            System.err.print("Recieved command from unrecognised socket");
            return;
        }
        if(command.equals("quit")) {
            //System.out.println("quitting");
            removeSocket(socket);
            return;
        }
        if(command.equals("update")) {
            try {
                socket.write("ID:"+getTableID()+":update");
                //System.out.println("Sending updated info: "+getData());
                socket.write(getData());
            } catch(IOException e) {
                System.err.println("Exception occured when sending update to client");
                System.err.println(e);
            }
            return;
        }
        
        if(command.contains(":")) {
            
            String subcommand = command.substring(0, command.indexOf(':'));
            //System.out.println("command received: "+subcommand);
            
            if(subcommand.equals("game")) {
                processGameCommand(command.substring(subcommand.length()+1), socket);
            }
            
            if(subcommand.equals("sit")) {
                Integer index = Integer.parseInt(command.substring(command.indexOf(':')+1));
            
                if(dat.seatIsOccupied(index)) return;
                //Checks if the requestinig player is already sat at the table
                for(int i = 0; i< dat.getMaxHands(); i++) {
                    if(dat.seatIsOccupied(i) && dat.playerOnSeat(i).getSocket().equals(socket)) return;
                }
            
                dat.setSeatOccupied(index, new PlayerData(5000, new Cards(2), socket));
            
                connectedSockets.stream().filter((s) -> (!s.equals(socket))).map((s) -> {
                    sendCommand("seattaken:"+index, s);
                    return s;
                }).forEachOrdered((s) -> {
                    try {
                        s.write(dat.playerOnSeat(index));
                    } catch(IOException e) {
                        System.err.println("Exception when sending new player data");
                        System.err.println(e);
                    }
                });
                
                sendCommand("sit:"+index, socket);
                try {
                    socket.write(dat.playerOnSeat(index));
                } catch(IOException e) {
                    System.err.println("Exception when sending new player data");
                    System.err.println(e);
                }
                
            }
            if(subcommand.equals("standup")) {
                Integer index = Integer.parseInt(command.substring(command.indexOf(':')+1));
                if(dat.seatIsOccupied(index) && dat.playerOnSeat(index).getSocket().equals(socket)) {
                    dat.setSeatFree(index);
                    sendCommandToAll("seatvacated:" + index);
                }
            }
        }
    }
    
    void processGameCommand(String command, ClientSocket socket) {
        //System.out.println("Recieved game command: "+command);
        
        if(!command.contains(":")) return;

        String substring = command.substring(0, command.indexOf(":")+1);        //should be fold:, check:, call:, or raise:
        //System.out.println(substring);
        
        String upperString = command.substring(substring.length());
        
        //System.out.println(substring);
        
        if(substring.equals("fold:")) {
            Integer index = Integer.parseInt(upperString);
            //System.out.println("fold "+index);
            if(dat.seatIsOccupied(index) && dat.playerOnSeat(index).getSocket().equals(socket) && dat.playerOnSeat(index).isInHand()) {
                dat.playerOnSeat(index).setAction(PokerAction.FOLD);
            }
            return;
        }
        if(substring.equals("check:")) {
            Integer index = Integer.parseInt(upperString);
            //System.out.println("check "+ index);
            if(dat.seatIsOccupied(index) && dat.playerOnSeat(index).getSocket().equals(socket) && dat.playerOnSeat(index).isInHand()) {
                dat.playerOnSeat(index).setAction(PokerAction.CHECK);
            }
            return;
        }
        
        if(substring.equals("call:")) {
            Integer index = Integer.parseInt(upperString);
            //System.out.println("call "+index);
            //System.out.println("call, "+index);
            //System.out.println(dat.seatIsOccupied(index)+", "+dat.playerOnSeat(index).getSocket().equals(socket)+", "+dat.playerOnSeat(index).isInHand());
            if(dat.seatIsOccupied(index) && dat.playerOnSeat(index).getSocket().equals(socket) && dat.playerOnSeat(index).isInHand()) {
                //System.out.println("Setting action");
                dat.playerOnSeat(index).setAction(PokerAction.CALL);
            }
            return;
        }
        
        if(!upperString.contains(":")) return;
        
        if(substring.equals("raise:")) {
            substring = upperString.substring(0, upperString.indexOf(":"));       
            upperString = upperString.substring(substring.length()+1);
            Integer index = Integer.parseInt(substring);
            Integer amount = Integer.parseInt(upperString);
            if(dat.seatIsOccupied(index) && dat.playerOnSeat(index).getSocket().equals(socket) && dat.playerOnSeat(index).isInHand()) {
                dat.playerOnSeat(index).setAction(PokerAction.RAISE);
                dat.playerOnSeat(index).raise = amount;
            }
            //return;
        } 
    }
    
    void sendCommandToAll(String command) {
        connectedSockets.forEach((socket) -> {
            sendCommand(command, socket);
        });
    }
    void sendCommandToPlayers(String command) {
        dat.getPlayers().forEach((player) -> {
           sendCommand(command, player.getSocket()); 
        });
    }
    void sendCommand(String command, ClientSocket socket) {
        try {
            socket.write("ID:"+getTableID()+":"+command);
        } catch(IOException e) {
            System.err.println("Exception occured when trying to send command");
            System.err.println(e);
        }
    }
    
    private int bettingRound(Integer i, Integer bet) throws IllegalMonitorStateException, InterruptedException {
        Integer endIndex = i;
        do {
            do {
                i--;
                if(i<0) i+= dat.getMaxHands();
            } while(!dat.seatIsOccupied(i) || !dat.playerOnSeat(i).isInHand());
                    
            sendCommandToAll("game:seat:"+i+":toact:"+(bet-dat.playerOnSeat(i).currentBet));

            synchronized(dat.playerOnSeat(i)) {
                dat.playerOnSeat(i).wait(21000);
            }
            
            if(dat.getPlayersInHand() == 1) 
                return 1;
            
            if(!dat.seatIsOccupied(i) || !dat.playerOnSeat(i).isInHand()) {
                continue;
            }
            
            if(dat.playerOnSeat(i).getAction().equals(PokerAction.NONE)) {
                dat.playerOnSeat(i).setAction(PokerAction.FOLD);
                //TODO check if possible to do so
            }
            
            if(dat.playerOnSeat(i).getAction().equals(PokerAction.CALL)) {
                dat.addToPot(bet - dat.playerOnSeat(i).currentBet);
                dat.playerOnSeat(i).chipCount = dat.playerOnSeat(i).chipCount + dat.playerOnSeat(i).currentBet - bet;
                dat.playerOnSeat(i).currentBet = bet;
                sendCommandToAll("game:seat:"+i+":action:"+dat.playerOnSeat(i).getAction() + ":"+bet);
            } else if(dat.playerOnSeat(i).getAction().equals(PokerAction.RAISE)) {
                dat.addToPot(bet + dat.playerOnSeat(i).raise - dat.playerOnSeat(i).currentBet);
                dat.playerOnSeat(i).chipCount = dat.playerOnSeat(i).chipCount + dat.playerOnSeat(i).currentBet - bet - dat.playerOnSeat(i).raise;
                dat.playerOnSeat(i).currentBet = bet + dat.playerOnSeat(i).raise;
                bet = dat.playerOnSeat(i).currentBet;
                sendCommandToAll("game:seat:"+i+":action:"+dat.playerOnSeat(i).getAction() + ":"+bet);
            } else {
                //i.e Fold or Check
                sendCommandToAll("game:seat:"+i+":action:"+dat.playerOnSeat(i).getAction());
            }
                    
            
            
            if(dat.playerOnSeat(i).getAction().equals(PokerAction.FOLD)) {
                if(dat.removePlayerFromHand(i) == 1) {
                    return 1;
                }
            }
            
            if(dat.playerOnSeat(i).getAction().equals(PokerAction.RAISE)) {
                endIndex = i;
                do {
                    endIndex++;
                    if(endIndex>=dat.getMaxHands()) endIndex-= dat.getMaxHands();
                } while(!dat.seatIsOccupied(endIndex) || !dat.playerOnSeat(endIndex).isInHand());
            }
                    
            //System.out.println(dat.playerOnSeat(i).getAction());
            dat.playerOnSeat(i).setAction(PokerAction.NONE);
                    
        } while(!i.equals(endIndex));
        
        return dat.getPlayersInHand();
    }
    
    private void onePlayerLeft() throws InterruptedException {
        //System.out.println("Game is over because everyone folded");
        /* Find the index of the one remaining player */ 
        for(Map.Entry<Integer, PlayerData> player :  dat.getPlayerAndIndex().entrySet()) {
            if(player.getValue().isInHand()) {
                sendCommandToAll("game:winner:"+player.getKey()+":"+dat.getPot());
                player.getValue().chipCount += dat.getPot();
                break;
            }
        } 
                        
        Thread.sleep(2000);
        sendCommandToAll("game:end");
    }
    
    private void newRound() throws InterruptedException {
        sendCommandToAll("game:rounddone:"+dat.getPot());
                
        for(Map.Entry<Integer, PlayerData> player : dat.getPlayerAndIndex().entrySet()) {
            if(player.getValue().isInHand()) {
                player.getValue().currentBet = 0;
                //System.out.println("Player "+player.getKey()+" has "+player.getValue().chipCount+" chips");
            }
        }

        dat.updateStageOfPlay();
        Thread.sleep(2000);
    }
    
    private void runGame() {
        while (true) {
            dat.setGameRunning(false);

            while(dat.getNoPlayers()<2) {
                try {
                    synchronized(dat) {
                        dat.wait();
                    }
                } catch(InterruptedException e) {
                    System.err.println("game thread was interrupted");
                    System.err.println(e);
                }
            }
       
            try {
                
                //Nothing wrong with the sleeps in this main loop
                
                //System.out.println("Starting game...");
                Thread.sleep(2000); 

                Integer dealer = dat.getDealer();
                //<editor-fold defaultstate="collapsed" desc="Finds the dealer, sb and bb positions">
                do {
                    dealer--;
                    if(dealer < 0) dealer += dat.getMaxHands();
                    //System.out.println(dat.dealer);
                } while (!dat.seatIsOccupied(dealer));
                Integer smallBlind = dealer;
                do {
                    smallBlind--;
                    if(smallBlind < 0) smallBlind += dat.getMaxHands();
                    
                } while(!dat.seatIsOccupied(smallBlind));
                Integer bigBlind = smallBlind;
                do {
                    bigBlind--;
                    if(bigBlind < 0) bigBlind += dat.getMaxHands();
                } while(!dat.seatIsOccupied(bigBlind));
                
                //</editor-fold>
                
                dat.setDealer(dealer);
                dat.setSmallBlind(smallBlind);
                dat.setBigBlind(bigBlind);
                
                Thread.sleep(2000);
            
                sendCommandToAll("game:start:dealer:"+dat.getDealer()+":small:"+dat.getSmallBlind()+":big:"+dat.getBigBlind());
                dat.setGameRunning(true);
                dat.setPlayersInHand(dat.getNoPlayers());

                //<editor-fold defaultstate="collapsed" desc="Deal all of the cards, and send them to the players">
                //Map<Integer, Cards> hands = new HashMap<>();
                Deck d = new Deck();
                
                Thread.sleep(1000);
                
                for(Map.Entry<Integer, PlayerData> player : dat.getPlayerAndIndex().entrySet()) {
                    //hands.put(player.getKey(), new Cards(2));
                    //player.getValue().resetHand();
                    
                    player.getValue().setHand(d.drawCard(), Card.EMPTY_CARD);
                    
                    for(ClientSocket sock : connectedSockets) {
                        if(!sock.equals(player.getValue().getSocket())) {
                            sendCommand("game:seat:"+player.getKey()+":card:0:null", sock);
                        } else {
                            //sendCommand("game:seat:"+player.getKey()+":card:0:"+hands.get(player.getKey()).getCard(0), player.getValue().getSocket());
                            sendCommand("game:seat:"+player.getKey()+":card:0:"+player.getValue().getHand().getCard(0), player.getValue().getSocket());
                        }
                    }
                }
                for(Map.Entry<Integer, PlayerData> player : dat.getPlayerAndIndex().entrySet()) {
                    //hand.getValue().setCard(d.drawCard(), 1);
                    player.getValue().getHand().setCard(d.drawCard(), 1);
                    
                    for(ClientSocket sock : connectedSockets) {
                        //if(!sock.equals(dat.playerOnSeat(hand.getKey()).getSocket())) {
                        if(!sock.equals(player.getValue().getSocket())) {
                            //sendCommand("game:seat:"+hand.getKey()+":card:1:null", sock);
                            sendCommand("game:seat:"+player.getKey()+":card:1:null", sock);
                        } else {
                            //sendCommand("game:seat:"+hand.getKey()+":card:1:"+hand.getValue().getCard(1), sock);
                            sendCommand("game:seat:"+player.getKey()+":card:1:"+player.getValue().getHand().getCard(1),sock);
                        }
                    }
                    
                    //dat.playerOnSeat(hand.getKey()).setInHand(true);
                    player.getValue().setInHand(true);
                    player.getValue().setAction(PokerAction.NONE);
                    //dat.playerOnSeat(hand.getKey()).setAction(PokerAction.NONE);
                }
                //</editor-fold>
                
                for(Map.Entry<Integer, PlayerData> player : dat.getPlayerAndIndex().entrySet()) {
                    if(player.getValue().isInHand()) {
                        //System.out.println(player.getValue().chipCount.toString());
                        player.getValue().currentBet = 0;
                        if(player.getKey().equals(dat.getBigBlind())) {
                            player.getValue().currentBet = 200;
                            player.getValue().chipCount-=200;
                        }
                        if(player.getKey().equals(dat.getSmallBlind())) {
                            player.getValue().currentBet= 100;
                            player.getValue().chipCount-=100;
                        }
                    }
                }
                dat.setPot(300);
                
                if(bettingRound(dat.getBigBlind(), 200) == 1) {
                    onePlayerLeft();
                    continue;
                }
                
                newRound();
                
                dat.resetBoard();
                d.drawCard();
                dat.setBoardCard(d.drawCard(), 0);
                dat.setBoardCard(d.drawCard(), 1);
                dat.setBoardCard(d.drawCard(), 2);
            
                sendCommandToAll("game:flop:"+dat.getBoardCard(0)+":"+dat.getBoardCard(1)+":"+dat.getBoardCard(2));  
                
                if(bettingRound(dat.getDealer(), 0) == 1) {
                    onePlayerLeft();
                    continue;
                }
                
                newRound();

                d.drawCard();
                dat.setBoardCard(d.drawCard(), 3);
            
                sendCommandToAll("game:turn:"+dat.getBoardCard(3));
                
                if(bettingRound(dat.getDealer(), 0) == 1) {
                    onePlayerLeft();
                    continue;
                }
                
                newRound();
            
                d.drawCard();
                dat.setBoardCard(d.drawCard(), 4);
            
                sendCommandToAll("game:river:"+dat.getBoardCard(4));
                
                if(bettingRound(dat.getDealer(), 0) == 1) {
                    onePlayerLeft();
                    continue;
                }      
                
                newRound();
                
                //System.out.println("Board is "+dat.getBoard());

                ScoredBoard bestScoredBoard = null;
                Integer bestIndex = 0;
                
                for(Map.Entry<Integer, PlayerData> player : dat.getPlayerAndIndex().entrySet()) {
                    if(player.getValue().isInHand()) {
                        //System.out.println(player.getKey());
                        ScoredBoard newScoredBoard = Poker.getBestBoard(player.getValue().getHand(), dat.getBoard());
                        //System.out.println(newScoredBoard);
                        if(bestScoredBoard == null || newScoredBoard.getScore().compareTo(bestScoredBoard.getScore()) > 0) {
                            bestScoredBoard = newScoredBoard;
                            bestIndex = player.getKey();
                        }
                    }
                }
            
                //System.out.println("Player "+bestIndex+" wins, with "+bestScoredBoard.toString());
                
                sendCommandToAll("game:winner:"+bestIndex+":"+dat.getPot()+":"+bestScoredBoard.toString());
                
                dat.playerOnSeat(bestIndex).chipCount += dat.getPot();
            
                dat.updateStageOfPlay();
                sendCommandToAll("game:end");
            
            } catch(DrawCardException | InterruptedException | IllegalMonitorStateException e) {
                System.err.println("Exception occured when playing the game");
                System.err.println(e);
            }
        }
    }
    
    int getNoHands() {
        return dat.getMaxHands();
    }
    Integer getTableID() {
        return getData().getTableID();
    }
    PokerTableData getData() {
        return dat;
    }
    
    public void addSocket(ClientSocket s) {
        connectedSockets.add(s);
    }
    public void removeSocket(ClientSocket s) {
        connectedSockets.remove(s);
    }
    public List<ClientSocket> getSockets() {
        return connectedSockets;
    }
    public boolean hasSocket(ClientSocket s) {
        return connectedSockets.contains(s);
    }
}
