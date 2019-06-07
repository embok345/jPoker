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

package space.poulter.poker;

import java.io.Serializable;

/**
 * @author Em Poulter
 */
public class PlayerData implements Serializable {
    //private transient PokerSocket clientSocket;

    private final transient PokerSocket sock;
    public Integer chipCount;
    public Integer currentBet;
    public Integer raise;
    private transient Cards hand;
    private volatile boolean inHand;
    private volatile Util.PokerAction playerAction;

    public PlayerData(Integer chips, Cards h) {
        //clientSocket = s;
        chipCount = chips;
        currentBet = 0;
        hand = h;
        sock = null;
        inHand = false;
        playerAction = Util.PokerAction.NONE;
    }

    public PlayerData(Integer chips, Cards h, PokerSocket sock) {
        chipCount = chips;
        hand = h;
        this.sock = sock;
        inHand = false;
        playerAction = Util.PokerAction.NONE;
    }

    public void resetHand() {
        hand = new Cards(2);
    }

    public void setHand(Card c1, Card c2) {
        hand = new Cards(c1, c2);
    }

    public Cards getHand() {
        return hand;
    }

    public void setHand(Cards h) {
        hand = h;
    }

    public boolean isInHand() {
        return inHand;
    }

    public void setInHand(boolean inHand) {
        this.inHand = inHand;
    }

    public Util.PokerAction getAction() {
        return playerAction;
    }

    public void setAction(Util.PokerAction newAction) {
        playerAction = newAction;
        try {
            synchronized (this) {
                this.notify();
            }
        } catch (IllegalMonitorStateException ex) {
            System.err.println("Exception when notifying of action");
        }
    }

    public Integer getChips() {
        return chipCount;
    }

    public PokerSocket getSocket() {
        return sock;
    }

}
