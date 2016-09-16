/**
 *   @author Brad Denniston, 6 Sept 2014
 *   card - an object with an ordinal and a color representing a playing card. 
 *   There are 104 cards.
 */
package spidersim;

/**
 * Class Card - this is a single playing card
 */
public class Card {
    public  int     value;  // 1-13 representing the playing cards, 13 is King
    public  Enums  color;  // RED or BLACK - see enum Colors
    
    public Card() {
    }
    
    public Card( Card cCard ) {
        value = cCard.value;
        color = cCard.color;
    }
    
    public Card( int iValueIn, Enums eColorIn ) {
        value = iValueIn;
        color = eColorIn;
    }
}
