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
 * 
 * @author Em Poulter
 */
public class Card implements Serializable {
    private final int value;
    private final char suit;
    
    public static final Card EMPTY_CARD = new Card();
    
    public Card() {
        value = 0;
        suit = 0;
    }
    
    public Card(String str) {
        if(str.equals("null") || str.length()==1 || str.length()>2) {
            value = 0;
            suit = 0;
        } else {
            if(Card.charToValue(str.charAt(0))<0 || Card.charToValue(str.charAt(0))>14) value = 0;
            else value = Card.charToValue(str.charAt(0));
            
            switch(str.codePointAt(1)) {
                case 'S':
                case 's':
                case 0x2660:
                    suit = 'S';
                    break;
                case 'H':
                case 'h':
                case 0x2665:
                    suit = 'H';
                    break;
                case 'C':
                case 'c':
                case 0x2663:
                    suit = 'C';
                    break;
                case 'D':
                case 'd':
                case 0x2666:
                    suit = 'D';
                    break;
                default:
                    suit = 0;
            }
            
        }
        
    }
    
    public Card(int value, char suit) {
        if(value>14 || value<0) {
            this.value = 0;
            this.suit = 0;
            return;
        }
        else this.value = value;
        
        if(suit=='H' || suit=='S' || suit=='D' || suit=='C') this.suit = suit;
        else this.suit = 0;
    }
    
    public int getValue() {
        return this.value;
    }
    public char getSuit() {
        return this.suit;
    }
    
    @Override
    public String toString() {
        String s = "";
        switch(this.value) {
            case 10: s+='T';
                     break;
            case 11: s+='J';
                     break;
            case 12: s+='Q';
                     break;
            case 13: s+='K';
                     break;
            case 14: s+='A';
                     break;
            default: s+=Integer.toString(this.value);
        }
        
        switch(this.suit) {
            case 'S': s+=new String(Character.toChars(Integer.parseInt("2660", 16)));
                      break;
            case 'H': s+=new String(Character.toChars(Integer.parseInt("2665", 16)));
                      break;
            case 'C': s+=new String(Character.toChars(Integer.parseInt("2663", 16)));
                      break;
            case 'D': s+=new String(Character.toChars(Integer.parseInt("2666", 16)));
        }
        
        
        return s;
    }
    
    public static char valueToChar(int i) {
        switch(i) {
            case 14: return 'A';
            case 13: return 'K';
            case 12: return 'Q';
            case 11: return 'J';
            case 10: return 'T';
            default: return Integer.toString(i).charAt(0);
        }
    }
    
    public static int charToValue(char c) {
        if(c == 'A') return 14;
        if(c == 'K') return 13;
        if(c == 'Q') return 12;
        if(c == 'J') return 11;
        if(c == 'T') return 10;
        return Integer.parseInt(String.valueOf(c));
    }
    
    @Override
    public boolean equals(Object o) {
        if(o.getClass() != this.getClass()) {
            return false;
        }
        Card c = (Card)o;
        if(c.getValue() != this.getValue()) {
            return false;
        }
        
        return c.getSuit()==this.getSuit();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + this.value;
        hash = 37 * hash + this.suit;
        return hash;
    }
    
    
}
