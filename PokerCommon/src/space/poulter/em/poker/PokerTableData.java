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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author Em Poulter
 */
public class PokerTableData implements Serializable {
    
    
    private volatile BiMap<Integer, PlayerData> players;
    private boolean gameIsRunning;
    /* stageOfPlay = -1 : not playing
     *                0 : before playing
     *                1 : hole cards dealt
     *                2 : flop
     *                3 : turn
     *                4 : river
     *                5 : showdown
     */
    private Integer stageOfPlay;
    
    private Integer playersInHand;
    
    private Integer pot;
    private Integer bet;
    
    private Integer dealer;
    private Integer smallBlind;
    private Integer bigBlind;
    
    private Cards b;
    
    private Integer tableID;
    private Integer maxHands;
    
    public void init(int id, int hands) {
        tableID = id;
        maxHands = hands;
        gameIsRunning = false;
        players = HashBiMap.create();
        stageOfPlay = -1;
        b = new Cards(5);
        dealer = 0;
        smallBlind = 0;
        bigBlind = 0;
        pot = 0;
        bet = 0;
        playersInHand = 0;
    }
    
    public void setTableID(Integer value) { 
        tableID = value;
    }
    public Integer getTableID() { return tableID; }

    public void setMaxHands(Integer value) { 
        maxHands = value;
    }
    public Integer getMaxHands() { return maxHands; }
    
    public void resetBoard() {
        b = new Cards(5);
    }
    public void setBoardCard(Card c, Integer i) {
        b.setCard(c, i);
    }
    public Card getBoardCard(Integer i) {
        return b.getCard(i);
    }
    public void setBoardCards(Cards cards) {
        if(cards.size() < 5) {
            Cards newCards = new Cards(5);
            int i=0;
            for(Card c : cards) {
                newCards.setCard(c, i++);
            }
            b = newCards;
        } else {
            b = cards;
        }
        
    }
    public Cards getBoard() {
        return b;
    }
    
    public PlayerData playerOnSeat(Integer index) {
        if(seatIsOccupied(index)) {
            return players.get(index);
        }
        return null;
    }
    public boolean seatIsOccupied(Integer index) {
        //return occupiedSeats.contains(index);
        return players.containsKey(index);
    }
    public void setSeatOccupied(Integer index, PlayerData dat) {
        if(!seatIsOccupied(index)) {
            players.put(index, dat);
            synchronized(this) {
                this.notify();
            }
        }
    }
    public void setSeatFree(Integer index) {
         if(seatIsOccupied(index)) {
            if(players.get(index).isInHand()) {
                players.get(index).setAction(Poker.PokerAction.FOLD);
                players.get(index).setInHand(false);
                playersInHand--;
            }
            players.remove(index);
        }
    }
    public boolean isGameRunning() {
        return gameIsRunning;
    }
    public void setGameRunning(boolean isGameRunning) {
        gameIsRunning = isGameRunning;
        if(gameIsRunning) 
            stageOfPlay = 0;
        else 
            stageOfPlay = -1;
        
    }
    public void setStageOfPlay(int stage) {
        stageOfPlay = stage;
    }
    public void updateStageOfPlay() {
        stageOfPlay = (stageOfPlay+1)%6;
        
    }
    public Integer getStageOfPlay() {
        return stageOfPlay;
    }
    
    public BiMap<Integer, PlayerData> getPlayerAndIndex() {
        return players;
    }
    public void setPlayers(BiMap<Integer, PlayerData> newPlayers) {
        players = newPlayers;
    }
    public Set<PlayerData> getPlayers() {
        //return players
        return players.inverse().keySet();
    }
    public Integer getNoPlayers() {
        return players.size();
    }
    public void setPlayersInHand(int noPlayers) {
        playersInHand = noPlayers;
    }
    public Integer getPlayersInHand() {
        return playersInHand;
    }
    public int removePlayerFromHand(Integer i) {
        
        if(seatIsOccupied(i) && playerOnSeat(i).isInHand()) {
            playerOnSeat(i).setInHand(false);
            playersInHand--;
        }
        
        return getPlayersInHand();
        
    }    
    public Integer getPot() {
        return pot;
    }

    public Integer getBet() {
        return bet;
    }

    public Integer getDealer() {
        return dealer;
    }

    public Integer getSmallBlind() {
        return smallBlind;
    }

    public Integer getBigBlind() {
        return bigBlind;
    }

    
    public void addToPot(Integer add) {
        pot += add;
    }
    public void setPot(Integer pot) {
        this.pot = pot;
    }

    public void setBet(Integer bet) {
        this.bet = bet;
    }

    public void setDealer(Integer dealer) {
        this.dealer = dealer;
    }

    public void setSmallBlind(Integer smallBlind) {
        this.smallBlind = smallBlind;
    }

    public void setBigBlind(Integer bigBlind) {
        this.bigBlind = bigBlind;
    }
    
    
    
    @Override
    public String toString() {
        return "ID:"+getTableID() + ", MaxHands:"+getMaxHands();
    }
    
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof PokerTableData)) return false;
        PokerTableData dat = (PokerTableData)o;
        return getTableID().equals(dat.getTableID());
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.tableID);
        return hash;
    }
    
    

    
}
