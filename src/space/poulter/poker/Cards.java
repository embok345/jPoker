package space.poulter.poker;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A wrapper for a collection of {@link Card}s.
 *
 * @author Em Poulter
 */
public class Cards implements Iterable<Card>, Serializable {

    private final Logger log = Logger.getLogger(getClass().getName());
    /* The list of cards */
    private final List<Card> cards = new ArrayList<>();

    {
        log.setLevel(Util.LOG_LEVEL);
    }

    /**
     * Create a collection of the specified number Cards. These cards are all copies of {Card.EMPTY_CARD}, having no
     * rank or suit.
     *
     * @param noCards The number of copies of the empty card to add to the collection.
     */
    public Cards(int noCards) {
        cards.addAll(Collections.nCopies(noCards, Card.EMPTY_CARD.clone()));
    }

    /**
     * Create a copy of the original Collection of Cards. Note all of the Cards in the old collection are
     * copied and added, rather than being just added.
     *
     * @param cards The old collection of cards
     */
    Cards(@NotNull Card... cards) {
        for (Card c : cards) {
            this.cards.add(new Card(c.getRank(), c.getSuit()));
        }
    }

    /**
     * Create a copy of the original Collection of Cards. Note all of the Cards in the old collection are
     * copied and added, rather than being just added.
     *
     * @param cards The old collection of cards
     */
    private Cards(@NotNull Cards cards) {
        for (Card c : cards) {
            this.cards.add(c.clone());
        }
    }

    /**
     * Create a copy of the original Collection of Cards. Note all of the Cards in the old collection are
     * copied and added, rather than being just added.
     *
     * @param cards The old collection of cards
     */
    private Cards(@NotNull Collection<Card> cards) {
        for (Card c : cards) {
            this.cards.add(c.clone());
        }
    }

    /**
     * Get the best possible poker hand which can be made from the 2 hole cards and 5 community cards. Search across
     * all possible selections of five cards from these seven which gives the highest ranked hand. If either
     * of the parameters is not valid, that is, doesn't have the correct number of cards, return the worst possible
     * rating.
     *
     * @param holeCards      The two cards held by the player.
     * @param communityCards The five community cards common to all players.
     * @return The best possible poker hand that can be made from the seven cards passed as arguments.
     */
    @NotNull
    @Contract("_, _ -> new")
    static public RankedCards getBestHand(@NotNull Cards holeCards, @NotNull Cards communityCards) {

        /* Remove any non cards. */
        Cards hole = holeCards.getRealCards();
        Cards community = communityCards.getRealCards();

        /* If the sizes of the collections are not valid, return the worst possible rank */
        if (hole.getSize() != 2 || community.getSize() != 5)
            return new RankedCards(new Cards(5), HandRank.HIGH_CARD, 0);

        /* Make a copy of the cards, and score them. */
        Cards newHand = new Cards(community);
        RankedCards bestRank = new RankedCards(newHand);

        for (int i = 0; i < 5; i++) {
            /* Replace the i-th community card by the 1st hole card, and score it. */
            newHand.setCard(i, holeCards.getCard(0));
            RankedCards newRank = new RankedCards(newHand);

            /* If the rank of the new hand is better than the previous best, this is our new best. */
            if (newRank.compareTo(bestRank) > 0)
                bestRank = newRank;

            for (int j = 0; j < 5; j++) {
                /* Replace the j-th community card by the 2nd hole card, and score it. */
                newHand.setCard(j, holeCards.getCard(1));
                newRank = new RankedCards(newHand);

                /* If the rank of the new hand is better, update the best. */
                if (newRank.compareTo(bestRank) > 0)
                    bestRank = newRank;

                /* Reset the j-th card in the hand to what it was before. */
                if (i == j)
                    newHand.setCard(j, holeCards.getCard(0));
                else
                    newHand.setCard(j, community.getCard(j));
            }

            /* Reset the i-th card in the hand to what it was before. */
            newHand.setCard(i, community.getCard(i));
        }

        /* This should never be reached. Return the worst possible rank. */
        return new RankedCards(new Cards(5), HandRank.HIGH_CARD, 0);
    }

