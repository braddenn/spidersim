/*
*   @author Brad Denniston, 6 Sept 2014
* Build a deck of cards. There are 104 cards in two colors.
* Each card is in an expandable list called a Set.
 */
package spidersim;

import java.util.*;

public class Deck {
    public ArrayList<ArrayList<Card>> alalSets; // column of sets of cards
    
    // constructor - build a Deck of Sets
    // a Set contains 1 Card
    // The deck has 104 cards, 4 Red 1-13 and 4 black 1-13
    public Deck(  ) {
        int ordinal;
        alalSets = new ArrayList<>();
        
        // load the cards
        for ( ordinal = 1; ordinal <= 13; ordinal++) {
            for( int iCount = 1; iCount <= 4; iCount++) {
                // the deck adds one set which contains one card
                
                ArrayList<Card> redCardSet = new ArrayList<>();
                Card redCard = new Card();
                redCard.value = ordinal;
                redCard.color = Enums.RED;
                redCardSet.add(redCard);        // add card to set
                alalSets.add( redCardSet );      // add set to deck

                ArrayList<Card> blackCardSet = new ArrayList<>();
                Card blackCard = new Card();
                blackCard.value = ordinal;
                blackCard.color = Enums.BLACK;
                blackCardSet.add(blackCard);
                alalSets.add( blackCardSet );
            }
        }
    }
    
    /**
     * RandomizeTheDeck - randomize the sets of the current deck
     *      copy the deck, empty the deck, then randomly remove cards from the 
     *      copy and add them to the deck.
     */
    public void RandomizeTheDeck() {
        ArrayList<ArrayList<Card>> alalCopy = new ArrayList<>();
        int i, iRand;
        Random rand = new Random();
        
        // remove deck to the copy
        for( i=0; i<104; i++) {
            alalCopy.add( alalSets.get(i) );
        }
        alalSets.clear();
        
        // randomly copy them back as copy size gets smaller
        for( i=104; i>0; i-- ) {
            iRand = (int)rand.nextInt(i);
            alalSets.add( alalCopy.remove(iRand) );
        }
    }
}
