package space.poulter.poker;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.logging.Logger;

/**
 * @author Em Poulter
 */
public class Card implements Serializable, Cloneable {

    /**
     * A representation of an empty card, having rank {CardRank.ZERO} and suit {CardSuit.NONE}.
     */
    public static final Card EMPTY_CARD = new Card(CardRank.ZERO, CardSuit.NONE);
    private final Logger log = Logger.getLogger(getClass().getName());
    /* The rank and suit of the card */
    private final CardRank rank;
    private final CardSuit suit;

    {
        log.setLevel(Util.LOG_LEVEL);
    }

    /**
     * Create a card with the given rank and suit.
     *
     * @param rank The rank of the card.
     * @param suit The suit of the card.
     */
    Card(CardRank rank, CardSuit suit) {
        this.rank = rank;
        this.suit = suit;
    }

    /**
     * Converts a string representation of a card into a Card. This String should be two characters long, with the first
     * being the value of the rank ('2',...'A'), and the second the suit ('s', 'S', or '♠', and similar for other
     * suits). If either the rank or the suit is not of a valid form, it will set to ZERO or NONE respectively. If the
     * string is not 2 characters long, or the string is "null" (the string containing the word "null"),
     * it will return a copy of {@link #EMPTY_CARD}.
     *
     * @param str The string representation of the card.
     */
    public Card(@NotNull String str) {
        if (str.equals("null") || str.length() != 2) {
            rank = CardRank.ZERO;
            suit = CardSuit.NONE;
        } else {
            rank = CardRank.fromChar(str.charAt(0));
            suit = CardSuit.fromChar(str.charAt(1));
        }
    }

    /**
     * Creates the Card with the given rank and suit. If rank is outside the range [2,14], the rank will be ZERO, and
     * similarly if suit is not 's', 'S', or '♠', or a different suit, the suit will be NONE.
     *
     * @param rank The rank of the card in the range [2,14]
     * @param suit The suit of the card as either a letter or a symbol.
     */
    Card(int rank, char suit) {
        this.rank = CardRank.fromInt(rank);
        this.suit = CardSuit.fromChar(suit);
    }

    /**
     * Get the rank of the Card.
     *
     * @return The rank of the Card.
     */
    CardRank getRank() {
        return this.rank;
    }

    /**
     * Get the rank of the Card as an int, in the range [2,14], or 0.
     *
     * @return The rank of the Card.
     */
    int getIntRank() {
        return this.rank.toInt();
    }

    /**
     * Get the suit of the Card
     *
     * @return The suit of the card.
     */
    public CardSuit getSuit() {
        return this.suit;
    }

    /**
     * Check if the Card is a non card. That is, if either the rank is ZERO, or the suit is NONE. The opposite of
     * {@link #isRealCard()}.
     *
     * @return true if the rank is ZERO or the suit is NONE, otherwise false.
     */
    boolean isNonCard() {
        return rank.equals(CardRank.ZERO) || suit.equals(CardSuit.NONE);
    }

    /**
     * Check if the Card is a real card. That is, if the rank is non ZERO and the suit is not NONE. The opposite of
     * {@link #isNonCard()}.
     *
     * @return true if the rank is non ZERO and the suit is not NONE, otherwise false.
     */
    boolean isRealCard() {
        return !isNonCard();
    }

    /**
     * Create a copy of the card, with the same rank and suit as this one.
     *
     * @return A new Card with the same rank and suit as this one.
     */
    @Override
    public Card clone() {
        try {
            Card c = (Card) super.clone();
            if (!c.equals(this) || super.equals(c)) throw new CloneNotSupportedException();
            return c;
        } catch (CloneNotSupportedException ex) {
            return new Card(getRank(), getSuit());
        }
    }

    /**
     * Returns a String representation of the Card. This is a String with two characters, the first being the rank
     * of the Card, and the second being the symbol representing the Card (e.g. ♠ for spades). If the rank is ZERO
     * or the suit is NONE, the corresponding character is 'X'.
     *
     * @return A String representation of the Card.
     */
    @Override
    public String toString() {
        String s = "";
        s += rank.toChar();
        s += suit.toSymbol();
        return s;
    }