    /**
     * Get the size of the underlying Collection of Cards.
     *
     * @return The size of the Collection of Cards.
     */
    public int getSize() {
        return cards.size();
    }

    /**
     * Try to get the Card at the given index of the Collection. If the index is out of bounds, return a copy of the
     * empty card.
     *
     * @param index The index of the collection to retrieve.
     * @return The Card at the given index, or the empty card if the index is out of bounds.
     */
    @NotNull
    public Card getCard(int index) {
        if (index < 0 || index >= cards.size()) {
            return Card.EMPTY_CARD.clone();
        }
        return cards.get(index);
    }

    /**
     * Set the Card at the given position in the Collection.
     *
     * @param index The index in which to place the Card
     * @param c     The Card to place in the collection
     */
    public void setCard(int index, @NotNull Card c) {
        if (index < 0 || index >= cards.size()) return;
        cards.set(index, c);
    }

    /**
     * Get a Collection consisting of the ranks of the cards within this collection.
     *
     * @return A collection of the ranks of the cards.
     */
    private List<Card.CardRank> getRanks() {
        List<Card.CardRank> ranks = new ArrayList<>();
        cards.forEach(c -> ranks.add(c.getRank()));
        return ranks;
    }

    /**
     * Get a Collection consisting of the suits of the cards within this collection.
     *
     * @return A collection of the suits of the cards.
     */
    private List<Card.CardSuit> getSuits() {
        List<Card.CardSuit> suits = new ArrayList<>();
        cards.forEach(c -> suits.add(c.getSuit()));
        return suits;
    }

    /**
     * Filter out all of the Cards in the collection which are not real cards. That is, remove all of those Cards
     * which have either ZERO rank or NONE suit. This does not alter this collection.
     *
     * @return A new instance containing only those cards which are real cards.
     */
    @NotNull
    @Contract(" -> new")
    private Cards getRealCards() {
        return new Cards(this.cards.stream().filter(Card::isRealCard).collect(Collectors.toList()));
    }

    /**
     * Checks if the collection of Cards contains any {@link Card#EMPTY_CARD}s.
     *
     * @return true if the collection contains at least one copy of {@link Card#EMPTY_CARD}, false otherwise.
     */
    boolean containsEmptyCards() {
        return cards.contains(Card.EMPTY_CARD);
    }

    /**
     * Checks if the collection of cards contains any Cards which are not complete. That is a card which
     * has either NONE suit or ZERO rank.
     *
     * @return true if the collection contains a non card, false otherwise.
     */
    boolean containsNonCard() {
        for (Card c : this.cards) {
            if (c.getRank().equals(Card.CardRank.ZERO) || c.getSuit().equals(Card.CardSuit.NONE))
                return true;
        }
        return false;
    }

    /**
     * Sort the underlying collection by the rank of the cards, but giving infinite weight to those ranks passed
     * as arguments. That is, sorting cards such that card1 is less than card2 if either the rank of card1 is less than
     * the rank of card2, and neither of those ranks are passed as arguments, or if the rank of card2 is passed as an
     * argument, and the rank of card1 is not, or if the ranks of both card1 and card2 are passed as arguments, but the
     * rank of card2 occurs first in the argument list.
     *
     * @param ranks The Card Ranks to give higher weight to, where the first argument has higher weight than the second,
     *              and so on, and all of which have higher weight than the ranks not passed as arguments.
     * @return A reference to this object.
     */
    @Contract("_ -> this")
    private Cards weightedSort(Card.CardRank... ranks) {
        List<Card.CardRank> ranksList = Arrays.asList(ranks);
        cards.sort((card1, card2) -> {

            /* If they have the same rank, they are equal ish. */
            if (card1.getRank() == card2.getRank()) return 0;

            /* If neither of the cards have rank on the list, just compare them normally. */
            if (!ranksList.contains(card1.getRank()) && !ranksList.contains(card2.getRank())) {
                return Integer.compare(card1.getIntRank(), card2.getIntRank());
            }

            /* If card1 is on the list but card2 isn't, card1 is larger. */
            if (ranksList.contains(card1.getRank()) && !ranksList.contains(card2.getRank())) {
                return 1;
            }

            /* If card2 is on the list but card1 ins't, card2 is larger. */
            if (ranksList.contains(card2.getRank()) && !ranksList.contains(card1.getRank())) {
                return -1;
            }

            /* Now both card1 and card2 have rank on the list, so the larger is determined by the smaller index. */
            /* If card1 occurs before card2, card1 is larger. */
            if (ranksList.indexOf(card1.getRank()) < ranksList.indexOf(card2.getRank())) {
                return 1;
            }
            /* Otherwise, card2 occurs before card1, so card2 is larger. */
            return -1;
        });
        return this;
    }

