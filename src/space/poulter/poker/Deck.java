package space.poulter.poker;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Em Poulter
 */
public class Deck {

    private List<Card> liveDeck;
    private List<Card> deadDeck;
    private Random rand;

    public Deck() {
        rand = new Random();
        this.liveDeck = new ArrayList<>();
        this.deadDeck = new ArrayList<>();

        for (Card.CardRank rank : Card.CardRank.values()) {
            for (Card.CardSuit suit : Card.CardSuit.values()) {
                this.liveDeck.add(new Card(rank, suit));
            }
        }
    }

    public Card drawCard() throws DrawCardException {
        if (liveDeck.isEmpty()) {
            //TODO should we just reshuffle the deck here?
            throw new DrawCardException("Could not draw card: deck is empty!");
        }
        Card c = liveDeck.get(rand.nextInt(liveDeck.size()));
        if (!liveDeck.remove(c)) {
            throw new DrawCardException("Could not remove card from live deck");
        }
        if (deadDeck.contains(c)) {
            throw new DrawCardException("Drawn card already exists in dead deck");
        }
        deadDeck.add(c);
        return c;
    }

    public class DrawCardException extends Exception {
        DrawCardException(String message) {
            super(message);
        }
    }
}
