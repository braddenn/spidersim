/*
 *  SpiderSim - simulates the card game Spider with 4 decks
 *  @author Brad Denniston, 6 Sept 2014
 * 
 * References:
 *	Online Simulator: http://www.spidersolitaire-cardgame.com/
 *	Rules: http://www.spidersolitaire-cardgame.com/#rules
 */
package spidersim;

public class SpiderSim {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        RunSim runsim = new RunSim();
        runsim.PlayHands();
    }
}
