/*
*   @author Brad Denniston, 6 Sept 2014
*   Called by main to set up and run the simulation and analysis
 */
package spidersim;

import java.util.*;

public class RunSim {
    
    public static Config   cConfig;
    public static Logger   Log;         //Log object for logging
    public static int  iChildCount = 1;
    
    private int     iWins = 0;
    private int     iLosses = 0;
    private CardState csState;
    private int     iTotalWins = 0;
    private int     iTotalLosses = 0;
    private int     iWinningPaths = 0;
    private int     iLosingPaths = 0;
    
    /**
     * Constructor: Read the config file, open the log file.
     */
    public RunSim() {
        cConfig = new Config();
        cConfig.sConfigFilename = "SpiderSim.xml";   
        cConfig.sLogfileName = "SpiderSim.txt";
        
        // open the log file
        // logging enables writes to the log file for info and debug
        Log = new Logger( );
        Log.OpenLogFile( cConfig.sLogfileName );
        Date date = new Date();
        String sToday = date.toString();
        Log.Write(sToday+"\n");
        
         // read the configration file (XML) into the config object
        cConfig.sConfigFilename = "SpiderSimConfig.xml"; // never changes
        cConfig.bDebugLogOn = false;  // can be overwritten by config file
        cConfig.bLogColumns = false;
        cConfig.bLogChild = false;
        cConfig.bReadState = false;
        
        // Initialize the link with the Master
        XMLConfigReader xcrReadConfig = new XMLConfigReader( cConfig.sConfigFilename );
        try {
            if( xcrReadConfig.LoadConfig() != Enums.OK ) 
            {
                Log.Write("Severe: Configuration file not found.\n");
                return;
            }
        } catch (Exception e) {
            Log.Write( "Exception: Configuration read exception in " + cConfig.sConfigFilename +"\n" );
            System.exit(-1);
        }
        if( cConfig.bDebugLogOn ) LogConfig( );
        Log.Write( "end of RunSim Constructor." + "\n");
    }
    
    /* LogConfig - write the configuration to the log file
     * @return - Enums
     */
    public Enums LogConfig( ) {

        if( Log.Write( "\n") != Enums.OK ) return Enums.FAILED;
        Log.Write( "Configuration. \n");
        Log.Write( "Config Filename - " + RunSim.cConfig.sConfigFilename + "\n" );
        Log.Write( "Number Of Plays - " + RunSim.cConfig.iNumberOfPlays + "\n");
        Log.Write( "bRandomize - " + RunSim.cConfig.bRandomize + "\n");
        Log.Write( "sLogfileName - " + RunSim.cConfig.sLogfileName + "\n");
        Log.Write( "sOutputFileName - " + RunSim.cConfig.sOutputFileName + "\n");
        Log.Write( "bDebugLogOn - " + RunSim.cConfig.bDebugLogOn + "\n");
        return Enums.OK;
    }
    
    /**
     * PlayHands - called from main
     * Create the deck
     * Loop - randomize deck, play hand, analyze results, report
     */
    public void PlayHands() {
        Enums eRetVal;
        Deck deck;
        
        Log.Write( "Play "+cConfig.iNumberOfPlays+" times with a "+ ((cConfig.bRandomize)?"random":"linear") + " deck\n" );
        // Create deck of 104 sets, 1 card per set.      

        // 104 card objects created
        // 104 sets created and assigned 1 card per set
        
	if( cConfig.iNumberOfPlays == 1 ) // only one Play
        {
            csState = new CardState();
            deck = new Deck();
            // put into order in XML file else randomize
            if( cConfig.bRandomize ) 
                deck.RandomizeTheDeck();
            Play( deck );
            Log.Write( "RunSim - End of play - "+iWins+" Wins and " + iLosses + " Losses. #children is "+iChildCount+"\n");        
        }
        else
        {
            // loop for the number of plays, always randomize
            for( int i = 1; i <= cConfig.iNumberOfPlays; i++ )
            {
                iChildCount = 1;
                iWins = 0;
                iLosses = 0;
                csState = new CardState();          
                deck = new Deck();
                deck.RandomizeTheDeck();
                eRetVal = Play( deck );
                if( eRetVal == Enums.CHILD_MAX ) {
                    Log.Write("End of play - max # of children\n");
                }
                if( eRetVal == Enums.EMPTY ){
                    Log.Write("End of play - ran all branches.\n");
                }
                // output the results for this play
                Log.Write( "RunSim - "+iWins+" Wins and " + iLosses + " Losses. #children is "+iChildCount+"\n");    
                if( iWins > 0 ) {
                    iTotalWins++;          
                    iWinningPaths += iWins;
                    iLosingPaths += iLosses;
                }
            }
            int iPercentWins = iTotalWins * 100/cConfig.iNumberOfPlays;
            int iPercentSuccess = iWinningPaths * 100/(iWinningPaths+iLosingPaths);
            Log.Write("Percent of deals that succeed in a winning hand is %"+iPercentWins+"\n");
            Log.Write("Percent of paths that succeed in a winning hand is %"+iPercentSuccess+"\n");
        }
        
        // analyze results
        // sum all results and output percent wins over plays, wins over losses
    }
    
