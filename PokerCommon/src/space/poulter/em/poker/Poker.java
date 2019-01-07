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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Em Poulter
 */
public class Poker {
    
    public static enum PokerAction {
        CALL, CHECK, FOLD, RAISE, NONE;
    }
    
    public static class ScoredBoard {
        final private Cards b;
        final private HandValue v;
    
        public ScoredBoard(Cards b, HandValue v) {
            this.b = b;
            this.v = v;
        }
    
        public HandValue getScore() {
            return this.v;
        }
        public Cards getBoard() {
            return this.b;
        }
    
        @Override
        public String toString() {
            String out = "";
            out+=this.b.toString();
            out+="\n";
            out+=this.v.toString();
            return out;
        }
    }

    static public ScoredBoard getBestBoard(Cards hand, Cards board) {
        
        if(board.size()<5 || hand.size()<2 || !board.isComplete() || !hand.isComplete()) return null;
        
        Cards bestBoard = new Cards(board);
        Cards newBoard;
        
        HandValue bestRanking = Poker.getHandRanking(board);
        HandValue newRanking;
        
        for(int i = 0; i<5; i++) {

            newBoard = new Cards(board);
            newBoard.setCard(hand.getCard(0), i);

            newRanking = Poker.getHandRanking(newBoard);
            
            System.out.println(newBoard);
            
            if(newRanking.compareTo(bestRanking) > 0) {
                bestRanking = newRanking;
                bestBoard = newBoard;
            }
            
            for(int j = 0; j<5; j++) {
                newBoard = new Cards(board);
                if(i != j) {
                    newBoard.setCard(hand.getCard(0), i);
                }
                newBoard.setCard(hand.getCard(1), j);
                System.out.println(newBoard);
                newRanking = Poker.getHandRanking(newBoard);
                if(newRanking.compareTo(bestRanking) > 0) {
                    bestRanking = newRanking;
                    bestBoard = newBoard;
                }
            }
        }
        
        return new ScoredBoard(bestBoard, bestRanking);
    }

    static public int flushValue(Cards cards) {
        if(cards.size()<5 || !cards.isComplete()) return -1;
        Set<Character> suits = new HashSet<>();
        int j = 0;
        for(Card c : cards) {
            if(suits.add(c.getSuit())) {
                if(++j > 1) return -1;
            }
        }
        return 1;
    }
    
    static public int highHandValue(Cards cards) {
        if(cards.size()<5 || !cards.isComplete()) return -1;
        List<Integer> cardValues = new ArrayList<>();
        for(Card c : cards) {
            cardValues.add(c.getValue());
        }
        Collections.sort(cardValues);
        return cardValues.get(0) + (cardValues.get(1)*14) + (cardValues.get(2)*14*14) + (cardValues.get(3)*14*14*14) + (cardValues.get(4)*14*14*14*14);
    }

    static public int straightValue(Cards cards) {
        if(cards.size()<5 || !cards.isComplete()) return -1;
        List<Integer> cardValues = cards.values();
        Integer previous = -1;
        for(Integer i : cardValues) {
            if(previous == -1) {
                previous = i;
                continue;
            }
            if(i != previous+1) {
                if(i == 14 && previous == 5 && cardValues.indexOf(i) == 4) {
                    return 5;
                }
                return -1;
            }
            previous = i;
        }
        return cardValues.get(4);
        
    }
    