    /**
     * Two cards are the same iff they are of the same suit and same rank.
     *
     * @param o the reference object with which to compare.
     * @return true if this Card is the same as the obj argument; false otherwise.
     */
    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Card))
            return false;

        Card c = (Card) o;

        return (c.getIntRank() == getIntRank()) && (c.getSuit().toInt() == getSuit().toInt());
    }

    /**
     * Returns the hash code value for the Card. Computed as rank + 15*suit. There are only 14 cards of each suit of
     * each rank (2-A, plus 0), put as A has value 14, the total comes to 15.
     *
     * @return A hash code value for the Card.
     */
    @Override
    public int hashCode() {
        return getIntRank() + (getSuit().toInt() * 15);
    }

    /**
     * Enum containing the 4 card suits, SPADE, HEART, DIAMOND, and CLUB, as well as a blank suit, NONE.
     */
    public enum CardSuit {
        NONE, SPADE, HEART, DIAMOND, CLUB;

        /**
         * Convert the given character into its Suit. For example, converts either s, S, or ♠ into SPADE, and
         * similarly for the other suits. Any unrecognized character gets converted into NONE.
         *
         * @param c The character to convert to a suit.
         * @return The Suit that the character represents, or NONE if it is unrecognized.
         */
        private static CardSuit fromChar(char c) {
            switch (c) {
                case 'S':
                case 's':
                case 0x2660:
                    return SPADE;
                case 'H':
                case 'h':
                case 0x2665:
                    return HEART;
                case 'D':
                case 'd':
                case 0x2666:
                    return DIAMOND;
                case 'C':
                case 'c':
                case 0x2663:
                    return CLUB;
                default:
                    return NONE;
            }
        }

        /**
         * Converts the Suit into a character representing it. For example, converts SPADE into 'S'.
         *
         * @return The character representing the suit.
         */
        char toChar() {
            switch (this) {
                case SPADE:
                    return 'S';
                case HEART:
                    return 'H';
                case DIAMOND:
                    return 'D';
                case CLUB:
                    return 'C';
                default:
                    return 'X';
            }
        }

        /**
         * Converts the Suit into the symbol representing it. For example, SPADE converts to ♠.
         *
         * @return The symbol representing the Suit.
         */
        char toSymbol() {
            switch (this) {
                case SPADE:
                    return Character.toChars(Integer.parseInt("2660", 16))[0];
                case HEART:
                    return Character.toChars(Integer.parseInt("2665", 16))[0];
                case DIAMOND:
                    return Character.toChars(Integer.parseInt("2666", 16))[0];
                case CLUB:
                    return Character.toChars(Integer.parseInt("2663", 16))[0];
                default:
                    return 'X';
            }
        }

        /**
         * Converts the Suit into a numeric value. Arbitrarily chosen so that SPADE = 1, HEART = 2, DIAMOND = 3,
         * CLUB = 4, and NONE = 0.
         *
         * @return The numeric value of the suit.
         */
        private int toInt() {
            switch (this) {
                case SPADE:
                    return 1;
                case HEART:
                    return 2;
                case DIAMOND:
                    return 3;
                case CLUB:
                    return 4;
                default:
                    return 0;
            }
        }
    }

    /**
     * Enum containing the different ranks of cards. That is, TWO through TEN, then JACK, QUEEN, KING, and ACE. Also
     * contains an empty rank, ZERO.
     */
    public enum CardRank {
        ZERO, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING, ACE;

        /**
         * Converts the given integer into the rank of card it represents. Namely, 2 -> TWO, ..., 10 -> TEN, then
         * 11 -> JACK, 12 -> QUEEN, 13 -> KING, and 14 -> ACE. Any numbers out of this range return ZERO.
         *
         * @param val The integer to convert to its respective rank.
         * @return The rank represented by the int, or ZERO if the int is outside the correct range.
         */
        static CardRank fromInt(int val) {
            switch (val) {
                case 2:
                    return TWO;
                case 3:
                    return THREE;
                case 4:
                    return FOUR;
                case 5:
                    return FIVE;
                case 6:
                    return SIX;
                case 7:
                    return SEVEN;
                case 8:
                    return EIGHT;
                case 9:
                    return NINE;
                case 10:
                    return TEN;
                case 11:
                    return JACK;
                case 12:
                    return QUEEN;
                case 13:
                    return KING;
                case 14:
                    return ACE;
                default:
                    return ZERO;
            }
        }

        /**
         * Converts the given character into the rank it represents. Characters '2', ..., '9' go to their respective
         * numbers, whilst 'T' -> TEN, 'Q' -> QUEEN, etc. Any unrecognized rank gets sent to ZERO.
         *
         * @param val The character to be converted to its rank.
         * @return The rank represented by the character, or ZERO if it is not valid.
         */
        private static CardRank fromChar(char val) {
            switch (val) {
                case 'A':
                case 'a':
                    return ACE;
                case 'K':
                case 'k':
                    return KING;
                case 'Q':
                case 'q':
                    return QUEEN;
                case 'J':
                case 'j':
                    return JACK;
                case 'T':
                case 't':
                    return TEN;
                default:
                    return fromInt(Integer.parseInt(String.valueOf(val)));
            }
        }

        /**
         * Converts the rank from an integer into the character representing it. {@link #toChar} composed
         * with {@link #fromInt(int)}.
         *
         * @param val The integer to convert to a rank.
         * @return The character representing the rank.
         */
        static char valueToChar(int val) {
            return CardRank.fromInt(val).toChar();
        }

        /**
         * Converts the card rank into the integer representing it. Namely, TWO->2, etc. and JACK -> 11, QUEEN -> 12,
         * KING -> 13, and ACE -> 14, and ZERO -> 0.
         *
         * @return The integer representing this rank.
         */
        int toInt() {
            switch (this) {
                case TWO:
                    return 2;
                case THREE:
                    return 3;
                case FOUR:
                    return 4;
                case FIVE:
                    return 5;
                case SIX:
                    return 6;
                case SEVEN:
                    return 7;
                case EIGHT:
                    return 8;
                case NINE:
                    return 9;
                case TEN:
                    return 10;
                case JACK:
                    return 11;
                case QUEEN:
                    return 12;
                case KING:
                    return 13;
                case ACE:
                    return 14;
                default:
                    return 0;
            }
        }

        /**
         * Converts the rank into the character representing it. The inverse of {@link #fromChar}. If the rank is ZERO,
         * the character is 'X'.
         *
         * @return The character representing this rank.
         */
        char toChar() {
            switch (this.toInt()) {
                case 14:
                    return 'A';
                case 13:
                    return 'K';
                case 12:
                    return 'Q';
                case 11:
                    return 'J';
                case 10:
                    return 'T';
                case 0:
                    return 'X';
                default:
                    return Integer.toString(this.toInt()).charAt(0);
            }
        }

        /**
         * Checks whether this rank is a neighbour of the given rank. That is, TWO is a neighbour of THREE,
         * THREE a neighbour of TWO and FOUR, ..., ACE is a neighbour of KING. ZERO is a neighbour of nothing.
         *
         * @param rank The rank which may be a neighbour of this rank.
         * @return true is the given rank is a neighbour of this rank, false otherwise.
         */
        boolean isNeighbour(CardRank rank) {
            switch (this) {
                case TWO:
                    return rank.equals(THREE);
                case THREE:
                    return rank.equals(TWO) || rank.equals(FOUR);
                case FOUR:
                    return rank.equals(THREE) || rank.equals(FIVE);
                case FIVE:
                    return rank.equals(FOUR) || rank.equals(SIX);
                case SIX:
                    return rank.equals(FIVE) || rank.equals(SEVEN);
                case SEVEN:
                    return rank.equals(SIX) || rank.equals(EIGHT);
                case EIGHT:
                    return rank.equals(SEVEN) || rank.equals(NINE);
                case NINE:
                    return rank.equals(EIGHT) || rank.equals(TEN);
                case TEN:
                    return rank.equals(NINE) || rank.equals(JACK);
                case JACK:
                    return rank.equals(TEN) || rank.equals(QUEEN);
                case QUEEN:
                    return rank.equals(JACK) || rank.equals(KING);
                case KING:
                    return rank.equals(QUEEN) || rank.equals(ACE);
                case ACE:
                    return rank.equals(KING);
                default:
                    return false;
            }
        }
    }


}