    /**
     * Returns a String representation of the Collection of Cards. This is a comma separated list of String
     * representations of each of the Cards, as defined in {@link Card#toString()}.
     *
     * @return A String representation of the Collection of Cards.
     */
    @Override
    public String toString() {
        String s = "";
        s = cards.stream().map(c -> c.toString() + ',').reduce(s, String::concat);
        //Remove the final comma.
        return s.substring(0, s.length() - 1);
    }

    /**
     * Determines if two instances of Cards are equal. They are equal iff they have the same Cards in their
     * underlying Collection. Note unlike List, this does not consider the ordering of the cards; all orderings of
     * the same Cards are equal.
     *
     * @param that The other object to compare this to.
     * @return true if the Cards in both collections are the same, false otherwise.
     */
    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object that) {
        /* Make sure we have the right class. */
        if (!(that instanceof Cards)) return false;

        /* If the collections have different sizes, they can't be the same. */
        if (((Cards) that).getSize() != this.getSize()) return false;

        /* If there are any cards in this collection not in that, they can't be equal. */
        for (Card c : this) {
            if (!((Cards) that).cards.contains(c)) return false;
        }

        return true;
    }

    /**
     * Return the Iterator associated with the underlying Collection.
     *
     * @return The iterator of the Collection.
     */
    @Override
    @NotNull
    public Iterator<Card> iterator() {
        return cards.iterator();
    }

    public enum HandRank {
        HIGH_CARD, PAIR, TWO_PAIR, THREE, STRAIGHT, FLUSH, FULL_HOUSE, FOUR, STRAIGHT_FLUSH;

        @NotNull
        @Contract(pure = true)
        @Override
        public String toString() {
            switch (this) {
                case HIGH_CARD:
                    return "High Card";
                case PAIR:
                    return "Pair";
                case TWO_PAIR:
                    return "Two Pair";
                case THREE:
                    return "Three of a kind";
                case STRAIGHT:
                    return "Straight";
                case FLUSH:
                    return "Flush";
                case FULL_HOUSE:
                    return "Full House";
                case FOUR:
                    return "Four of a kind";
                case STRAIGHT_FLUSH:
                    return "Straight Flush";
                default:
                    return "";
            }
        }
    }

    /**
     * A wrapper for the cards class which also contains the rank of the poker hand contained in the cards.
     */
    static class RankedCards extends Cards implements Comparable<RankedCards> {

        /* The rank of the hand in the Cards. */
        private HandRank rank;

        /* The score within the rank of the hand.
         * - If the rank is a straight, this is just the value of the
         *   highest card in the straight (inc. 5 high straight).
         * - If the rank is HIGH_CARD, or FLUSH, the rank is sum(15^(5 - i) * rank(i)),
         *   where rank(i) is the rank of the ith highest card (rank(0) is the rank of the
         *   highest rank card, rank(5) is the rank of the lowest rank card.)
         * - For the others it is similar, but with higher weighting going to whichever
         *   ranks are duplicated. e.g. for FOUR, it is 15*quad + kicker, where
         *   quad is the rank of the quad, and kicker is the rank of the kicker.
         */
        private int score;

        private RankedCards(@NotNull Cards cards, HandRank rank, int score) {
            for (Card c : cards) {
                if (!c.isNonCard())
                    super.cards.add(c);
            }
            this.rank = rank;
            this.score = score;
        }

        /**
         * Determines the ranking of the poker hand. If it is not a full hand, i.e. 5 cards, it will just get the best
         * for those cards. The Cards are sorted to have the most important cards first, in particular, the Card
         * in the first index will be the highest rank card, or the card with highest multiplicity of rank of highest rank.
         * That is, pairs occur before high cards, sets occur before high cards, etc.
         */
        RankedCards(@NotNull Cards cards) {

            /* Remove all of the non cards, and sort them into decreasing rank. */
            for (Card c : cards) {
                if (!c.isNonCard())
                    super.cards.add(c.clone());
            }
            super.cards.sort(Comparator.comparing(Card::getRank));
            Collections.reverse(super.cards);

            /* If the size of the Collection is 0, or we have more than 5 Cards it can't have a rank. */
            if (super.getSize() == 0 || super.getSize() > 5) {
                rank = HandRank.HIGH_CARD;
                score = 0;
                return;
            }

            /* If the size is 1, the only possibility is high card. */
            if (super.getSize() == 1) {
                rank = HandRank.HIGH_CARD;
                score = super.getCard(0).getIntRank();
                return;
            }

            /* Get a collection of the ranks of Cards in the Collection */
            List<Card.CardRank> cardRanks = super.getRanks();

            /* Find if we have any duplicate ranks. */
            Card.CardRank quad = Card.CardRank.ZERO;
            Card.CardRank set = Card.CardRank.ZERO;
            List<Card.CardRank> pairs = new ArrayList<>();
            for (Card.CardRank rank : cardRanks) {
                switch (Collections.frequency(cardRanks, rank)) {
                    case 2:
                        if (!pairs.contains(rank))
                            pairs.add(rank);
                        break;
                    case 3:
                        set = rank;
                        break;
                    case 4:
                        quad = rank;
                }
            }

            /* If `quad' is set, we have a quad. */
            if (quad != Card.CardRank.ZERO) {

                /* Find the largest and smallest ranks amongst the cards. */
                Card.CardRank maxRank = Collections.max(cardRanks);
                Card.CardRank minRank = Collections.min(cardRanks);

                if (maxRank == quad) {
                    /* If the quad is of the larger rank, the score is 15 times this, plus the kicker. */
                    score = 15 * maxRank.toInt() + ((minRank != maxRank) ? minRank.toInt() : 0);
                } else {
                    /* Otherwise, the score is 15 times the smaller rank, plus the kicker. */
                    score = 15 * minRank.toInt() + ((minRank != maxRank) ? maxRank.toInt() : 0);
                    /* Reverse the cards so the quads are first. */
                    Collections.reverse(super.cards);
                }
                rank = HandRank.FOUR;
                return;
            }

            /* If `set' is set, and we have one pair, we have a full house. */
            if (set != Card.CardRank.ZERO && pairs.size() == 1) {

                /* Find the largest and smallest ranks amongst the cards. */
                Card.CardRank maxRank = Collections.max(cardRanks);
                Card.CardRank minRank = Collections.min(cardRanks);

                if (maxRank == set) {
                    /* If the larger rank is the rank of the set, this is the dominant value of score. */
                    score = 15 * maxRank.toInt() + minRank.toInt();
                } else {
                    /* If the smaller rank is the rank of the set, this is the dominant value of score. */
                    score = 15 * minRank.toInt() + maxRank.toInt();
                    /* Reverse the cards so the set is first. */
                    Collections.reverse(super.cards);
                }
                rank = HandRank.FULL_HOUSE;
                return;
            }

            /* If only `set' is set, we just have a set. */
            if (set != Card.CardRank.ZERO) {
                score = set.toInt();
                for (Card.CardRank rank : cardRanks) {
                    if (rank != set) {
                        score *= 15;
                        score += rank.toInt();
                    }
                }
                Collections.reverse((super.weightedSort(set)).cards);

                rank = HandRank.THREE;
                return;
            }

            /* If there are two elements of `pairs', we have 2 pair. */
            if (pairs.size() == 2) {
                score = 15 * pairs.get(0).toInt() + pairs.get(1).toInt();
                for (Card.CardRank rank : cardRanks) {
                    if (!pairs.contains(rank)) {
                        score *= 15;
                        score += rank.toInt();
                    }
                }
                Collections.reverse((super.weightedSort(pairs.get(0), pairs.get(1))).cards);
                rank = HandRank.TWO_PAIR;
                return;
            }

            /* If there is only one, we have just one pair. */
            if (pairs.size() == 1) {
                score = pairs.get(0).toInt();
                for (Card.CardRank rank : cardRanks) {
                    if (rank != pairs.get(0)) {
                        score *= 15;
                        score += rank.toInt();
                    }
                }
                Collections.reverse((super.weightedSort(pairs.get(0))).cards);
                rank = HandRank.PAIR;
                return;
            }

            /* If we don't have a duplicate rank, yet fewer than 5 cards, we must have a high card. */
            if (super.getSize() != 5) {
                score = 0;
                for (Card c : this) {
                    score *= 15;
                    score += c.getIntRank();
                }
                rank = HandRank.HIGH_CARD;
                return;
            }

            /* If there are 5 cards, and no duplicate ranks, we could have a flush, straight, or
             * just a high card. */

            /* Get the collection of suits. */
            List<Card.CardSuit> cardSuits = super.getSuits();
            Set<Card.CardSuit> cardSuitsSet = new HashSet<>(cardSuits);
            boolean flush = false, straight = true;

            /* If there is only one unique suit, we have a flush. */
            if (cardSuitsSet.size() == 1) flush = true;

            /* Iterate over all of the ranks */
            Iterator<Card.CardRank> cardIterator = cardRanks.iterator();
            Card.CardRank previousRank = null, currentRank;
            while (cardIterator.hasNext()) {
                if (previousRank == null) {
                    previousRank = cardIterator.next();
                    continue;
                }
                currentRank = cardIterator.next();
                /* If the current rank is not a neighbour of the previous one, we can't have a straight. */
                if (!currentRank.isNeighbour(previousRank)) {
                    straight = false;
                    break;
                }
                previousRank = currentRank;
            }
            /* If we complete the loop, we do have a straight. */

            /* Check for an A-5 straight directly. */
            boolean lowStraight = false;
            if (!straight) {
                straight = true;
                lowStraight = true;
                for (Card.CardRank rank : cardRanks) {
                    /* If any rank is not A-5, we can't have a straight, and of course we can't have duplicate ranks. */
                    if (rank != Card.CardRank.ACE && rank != Card.CardRank.TWO && rank != Card.CardRank.THREE &&
                            rank != Card.CardRank.FOUR && rank != Card.CardRank.FIVE) {
                        lowStraight = false;
                        straight = false;
                    }
                }
            }

            /* If we have a straight and a flush, we return straight flush, if just one we return that,
             * and if we have neither, we just have a high card.
             */
            if (flush && straight) {
                /* The score for the straight is just the highest card. */
                if (lowStraight) {
                    /* In the A-5 straight, the highest card is 5. */
                    Collections.reverse(super.cards);
                    Collections.rotate(super.cards, 1);
                    Collections.reverse(super.cards);

                    rank = HandRank.STRAIGHT_FLUSH;
                    score = 5;
                    return;
                } else {
                    rank = HandRank.STRAIGHT_FLUSH;
                    score = super.getCard(0).getIntRank();
                    return;
                }
            }
            if (flush) {
                /* The score for the flush is the ranking of all cards. */
                score = 0;
                for (Card c : this) {
                    score *= 15;
                    score += c.getIntRank();
                }
                rank = HandRank.FLUSH;
                return;
            }
            if (straight) {
                if (lowStraight) {
                    Collections.reverse(super.cards);
                    Collections.rotate(super.cards, 1);
                    Collections.reverse(super.cards);
                    rank = HandRank.STRAIGHT;
                    score = 5;
                    return;
                } else {
                    rank = HandRank.STRAIGHT;
                    score = this.getCard(0).getIntRank();
                    return;
                }
            }

            score = 0;
            for (Card c : this) {
                score *= 15;
                score += c.getIntRank();
            }
            rank = HandRank.HIGH_CARD;


        }

        /**
         * Get the ranking of the poker hand.
         *
         * @return The rank of the hand.
         */
        HandRank getRank() {
            return rank;
        }

        /**
         * Get the score within the rank of the poker hand.
         *
         * @return The score of the poker hand.
         */
        int getScore() {
            return score;
        }

        /**
         * Returns a String representation of the Ranked Cards. This is a description of the rank, followed by the String
         * representation of the Cards, as in {@link Cards#toString()}.
         *
         * @return A String representation of the ranking of the Cards.
         */
        @Override
        public String toString() {
            switch (rank) {
                case HIGH_CARD:
                    return "High Card " + getCard(0).getRank().toChar() + "\n" + super.toString();

                case PAIR:
                    return "Pair of " + getCard(0).getRank().toChar() + "s\n" + super.toString();

                case TWO_PAIR:
                    return "Two Pair, " + getCard(0).getRank().toChar() + "s and " +
                            getCard(2).getRank().toChar() + "s\n" + super.toString();

                case THREE:
                    return "Three of a kind of " + getCard(0).getRank().toChar() + "s\n" + super.toString();

                case STRAIGHT:
                    return "Straight, " + getCard(4).getRank().toChar() + " to " +
                            getCard(0).getRank().toChar() + "\n" + super.toString();

                case FLUSH:
                    return getCard(0).getRank().toChar() + " high " + getCard(0).getSuit().toSymbol() +
                            " Flush\n" + super.toString();

                case FULL_HOUSE:
                    return "Full House, " + getCard(0).getRank().toChar() + "s full of " +
                            getCard(3).getRank().toChar() + "s\n" + super.toString();

                case FOUR:
                    return "Four of a kind of " + getCard(0).getRank().toChar() + "s\n" + super.toString();

                case STRAIGHT_FLUSH:
                    if (getCard(0).getRank().equals(Card.CardRank.ACE)) {
                        /* If we have an ace high straight flush, this is a royal flush. */
                        return getCard(0).getSuit().toSymbol() + "Royal Flush!\n" + super.toString();
                    } else {
                        return "Straight " + getCard(0).getRank().toChar() + " Flush, " +
                                getCard(4).getRank().toChar() + " to " + getCard(0).getRank().toChar() +
                                "\n" + super.toString();
                    }

                default:
                    /* This can never be reached. */
                    return "";

            }
        }

        /**
         * Compares the rank of two poker hands. Returns 1 is this is greater than that, -1 if that is greater than
         * this, and 0 if they are they have the same rank. this is greater than that if the rank of this is greater
         * than the rank of that, or if they have the same rank, if the score of this is greater than the score of that.
         * They are equal if they have the same rank, and same score, as per {@link #equals(Object)}.
         *
         * @param that The other instance of RankedCards to compare this to.
         * @return 1 if this is greater than that, -1 if that is greater than this, or 0 if they are equal.
         */
        @Override
        public int compareTo(@NotNull RankedCards that) {
            /* If the ranks aren't the same, this determines their order. */
            if (this.getRank().compareTo(that.getRank()) != 0) {
                return this.getRank().compareTo(that.getRank());
            }
            /* If the ranks are the same, the score determines their order. */
            return Integer.compare(this.getScore(), that.getScore());
        }

        /**
         * Determines if two instances of RankedCards are equal. Two instances of RankedCards are equal iff
         * their rank and their score are equal. Note that this may not agree with {@link Cards#equals(Object)},
         * as this is not affected by the values of the cards themselves.
         *
         * @param that The Object with which to compare this.
         */
        @Contract(value = "null -> false", pure = true)
        @Override
        public boolean equals(Object that) {
            /* Make sure we have the right class. */
            if (!(that instanceof RankedCards)) return false;

            /* If their rank differs, they can'tbe the same. */
            if (this.getRank() != ((RankedCards) that).getRank()) return false;

            /* If their ranks are the same, they are equal iff their scores are the same. */
            return this.getScore() == ((RankedCards) that).getScore();
        }
    }

}
