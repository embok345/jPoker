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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import space.poulter.poker.Deck.DrawCardException;

/**
 *
 * @author Em Poulter
 */
public class Cards implements Iterable<Card>, Serializable {
    
    private final List<Card> cards;
    
    public Cards(Cards old) {
        cards = new ArrayList<>(old.size());
        for(Card c : old) {
            cards.add(new Card(c.getValue(), c.getSuit()));
        }
    }
    
    public Cards(List<Card> cards) {
        this.cards = cards;
    }

    public Cards(Card... cards) {
        this.cards = Arrays.asList(cards);
    }

    public Cards(Integer noCards) {
        cards = new ArrayList<>(noCards);
        for(Integer index = 0; index<noCards; index++) {
            cards.add(index, Card.EMPTY_CARD);
        }
    }
    
    public Cards(Deck d, Integer noCards) throws DrawCardException {
        cards = new ArrayList<>(noCards);
        for(Integer index = 0; index<noCards; index++) {
            cards.add(index, d.drawCard());
        }
    }
    
    public List<Card> getCards() {
        return this.cards;
    }
    public Card getCard(int i) {
        return this.cards.get(i);
    }
    
    public void setCard(Card c, int i) {
        cards.set(i, c);
    }
    
    @Override
    public String toString() {
        String s = "";
        s = this.cards.stream().map((c) -> c.toString() + ',').reduce(s, String::concat); 
        return s.substring(0, s.length()-1);
    }

    @Override
    public Iterator iterator() {
        return cards.iterator();
    }

    public boolean isComplete() {
        return !cards.contains(Card.EMPTY_CARD);
    }
    public Integer size() {
        return cards.size();
    }
    public List<Integer> values() {
        List<Integer> values = new ArrayList<>();
        cards.forEach((c) -> {
            values.add(c.getValue());
        });
        return values;
    }
}