    static public HandValue getHandRanking(Cards cards) {
        
        if(cards.size()<5 || !cards.isComplete()) return null;
        
        HandValue handValue = new HandValue();
        List<Integer> cardValues = new ArrayList<>();
        for(Card c : cards) {
            cardValues.add(c.getValue());
        }
        Collections.sort(cardValues);
        Set<Integer> cardValuesSet = new HashSet<>(cardValues);
        if(cardValuesSet.size()<cardValues.size()) {
            if(cardValuesSet.size() == 2) {
                int i = 1;
                Integer in = cardValues.get(0);
                for(int j = 1; j<5; j++) {
                    if(!in.equals(cardValues.get(j))) {
                        break;
                    }
                    i++;
                }
                
                if(i == 1 || i==2) {
                    //quad, with kicker, quad; or,
                    //full, with pair, set
                    handValue.setScore(cardValues.get(0) + (14*cardValues.get(4)));
                }
                if(i == 3 || i==4) {
                    //full, with set, pair; or,
                    //quad, with quad, kicker
                    handValue.setScore((14*cardValues.get(0)) + cardValues.get(4));
                }
                if(i==2 || i==3) {
                    handValue.setRank(6);
                }
                if(i==1 || i==4) {
                    handValue.setRank(7);
                }
                return handValue;
            }
            if(cardValuesSet.size() == 3) {
                int i = 1;
                Integer in = cardValues.get(0);
                for(int j = 1; j<5; j++) {
                    if(!in.equals(cardValues.get(j))) {
                        break;
                    }
                    i++;
                }
                if(i == 3) {
                    //set, kicker, kicker
                    handValue.setScore((cardValues.get(0)*14*14) + (cardValues.get(3)) + (cardValues.get(4)*14));
                    handValue.setRank(3);
                    return handValue;
                }
                if(i == 2) {
                    //pair, something
                    in = cardValues.get(2);
                    i = 1;
                    for(int j = 3; j<5; j++) {
                        if(!in.equals(cardValues.get(j))) {
                            break;
                        }
                        i++;
                    }
                    if(i == 1) {
                        //pair, kicker, pair
                        handValue.setScore((cardValues.get(0)*14) + cardValues.get(2) + (cardValues.get(3)*14*14));
                        handValue.setRank(2);
                        return handValue;
                    }
                    if(i == 2) {
                        //pair, pair, kicker
                        handValue.setScore((cardValues.get(0)*14) + (cardValues.get(2)*14*14) + cardValues.get(4));
                        handValue.setRank(2);
                        return handValue;
                    }
                }
                if(i == 1) {
                    //kicker, something
                    in = cardValues.get(1);
                    for(int j = 2; j<5; j++) {
                        if(!in.equals(cardValues.get(j))) {
                            break;
                        }
                        i++;
                    }
                    if(i == 1) {
                        //kicker, kicker, set
                        handValue.setScore(cardValues.get(0) + (cardValues.get(1)*14) + (cardValues.get(2)*14*14));
                        handValue.setRank(3);
                        return handValue;
                    }
                    if(i == 2) {
                        //kicker, pair, pair
                        handValue.setScore(cardValues.get(0) + (cardValues.get(1)*14) + (cardValues.get(3)*14*14));
                        handValue.setRank(2);
                        return handValue;
                    }
                    if(i == 3) {
                        //kicker, set, kicker
                        handValue.setScore(cardValues.get(0) + (cardValues.get(1)*14*14) + (cardValues.get(4)*14));
                        handValue.setRank(2);
                        return handValue;
                    }
                    
                }
                
            }
            
            if(cardValues.get(0).equals(cardValues.get(1))) {
                //pair, k, k, k
                handValue.setScore((cardValues.get(0)*14*14*14) + cardValues.get(2) + (cardValues.get(3)*14) + (cardValues.get(4)*14*14));
            } else if(cardValues.get(1).equals(cardValues.get(2))) {
                //k, pair, k, k
                handValue.setScore(cardValues.get(0) + (cardValues.get(1)*14*14*14) + (cardValues.get(3)*14) + (cardValues.get(4)*14*14));
            } else if(cardValues.get(2).equals(cardValues.get(3))) {
                //k, k, pair, k
                handValue.setScore(cardValues.get(0) + (cardValues.get(1)*14) + (cardValues.get(2)*14*14*14) + (cardValues.get(4)*14*14)); 
            } else if(cardValues.get(3).equals(cardValues.get(4))) {
                //k, k, k, pair
                handValue.setScore(cardValues.get(0) + (cardValues.get(1)*14) + (cardValues.get(2)*14*14) + (cardValues.get(3)*14*14*14));
            }
            
            handValue.setRank(1);
            return handValue;
        }

        int flushValue = Poker.flushValue(cards);
        int straightValue = Poker.straightValue(cards);
        
        //System.out.println(straightValue);
        
        if(flushValue!=-1 && straightValue!=-1) {
            handValue.setScore(straightValue);
            handValue.setRank(8);
            return handValue;
        } else if(flushValue!=-1) {
            handValue.setScore(Poker.highHandValue(cards));
            handValue.setRank(5);
            return handValue;
        } else if(straightValue!=-1) {
            handValue.setScore(straightValue);
            handValue.setRank(4);
            return handValue;
        }
        
        handValue.setScore(Poker.highHandValue(cards));
        handValue.setRank(0);
        return handValue;
    }
}