    /**
     * Play - a Play is  a set of moves down a tree.
     * A CardState moves until there is a conflict (two moves to same destination)
     *      At a point of multiple possible actions child CardStates are spawned
     *      Action continues with the parent CardState till a win or loss. Then
     *      get each child - if there is one, else next parent till the end.
     * @param deck 
     */
    public Enums Play( Deck deck ) {
        Enums eRetVal;

        // restore a previous deck state
        if( cConfig.iNumberOfPlays == 1 ) { // only if one Play
            if( RunSim.cConfig.bReadState ) { // read state only when directed
                csState.LoadStateFromFile(deck);
            }
            else { // write this new state except when reading old state
                csState.SaveStateToFile( deck );
            }
        }
        
        // show the deck
        csState.LogDeck(deck);
        
        // set up the play area
        csState.DealToHidden( deck.alalSets );
        
        // deal
        csState.DealColumnCards( deck.alalSets );
        
        csState.LogHidden( );

        csState.LogColumns( "RunSim - Initial Columns");
        
        csState.LogChild( "Initial Parent ID is "+csState.iSpawnID+"\n" );

        
        eRetVal = Enums.MERGE;
        do{
            switch (eRetVal) {
                case MERGE:
                    eRetVal = csState.CardSpaceAction( Enums.MERGE, deck );
                    break;
                    
                case ATTACH:
                    eRetVal = csState.CardSpaceAction( Enums.ATTACH, deck );
                    break;
                    
                case DEAL:
                    eRetVal = csState.CardSpaceAction( Enums.DEAL, deck );
                    break;
                    
                case EMPTY:
                    eRetVal =  csState.CardSpaceAction( Enums.EMPTY, deck );
                    break;
                    
                case END_OF_DECK:{
                    iLosses++;
                   // Log.Write("*****  Lost - #wins is "+iWins+"#losses is "+iLosses+" rack is "+csState.iRack+"\n"); 
                    csState = NextChild( csState );
                    if( csState != null ) {
                        eRetVal = csState.aAction;
                    } break;
                }
                    
                case WIN:{
                    iWins++;
                   // Log.Write("*****  Won - #wins is "+iWins+" #losses is "+iLosses+"\n");
                    csState = NextChild( csState );
                    if( csState != null ) {
                        eRetVal = csState.aAction;
                    } break;
                }

                case FAILED:
                default: // should not happen
                    csState = null;
            }
            if( iChildCount > 10000 ) {
                Log.Write("At 10000 children. Stopping.\n");
                return Enums.CHILD_MAX;
            }
        } while (csState != null );
        return Enums.EMPTY;
    }
    
    /**
     * NextChild
     * @return the CardState of the next child in the current state's list
     * note that the parents are dead - they completed all experiences
     * @return the parent of current CardState if no more children
     * @return null if no more parents
     */
    private CardState NextChild( CardState csOldState ) {
        CardState csNewState;
        if( csOldState.alChild.isEmpty() == false ) {
            //Log.Write("Size of parent "+csOldState.iSpawnID+" child list is "+csOldState.alChild.size()+"\n");
            csNewState = csOldState.alChild.remove(0);
            csNewState.LogChild("Next child " + csNewState.iSpawnID+" doing "+ csNewState.aAction +"\n");
        }
        else{
            csNewState = csOldState;
            do {  // no children, move up parent list till children found
                csNewState = csNewState.csParent;
                if( csNewState == null ) {
                    Log.Write("NULL parent. End of play.\n");
                    return null;
                }
                csNewState.LogChild("Next parent " + csNewState.iSpawnID+" with "+csNewState.alChild.size()+" children\n");
                if( csNewState.alChild.isEmpty() == false ) {
                    csNewState = csNewState.alChild.remove(0);
                    csNewState.LogChild("Next child " + csNewState.iSpawnID+" doing "+ csNewState.aAction +"\n");
/*                    if( csNewState.iSpawnID == 300 ) {
                        Log.Write("at debug start point\n");
//                        cConfig.bLogColumns = true;
                        cConfig.bDebugLogOn = true;
                    }*/
                    return csNewState;
                }
            } while( csNewState != null );
        }
        return csNewState;
    }
}
