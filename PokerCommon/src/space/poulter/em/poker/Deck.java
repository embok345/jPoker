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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Em Poulter
 */
public class Deck {
    
    public class DrawCardException extends Exception {
        public DrawCardException(String message) {
            super(message);
        }
    }
    
    private List<Card> liveDeck;
    private List<Card> deadDeck;
    private Random rand;
    
    public Deck() {
        rand = new Random();
        this.liveDeck = new ArrayList<>();
        this.deadDeck = new ArrayList<>();
        
        for(int i = 0; i<4; i++) {
            char suit=' ';
            switch(i) {
                case 0: suit = 'S';
                        break;
                case 1: suit = 'H';
                        break;
                case 2: suit = 'D';
                        break;
                case 3: suit = 'C';
            }
            for(int j = 2; j<=14; j++) {
                this.liveDeck.add(new Card(j, suit));
            }
        }
    }
    
    public List<Card> getDeck() {
        return this.liveDeck;
    }
    
    public Card drawCard() throws DrawCardException {
        if(liveDeck.isEmpty()) {
            throw new DrawCardException("Could not draw card: deck is empty!");
        }
        int i = rand.nextInt(liveDeck.size());
        Card c = liveDeck.get(i);
        if(!liveDeck.remove(c)) {
            throw new DrawCardException("Could not remove card from live deck");
        }
        if(deadDeck.contains(c)) {
            throw new DrawCardException("Drawn card already exists in dead deck");
        }
        if(!deadDeck.add(c)) {
            throw new DrawCardException("Could not add card to drawn deck");
        }
        return c;
    }
}
