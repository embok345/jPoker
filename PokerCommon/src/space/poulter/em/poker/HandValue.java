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

/**
 *
 * @author Em Poulter
 */
public class HandValue implements Comparable{
    private int rank;
    private int score;
    
    public HandValue(int rank, int score) {
        if(rank>8 || rank < 0) {
            System.err.println("Invalid rank set: "+rank);
            this.rank = 0;
        } else {
            this.rank = rank;
        }
        if(score < 0) {
            System.err.println("Invalid score set: "+score);
            this.score = 0;
        } else {
            this.score = score;
        }
    }
    public HandValue() {}
    
    public void setRank(int rank) {
        if(rank > 8 || rank < 0) {
            System.err.println("Invalid rank set: "+rank);
            this.rank = 0;
            return;
        }
        this.rank = rank;
    }
    public void setScore(int score) {
        if(score < 0) {
            System.err.println("Invalid score set: "+score);
            this.score = 0;
            return;
        }
        this.score = score;
    }
    
    public int getRank() {
        return this.rank;
    }
    public int getScore() {
        return this.score;
    }
    
    @Override
    public String toString() {
        String out = "";
        switch(this.getRank()) {
            case 0: out+="High Card ";
                    int high = this.score/(14*14*14*14);
                    return out += Card.valueToChar(high);
            case 1: out+="Pair of ";
                    int pair = this.score/(14*14*14);
                    int kicker = (this.score/(14*14))%14;
                    if(kicker == 0) {
                        pair--;
                        kicker+=14;
                    }
                    return out+=Card.valueToChar(pair)+"s";
            case 2: out+="Two Pair, ";
                    int pair1 = this.score/(14*14);
                    int pair2 = (this.score%(14*14))/14;
                    kicker = (this.score%(14));
                    if(kicker == 0) {
                        pair2--;
                        kicker += 14;
                    }
                    return out+=Card.valueToChar(pair1)+"s and "+Card.valueToChar(pair2)+"s";
            case 3: out += "Three of a kind, ";
                    int set = this.score/(14*14);
                    kicker = (this.score/14)%14;
                    if(kicker == 0) {
                        set--;
                        kicker+=14;
                    }
                    return out+=Card.valueToChar(set)+"s";
            case 4: return out += Card.valueToChar(this.score) + "-high straight";
            case 5: out += "Flush";
                    high = this.score/(14*14*14*14);
                    return Card.valueToChar(high)+"-high flush";
            case 6: out += "Full house";
                    set = this.score/14;
                    pair = this.score%14;
                    if(pair == 0) {
                        set--;
                        pair+=14;
                    }
                    return out += ", " + Card.valueToChar(set)+"s full of "+Card.valueToChar(pair) + "s";
            case 7: out += "Four of a kind";
                    int fours = this.score/14;
                    kicker = this.score%14;
                    if(kicker == 0) {
                        //Then we have an ace kicker
                        fours--;
                        kicker += 14;
                    }
                    return out += ", "+Card.valueToChar(fours);
            case 8: 
                    return out += Card.valueToChar(this.score) + " high straight flush";
            default: 
                    return "Not a valid hand";
        }
    }

    @Override
    public boolean equals(Object o) {
        if(o.getClass()!=this.getClass()) {
            return false;
        } 
        HandValue h = (HandValue)o;
        return !(h.getRank()!=this.getRank() || h.getScore()!=this.getScore());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + this.rank;
        hash = 79 * hash + this.score;
        return hash;
    }
    
    @Override
    public int compareTo(Object o) {
        if(o.getClass()!=this.getClass()) {
            throw new ClassCastException("Could not compare hand values; one is not of correct class");
        }
        HandValue h = (HandValue)o;
        if(this.getRank() < h.getRank()) {
            return -1;
        }
        if(this.getRank() > h.getRank()) {
            return 1;
        }
        if(this.getScore() < h.getScore()) {
            return -1;
        }
        if(this.getScore() > h.getScore()) {
            return 1;
        }
        
        return 0;
    }
}
