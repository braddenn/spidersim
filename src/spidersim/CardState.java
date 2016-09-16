/*
*   @author Brad Denniston, 6 Sept 2014
* an object containing the current state of the cards in play.
*/
package spidersim;

import java.util.*;
import java.io.*;

public class CardState {
    public int          iDeckPosition;      // 0 to 5 - points to the next set in the stack to be delt.
    public CardState    csParent;           // parent reference
    public ArrayList<CardState>    alChild;            // reference to child cardstate
    public Enums        aAction;            // first step for child to take
    public int          iSpawnID;           // child count for this child, if 0 then done
    public  int         iRack = 0 ;         // count of completions, 8 is a win
    
    private int         iActionSrc;         // source of set for an action
    private int         iActionDst;         // destination of set for an action
    private boolean     bThisIsAChild;
    private int         iFirstMerge;        // where to start merging in the child
    private ArrayList<ArrayList<ArrayList<Card>>>   alTenColumns;   // 10 columns of CardSets for face up cards
    private ArrayList<ArrayList<ArrayList<Card>>>   alHiddenCards;    // 10 columns of CardSets for hidden cards
    private int[]       aiHiddenIndex = {5,5,5,5,4,4,4,4,4,4}; // number of hidden cards used in each column
    private int[]       iaAttachCount = {0,0,0,0,0,0,0,0,0,0}; // number of attaches in each column
    private int[]       iaSrcHistory = {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
    private int[]       iaDstHistory = {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
    private int         iHistory = 0;
    
    /**
     * CardState - constructor creates columns, sets indicies
     */
    public CardState( ) {
        csParent = null;
        iDeckPosition = 0;
        iSpawnID = RunSim.iChildCount;
        alChild = new ArrayList<>();
        iActionSrc = -1;
        iActionDst = -1;
        bThisIsAChild = false;
        int i, j;

        // column of faceup cards where play takes place
        // Array with 10 arrays of CardSet
        alTenColumns = new ArrayList<>();
        for( i=0; i<10; i++) { // add a double array into a triple array
            ArrayList<ArrayList<Card>> alCardSets = new ArrayList<>();
            alTenColumns.add( alCardSets );
        }
        
        // 10 columns of hidden cards that are exposed one at a time
        alHiddenCards = new ArrayList<>();
        
        for( i=0; i<10; i++) { // 10 columns
            ArrayList<ArrayList<Card>> alColumn = new ArrayList<>();
            alHiddenCards.add(alColumn);
        } 
    }
    
    /**
     * CardState - constructor creates columns, sets indicies
     */
    public CardState( CardState csMother, Enums eAction, int iSrc, int iDst ) {
        int iMomSetCount, iMomCardCount;
        int iCardCount, iCol, iCard;
        ArrayList<ArrayList<Card>> alalSet, alalMomSet;
        ArrayList<Card> alSet, alMomSet;
        Card oCard;
        
        alChild = new ArrayList<>();
        iSpawnID = ++RunSim.iChildCount;
        csParent = csMother;
        iDeckPosition = csMother.iDeckPosition;
        aAction = eAction;
        iActionSrc = iSrc;
        iActionDst = iDst;
        bThisIsAChild = true;
        alHiddenCards = csMother.alHiddenCards;
        
        // column of faceup cards where play takes place
        // Array with 10 arrays of arrays of CardSets arrays of cards
        alTenColumns = new ArrayList<>();
        iHistory = csMother.iHistory;
        for( iCol=0; iCol<10; iCol++) {
            iaDstHistory[iCol] = csMother.iaDstHistory[iCol];
            iaSrcHistory[iCol] = csMother.iaSrcHistory[iCol];
            aiHiddenIndex[iCol] = csMother.aiHiddenIndex[iCol];
            iaAttachCount[iCol] = csMother.iaAttachCount[iCol];
            alalSet = new ArrayList<>(); // column of sets
            alTenColumns.add( alalSet ); // add double arrays into a triple array
        
            // in each column of alalSet add alsets
            alalMomSet = csMother.alTenColumns.get(iCol);
            iMomSetCount = alalMomSet.size();            
            for( int iSet=0; iSet<iMomSetCount; iSet++ ) {
                alSet = new ArrayList<>();
                alalSet.add(alSet);
                
                // for each alLastSetOfSrcCards add the card from the mother
                // don't need new ones as they don't change
                iMomCardCount = alalMomSet.get(iSet).size();
                for( iCard=0; iCard<iMomCardCount; iCard++ ) {
                    alSet.add(alalMomSet.get(iSet).get(iCard));
                }
            }
        }
        
        // columns of hidden cards that are exposed one at a time
        alHiddenCards = csMother.alHiddenCards;
        
        // need 8 to win
        iRack = csMother.iRack;
    }
    
    /**
     * DealToHidden - deal from deck 10 sets to each column + 4 more
     * The local deck index ends pointing to the first 10 for dealing cards
     * @param deck 
     */
    public void DealToHidden( ArrayList<ArrayList<Card>> deck ) {
        int iColIndex;
        ArrayList<Card> alSet;
        Card cNewCard;

        for( int iRow = 0; iRow < 5; iRow++ ) {
            if( iRow == 0 ) {
                for( iColIndex =0; iColIndex < 4; iColIndex++ ) {
                    // get the column of CardSets, then add a CardSet from the deck
                    // does this work if I just put in a reference to the deck card?   YES
                    cNewCard = new Card( deck.get( iDeckPosition++ ).get(0) );
                    alSet = new ArrayList<>();
                    alSet.add(cNewCard);
                    alHiddenCards.get(iColIndex).add( alSet );
                }
            }
            else {
                for( iColIndex = 0; iColIndex < 10; iColIndex++ ) {
                    cNewCard = new Card( deck.get( iDeckPosition++ ).get(0) );
                    alSet = new ArrayList<>();
                    alSet.add(cNewCard);
                    alHiddenCards.get(iColIndex).add( alSet );
                }
            }
        }
    }

    /**
     * DealColumnCards - add one card to each of 10 columns of the CardState
     *      The CardState has an offset into the deck.
     * @param csState
     * @return - WIN - moved 13 to rack, rack is now full
     * @return - OK - added one card to each column, may have moved 13 to rack 
     */
    public Enums DealColumnCards( ArrayList<ArrayList<Card>> deck ) {
        int iColIndex, iColSize;
        Card cDeckCard, cColCard;
        
        ArrayList<ArrayList<Card>> alalColumn;
        ArrayList<Card> alDeckSet, alColSet;
        
        if( iDeckPosition >= 104 )
            return Enums.END_OF_DECK;

        // copy to the 10 columns
        for( iColIndex =0; iColIndex < 10; iColIndex++ ) {
            // create a new card, copy the value from the deck
            alDeckSet = new ArrayList<>();
            cDeckCard = new Card((deck.get( iDeckPosition++ )).get(0)); // only one card in a deck set
            alDeckSet.add(cDeckCard);
            
            // add a CardSet from the deck
             alalColumn = alTenColumns.get(iColIndex);
             iColSize = alalColumn.size();
             if( iColSize == 0 ) {
                alalColumn.add(alDeckSet);
             }
             else
             {
                alColSet = alalColumn.get(iColSize - 1);
                if( alColSet.isEmpty() ) {
                    alColSet.add(cDeckCard);
                }
                else {
                    // cColCard = new Card(alColSet.get(alColSet.size() - 1));
                    cColCard = alColSet.get(alColSet.size() - 1);
                    // merge sets if legal else add set
                    if( cDeckCard.value == (cColCard.value - 1) && cDeckCard.color == cColCard.color )
                    {
                        alColSet.add( cDeckCard );
                        // check for 13 cards, if found ++Rack
                        if( alColSet.size() == 13 )
                        {
                            alalColumn.remove(iColSize - 1);
                            if( ++iRack == 8 )
                                return Enums.WIN;
                        }
                    }
                    else {
                        if( cDeckCard.value == (cColCard.value - 1 ) && cDeckCard.color != cColCard.color )
                            iaAttachCount[iColIndex]++;
                        alalColumn.add( alDeckSet );
                    } 
                }
            }
        } 
        LogColumns("After the deal\n");
        return Enums.OK;
    }
    
    /**
     * FillSpacesFromHidden - for each space in columns try to fill from
     * hidden cards. 
     * @return SPACE_EXISTS if no more hidden cards (column still empty) 
     * @return OK if spaces have been filled or no spaces
     */
    private Enums FillSpacesFromHidden( ) {
        int iColumnNumber;
        boolean bFoundHidden = false;
        
        for( iColumnNumber = 0; iColumnNumber < 10; iColumnNumber++ ) {
            if( alTenColumns.get(iColumnNumber).isEmpty()
                && aiHiddenIndex[iColumnNumber] > 0 ){
                // found an empty  column, copy next avaiable hidden card into it
                GetHiddenSet( iColumnNumber );
                bFoundHidden = true;
            }
        }
        if( bFoundHidden == false )
            FillEmptyColumns();
        return Enums.OK; // no empty columns found
    }
    
    /**
     * GetHiddenSet - fill empty column from latest hidden. If no hidden then
     *      FillEmptyColumns
     * @param int iCol - the index of the empty column
     */
    private Enums GetHiddenSet( int iCol ) {
        Card cTmpCard;
        int iHiddenIndex;
        
        if( aiHiddenIndex[iCol] > 0 ) {
            Card cNewCard = new Card();
            ArrayList<Card> alSet = new ArrayList<>();
            iHiddenIndex = aiHiddenIndex[iCol] - 1;
            cTmpCard = alHiddenCards.get(iCol).get(aiHiddenIndex[iCol]-1).get(0);
            cNewCard.color = cTmpCard.color;
            cNewCard.value = cTmpCard.value;
            alSet.add(cNewCard);
            aiHiddenIndex[iCol] = iHiddenIndex;
            alTenColumns.get(iCol).add(alSet);
        }   
        else
            FillEmptyColumns();
        return Enums.OK;
    }
   /**
     * MoveBottomSetOfSrcToBottomOfDst
     * params - int iSrc source column, int iDst dest column
     * returns - EMPTY_SOURCE if no cards in source column
     * returns - OK if move was successful
     */
    private Enums MoveBottomSetOfSrcToBottomOfDst( int iSrc, int iDst ) {
        ArrayList<Card> alSet = new ArrayList<>();
        
        // LogColumns( "MoveBottom:move iSrc = "+(iSrc+1)+" to iDst = "+(iDst+1)+"\n");
        int iSize = alTenColumns.get( iSrc ).size();
        if( iSize == 0 ) 
            return Enums.EMPTY_SOURCE;
        alSet = alTenColumns.get(iSrc).remove( iSize - 1 );
        alTenColumns.get(iDst).add(alSet);
        return Enums.OK;
    }
    
    /**
     * FindAllMergables - for column iDst find all bottom sets that can merge
     * They can merge if the colors are the same and src value is dst value - 1
     * @param int iDst - column number of the merge target
     * @return int array that is the index of the card for each last set that 
     *      can be merged. Value is -1 if cannot be merged. Last array
     *      entry, [10], is the count of merges;
     */
    private int[] FindAllMergables( int iDst ) {
        int[] aiMerges = new int[11];
        Card cDstCard, cSrcCard;
        ArrayList<ArrayList<Card>> alSrcColumn;
        ArrayList<ArrayList<Card>> alDstColumn;
        ArrayList<Card> alLastSetOfSrcCards;
        ArrayList<Card> alLastSetOfDstCards;
        int iSrc;
        int iDstSetSize;
        
        for( iSrc = 0; iSrc<10; iSrc++) {
            aiMerges[iSrc] = -1; // -1 means does not attach
        }
        aiMerges[10] = 0;
        
        // get the destination card - last card in last set
        alDstColumn = alTenColumns.get(iDst);
        if(alDstColumn.isEmpty() )
            return aiMerges;
        alLastSetOfDstCards = alDstColumn.get(alDstColumn.size() - 1);
        if( alLastSetOfDstCards.isEmpty() )
            return aiMerges;
        
        // get the bottom card of this column
        iDstSetSize = alLastSetOfDstCards.size();
        cDstCard = alLastSetOfDstCards.get(iDstSetSize - 1);
        
        for( iSrc = 0; iSrc<10; iSrc++) {
            if( iSrc == iDst ) 
                continue;
            if( CheckHistory( iSrc, iDst) == Enums.FAILED )
                continue;
            // get the source column
            alSrcColumn = alTenColumns.get(iSrc);
            // get last set in source column
            if( alSrcColumn.size() > 0 ) {
                alLastSetOfSrcCards = alSrcColumn.get(alSrcColumn.size() - 1);
                // get first card in source set
                int iSrcSetSize = alLastSetOfSrcCards.size();
                if( iSrcSetSize == 0 ) 
                    continue;
                cSrcCard = alLastSetOfSrcCards.get(0);
                if( cDstCard.color == cSrcCard.color ) {
                    for( int iCard = 0; iCard < iSrcSetSize ; iCard++) {
                        cSrcCard = alLastSetOfSrcCards.get(iCard);
                            // can they merge?
                        if( cDstCard.value - 1 == cSrcCard.value ) {
                            // is it worth doing, is result bigger than source?
                            if( iDstSetSize + (iSrcSetSize - iCard) > iSrcSetSize ) {
                                aiMerges[iSrc] = iCard;
                                aiMerges[10]++;
                            }
                        }
                    }
                }
            }
        }
        return aiMerges;
    }
    
    /**
     * FindAllAttaches - for column iDst find all bottom sets that can merge
     * They can merge if the colors are NOT the same and src value is dst value - 1
     * @param int iDst - column number of the merge target
     * @return boolean array that is true for each last set that can be merged
     */
    private int[] FindAllAttaches( int iDst ) {
        int[] aiAttaches = new int[11];
        Card cDstCard, cSrcCard; 
        Card cSrcPrevCard = null;
        ArrayList<ArrayList<Card>> alSrcColumn, alDstColumn;
        ArrayList<Card> alDstSet, alSrcSet, alPrevSet;
        int iSetCount;
        
        // get the destination card - last card in last set
        alDstColumn = alTenColumns.get(iDst);
        alDstSet = alDstColumn.get(alDstColumn.size() - 1);
        if( alDstSet.isEmpty() ) // no cards in Dst set
            return aiAttaches;
        cDstCard = alDstSet.get(alDstSet.size() - 1);
        
        aiAttaches[10] = 0; // number of attaches
        for( int iSrc = 0; iSrc<10; iSrc++) {
            aiAttaches[iSrc] = -1;
            if( iSrc == iDst ) 
                continue;
            if( CheckHistory( iSrc, iDst) == Enums.FAILED )
                continue;
            // get the source card  column
            alSrcColumn = alTenColumns.get(iSrc);
            // get last set in column
            iSetCount = alSrcColumn.size();
            if( iSetCount > 0 ) {
                // get the last card in the prev set
                // - if same as dst card do not attach
                if( iSetCount > 1 ) {
                    alPrevSet = alSrcColumn.get(iSetCount - 2);
                    cSrcPrevCard = alPrevSet.get(alPrevSet.size()-1); // else it is null
                }
                alSrcSet = alSrcColumn.get(iSetCount - 1);
                // get first card in that set
                if( !alSrcSet.isEmpty() ) {
                    cSrcCard = alSrcSet.get(0);
                    // can they merge? not if src and dst cards are the same
                    if( cSrcPrevCard != null ) {
                        if( cSrcPrevCard.value != cDstCard.value
                            && cSrcPrevCard.color != cDstCard.color 
                            && cDstCard.value - 1 == cSrcCard.value 
                            && cDstCard.color != cSrcCard.color ) {
                            aiAttaches[iSrc] = 0;
                            aiAttaches[10]++;
                        }
                    }
                    else if( cDstCard.value - 1 == cSrcCard.value 
                             && cDstCard.color != cSrcCard.color ) {
                        aiAttaches[iSrc] = 0;
                        aiAttaches[10]++;
                    }
                }
            }
        }
        return aiAttaches;
    }
    
    /**
     * FillEmptyColumns from other columns, spawn children to try all combos
     * if move leaves empty column don't do it. Object is to flip hidden cards.
     * Only do one column then let next merge occur
     * @returns OK if no problems
     * @returns EMPTY_SOURCE if move not made due to no set in source location
     * @returns SPACE_EXISTS if still an empty column (no hidden card or empty source)
     */
    private Enums FillEmptyColumns() {
        int iDst, iSrc, iChild;
        int GetsFromHiddenCount, iMoveBottomSetCount;
        int[] iSrcIndicies = new int[10];
        ArrayList<Card>alSet;
        Enums RetVal = Enums.OK;
        
        if( bThisIsAChild ) { // if a child start where parent left off
            LogColumns("FillEmptyColumns - child - src= "+(iActionSrc+1)+" dst= "+(iActionDst+1)+"\n");
            if( Enums.EMPTY_SOURCE == MoveBottomSetOfSrcToBottomOfDst( iActionSrc, iActionDst ) ) {
                LogColumns("FillEmptyColumns - after move, empty source, fill from hidden\n");
                RetVal = FillSpacesFromHidden(); // already checked that hidden exists else loops
            }
            LogColumns("FillEmptyColumns - after fill and possibly hidden\n");
            bThisIsAChild = false;
            iActionSrc = iActionDst = 0;
            return RetVal;
        }
     
        // Only do one column then let next merge occur
        for( iDst=0; iDst<10; iDst++) {
            GetsFromHiddenCount = 0;
            iMoveBottomSetCount = 0;
            if( alTenColumns.get(iDst).isEmpty())  { // if space iDst is empty, fill from other columns
                LogColumns("FillEmptyColumns - Column "+(iDst+1)+" is empty.");
                for( iSrc=0; iSrc<10; iSrc++ ){
                    if(iSrc == iDst )
                        continue;
                    // mark one-set columns with hidden cards available
                    if( alTenColumns.get(iSrc).size() == 1
                        && aiHiddenIndex[iSrc] != 0) {
                            GetsFromHiddenCount++;
                            iSrcIndicies[iSrc] = 1;
                    }
                    else if(alTenColumns.get(iSrc).size() > 1){
                        // this is more than one set, will move bottom set
                        iMoveBottomSetCount++;
                        iSrcIndicies[iSrc] = 2;
                    }
                    else
                        iSrcIndicies[iSrc] = 0;
                }
                if( GetsFromHiddenCount > 0 ) {
                    for( iSrc=0; iSrc<10; iSrc++ ){
                        if( iSrcIndicies[iSrc] == 1 ) {
                            if( GetsFromHiddenCount-- > 1 ) {
                                // before doing fill, spawn children for other filler sources to this space
                                CardState csNew = new CardState( this, Enums.EMPTY, iSrc, iDst );
                                alChild.add(csNew);  // this child operates on next Src column
                                LogChild("Parent "+iSpawnID+" creating EMPTY child "+ (RunSim.iChildCount) + " col "+(iSrc+1)+" to " + (iDst+1) + "\n");
                            }
                            else { // last one, do this move now
                                LogColumns("FillEmptyColumns - fill column "+(iDst+1)+" from "+(iSrc+1)+" then get hidden card.\n");
                                MoveBottomSetOfSrcToBottomOfDst( iSrc, iDst );
                                LogColumns("FillEmptyColumns - after move.\n");
                                GetHiddenSet(iSrc);
                           }
                        }
                    }
                    return Enums.OK; // spawned children, moved one set from one-set column
                }
                if( iMoveBottomSetCount > 0 ) { // move last set of each multi-set column
                    iChild = 1;
                    for( iSrc=0; iSrc<10; iSrc++ ){
                        if( iSrcIndicies[iSrc] == 2 ) { // marked for move
                            if( iChild++ < iMoveBottomSetCount ) {
                                CardState csNew = new CardState( this, Enums.EMPTY, iSrc, iDst );
                                alChild.add(csNew);  // this child operates on next Src column
                                LogChild("FillEmptyColumns.Parent "+iSpawnID+" creating EMPTY child "+(RunSim.iChildCount)  + " from "+(iSrc+1)+" to " + (iDst+1) + "\n");
                            }
                            else { // last one, do it now, not in a child
                                if( alTenColumns.get(iSrc).size() > 1) {
                                    LogColumns("FillEmptyColumns - fill from column "+(iSrc+1)+" then get hidden card.\n");
                                    Enums eRetVal =  MoveBottomSetOfSrcToBottomOfDst( iSrc, iDst );
                                    LogColumns("FillEmptyColumns - after move\n");
                                    return eRetVal;
                                    // moved one attached set
                                }
                            }
                        }
                    }                   
                }
            }
        }
        return Enums.OK; // no empty columns
    }
    
     /**
     * CardSpaceAction - Action is EMPTY, MERGE, ATTACH or DEAL.
     * Do MERGES (same suit, values going down) until no change.
     * Do ATTACH (different suit, values going down) until no change.
     * Loop on MERGE, ATTACH until both do not change. Then DEAL.
     * 
     * Two copies of the cardState are spawned at every point where there are 
     * two or more options to move a set to the same destination. Then the Action is 
     * set to MERGE or APPEND for the second possible move. 
     * 
     * The child pointer of the current cardState is set to the second copy. Since this
     * is used there needs to be a second copy for the main branch.
     * 
     * Action continues the play down the main copy. After the main branch is
     * completed action moves down the child (if there is one) then up to the parent.
     * @return MERGE - continue execution by doing a merge
     * @return ATTACH - continue execution by doing an attach
     * @return DEAL - continue execution by doing a deal
     * @return WIN - this branch is a WIN, go up to next child and continue
     * @return FAILED - the aAction was not properly set
     * @return END_OF_DECK - end of this hand, no cards to deal
     */
    public Enums CardSpaceAction( Enums eAction, Deck deck ) {
        Enums eRetVal;
        int iDst;
        int iMultiMergeCount;
        
        if( eAction == Enums.EMPTY) {
            FillEmptyColumns();
            return Enums.MERGE;
        }
        
        if( eAction == Enums.MERGE ) {
            return Merge();
        }
        
        else if( eAction == Enums.ATTACH ) {
            return Attach();
        }
        
        else if( eAction == Enums.DEAL ) {
            if( DealColumnCards( deck.alalSets ) == Enums.END_OF_DECK ) {
                LogColumns( "END_OF_DECK"); 
                return Enums.END_OF_DECK;
            }
            return Enums.MERGE;
        }
        return Enums.FAILED;
    }
    
    /**
     * Merge
     * straight moves from left to right until no changes 
     * if this is a child then continue at parents dst column
     */
    private Enums Merge() {
        boolean bSomethingMerged, bSomethingAttached;
        int[] aiMerges;
        int iDst, iMultiMergeCount, iFirstDst, iFirstSrc;
        Enums eRetVal;
        
        do{ // all 10 columns
            bSomethingMerged = false;
            if( bThisIsAChild ) { // if a child start where parent left off
                iFirstDst = iActionDst;
                iFirstSrc = iActionSrc;
            }
            else {
                iFirstDst = 0;
                iFirstSrc = 0;
            }
            for( iDst=iFirstDst; iDst<10; iDst++) { 
                if( alTenColumns.get(iDst).size() > 0 ) { // skip empty columns
                    aiMerges = FindAllMergables( iDst );
                    if( aiMerges[10] > 0 ) {  // merge count
                        if( bThisIsAChild ) { // if child, only do next merge
                            bThisIsAChild = false;
                            bSomethingMerged = true;
                            eRetVal = MergeSets( iDst, iFirstSrc, aiMerges[iFirstSrc] );
                            LogColumns("Merge - Col "+(iFirstSrc+1)+" to "+(iDst+1));
                            if( eRetVal == Enums.WIN )
                                return Enums.WIN; // rack count is 8
                            if( eRetVal == Enums.SPACE_EXISTS )
                                GetHiddenSet( iFirstSrc );
                            break;
                        }
                        else if( aiMerges[10] == 1 ) { // only one merge
                            for( int iMerSrc = 0; iMerSrc<10; iMerSrc++ ) { // find first merge
                                if( aiMerges[iMerSrc] >= 0 ) { // -1 means cannot merge                                    if( bSomethingMerged == false ) {
                                    bSomethingMerged = true;
                                    eRetVal = MergeSets( iDst, iMerSrc, aiMerges[iMerSrc] );
                                    LogColumns("Merge - Col "+(iMerSrc+1)+" to "+(iDst+1));
                                    if( eRetVal == Enums.WIN )
                                        return Enums.WIN; // rack count is 8
                                    if( eRetVal == Enums.SPACE_EXISTS )
                                        GetHiddenSet( iMerSrc );
                                }
                            }
                            if( bSomethingMerged ) break;
                        }
                        else { // more than one merge.
                            iMultiMergeCount = 1;
                            for( int iMerSrc = 0; iMerSrc<10; iMerSrc++ ) { // find each merge
                                if( aiMerges[iMerSrc] >= 0 ) { // merge this source
                                    if( iMultiMergeCount++ < aiMerges[10]) { // Do children first
                                        CardState csState = new CardState( this, Enums.MERGE, iMerSrc, iDst );
                                        alChild.add(csState);
                                        LogChild("Parent "+iSpawnID+" creating MERGE child "+ (RunSim.iChildCount) + " col "+(iMerSrc+1)+" to " + (iDst+1) + "\n");
                                    }
                                    else {
                                        bSomethingMerged = true;
                                        eRetVal = MergeSets( iDst, iMerSrc, aiMerges[iMerSrc] );
                                        LogColumns("Merge - Col "+(iMerSrc+1)+" to "+(iDst+1));                                        if( eRetVal == Enums.WIN )
                                            return Enums.WIN; // rack count is 8
                                        if( eRetVal == Enums.SPACE_EXISTS )
                                            GetHiddenSet( iMerSrc );
                                    }
                                }
                            }
                            if( bSomethingMerged ) break;
                        }
                    }
                }
            }
            iActionDst = 0;
        } while( bSomethingMerged == true ); // do all columns again
        //    if( RunSim.cConfig.bDebugLogOn ) 
        LogDebug( "Merge - No Change in any column. End of merges, do one Attach.\n");       
        return Enums.ATTACH;
    }
    
    /**
     * Attach - move card of different color, value -1 to destination columns.
     * Do one attach, then back to merges
     */
    private Enums Attach() {
        int iDst, iFirstDst, iFirstSrc;
        int[] aiAttaches;
        int iMultiAttachCount;

        // if this is a child then move from iActionSrc to parents dst column
        if( bThisIsAChild ) { // child, only do this attach
            bThisIsAChild = false;
            AttachSets( iActionDst, iActionSrc );
            FillSpacesFromHidden();
            LogColumns( "Attach - Child. Col "+(iActionSrc+1)+" to "+(iActionDst+1)+"Go to Merge");
            iActionSrc = 0;
            iActionDst = 0;
            return Enums.MERGE;
        }

            // moves from left to right 
        for( iDst=0; iDst<10; iDst++) {
            if( alTenColumns.get(iDst).size() > 0 ) { // skip empty columns
                aiAttaches = FindAllAttaches( iDst );
                if( aiAttaches[10] > 0 ) { // there are 2 or more attaches
                    if( aiAttaches[10] == 1 ) { // only one attach
                        for( int iAttachSrc = 0; iAttachSrc < 10; iAttachSrc++ ) {
                            // limit number of attaches per column else cannot be undone
                            if( aiAttaches[iAttachSrc] >= 0 // -1 is no attach
                                && iaAttachCount[iDst] <= RunSim.cConfig.iAttachMax ) {
                                AttachSets( iDst, iAttachSrc );
                                iaAttachCount[iDst]++;
                                if( iaAttachCount[iAttachSrc] > 0 )
                                    iaAttachCount[iAttachSrc]--;
                                FillSpacesFromHidden();
                                LogColumns( "Attach - Col "+(iAttachSrc+1)+" to "+(iDst+1)+" Go to Merge"); 
                                return Enums.MERGE;
                            }
                        }
                    }
                    else { // found a multiple attaches, do them in child branches
                        iMultiAttachCount = 0;
                        for( int iAttSrc = 0; iAttSrc<10; iAttSrc++ ) { // find each attach
                            if( aiAttaches[iAttSrc] >= 0 ) { // attach this source
                                if( ++iMultiAttachCount < aiAttaches[10]) { // Do children first   
                                    CardState csState = new CardState( this, Enums.ATTACH, iAttSrc, iDst );
                                    alChild.add(csState);
                                    LogChild("Parent "+iSpawnID+" creating ATTACH child "+ (RunSim.iChildCount) + " col "+(iAttSrc+1)+" to " + (iDst+1) + "\n");
                                }
                                else {
                                    LogColumns("Attach - did last attach col "+(iAttSrc+1)+" to " + (iDst+1) + "\n");
                                    AttachSets( iDst, iAttSrc );
                                    FillSpacesFromHidden();
                                    return Enums.MERGE;
                                }   
                            }
                        } 
                    }
                }  
            }
        }
        LogDebug( "Attach - No attach & no merge - DEAL\n");
        return Enums.DEAL; // nothing attached
    }
    
    /**
     * MergeSets - remove bottom set from Src, merge to bottom set of Dst,
     *      if new length of Dst is 13 move the set to the rack
     * @param iDst - index of the destination column
     * @param iSrc - index of the source column
     * @param iCard - index of the first card in the source set
     * @return - WIN if this added 8th set to the rack
     * @return - CONTINUE
     * @return - SPACE_EXISTS if source column is left empty
     */
    public Enums MergeSets( int iDst, int iSrc, int iCard ) {
        ArrayList<ArrayList<Card>> alColOfSets;
        ArrayList<Card> alDstSetOfCards, alSrcSetOfCards;

        // get the destination column then last set
        alColOfSets = alTenColumns.get(iDst);
        alDstSetOfCards = alColOfSets.get(alColOfSets.size() - 1);
        
        // get the source column then remove last source set
        alColOfSets = alTenColumns.get(iSrc);
        alSrcSetOfCards = alColOfSets.get(alColOfSets.size() - 1);
        
        int iSrcSetSize = alSrcSetOfCards.size();  

        if( iSrcSetSize > 0 ) {
            int iRemoveCard = iCard;
            for( int i = iCard; i < iSrcSetSize; i++) {
                Card tmpCard = alSrcSetOfCards.remove(iRemoveCard);
                alDstSetOfCards.add(  new Card(tmpCard.value, tmpCard.color) );
            }
        }
        if( alSrcSetOfCards.isEmpty() ) {
            alSrcSetOfCards = alColOfSets.remove(alColOfSets.size() - 1);
        }
        
        // if 13 cards in last set then remove the set, increment rack count
        if( alDstSetOfCards.size() == 13 )
        {
            alColOfSets = alTenColumns.get(iDst);
            alColOfSets.remove(alColOfSets.size() - 1);
            if( ++iRack == 8 )
                return Enums.WIN;
            LogDebug( "MergeSets - Moved 13 cards to iRack now at "+iRack+"\n");
        }
        if( alTenColumns.get(iSrc).isEmpty() ) 
            return Enums.SPACE_EXISTS;
        return Enums.CONTINUE;
    }
    
    /**
     * AttachSets - remove bottom set from Src, add to bottom set of Dst,
     * @param iDst - index of the destination column
     * @param iSrc - index of the source column
     */
    public void AttachSets( int iDst, int iSrc ) {
        ArrayList<ArrayList<Card>> alSrcColOfSets;
        ArrayList<ArrayList<Card>> alDstColOfSets;
        
        // get the source and destination columns
        alSrcColOfSets = alTenColumns.get(iSrc);
        alDstColOfSets = alTenColumns.get(iDst);
        
        // now add last source set to dest column
        ArrayList<Card> alSrcSet = alSrcColOfSets.remove(alSrcColOfSets.size() - 1);
        int iSrcSetSize = alSrcSet.size();
        ArrayList<Card> alDstSet = new ArrayList<>();
        for( int i=0; i < iSrcSetSize; i++) {
            Card tmpCard = alSrcSet.remove(0);
            alDstSet.add(  new Card(tmpCard.value, tmpCard.color) );
        }
        alDstColOfSets.add( alDstSet );
    }
        
    
    /**
     * CheckHistory - record 10 moves, check them to stop undo moves
     * check if Dst/Src is opposite of Src/Dst
     * @return - OK - opposite move not in history or with covering move
     * @return - FAILED - opposite move in history without covering move
     */
    private Enums CheckHistory( int iSrc, int iDst ){
        int i, j;
        boolean bFirstMoveFound = false;
        boolean bDstFound = false; // if iDst does not show up in dst list then OK 
        Enums eRetVal = Enums.FAILED; // no previous covering move

        for( j=iHistory; j>iHistory-10; j-- ) {
            if( j < 0 )
                i = j + 10;
            else i = j;
            if( iaDstHistory[i] == iDst ) {
                bDstFound = true;
                if( iaSrcHistory[i] != iSrc ) { // some previous covering move
                    eRetVal = Enums.OK;
                    bFirstMoveFound = true;
                }
                if( bFirstMoveFound == true && iaSrcHistory[i] == iSrc ){ // double move OK
                    break; // if src is iSrc and another iDst/iSrc pair found then OK
                }
            }
        }

        // add new iSrc/iDst pair at ++iHistory
        iaSrcHistory[iHistory] = iSrc;
        iaDstHistory[iHistory] = iDst;
        if( ++iHistory >= 10 )
            iHistory = 0;
        if( bDstFound == false ) // if iDst does not show up in dst list then OK 
            return Enums.OK;
        return eRetVal;
    }

    /** LogDeck - write the deck cards to the log file
     * @param - dObj - deck to write
     * @return - Enums
     */
    public void LogDeck( Deck dObj ) {
        String sLineout;

        // deck - after deal to hidden and columns, show 50 cards
        LogDebug( " The remaining deck: \n");
        
        int iCard = 54;
        sLineout = "";
        for( int k=0; k<5; k++) {
            for( int i=0; i<10; i++, iCard++ ) {
                Card cCard = dObj.alalSets.get(iCard).get(0);
                sLineout = sLineout + (cCard.value < 10 ? " " : "") + cCard.value + ((cCard.color == Enums.BLACK) ? "B " : "R ");   
            }
            LogDebug( sLineout + "\n" );
            sLineout = "";
        }
    }
    
    /* LogHidden - write the hidden cards to the log file
     * hidden - show 5 rows, 10 columns, 3 char/col
     * @param - csObj - CardState with hidden to write
     * @return - Enums
     */
    public void LogHidden( ) {
        String sLineout;
        ArrayList<Card> alSet;
        Card cCard;
        int iRow;

        // Hidden - after deal to hidden show 54 cards
        LogDebug( " The hidden cards: \n");

        sLineout = "";
        for( iRow=0; iRow<4; iRow++) { // 5 rows
            for( int iCol=0; iCol<10; iCol++ ) { // 10 columns per row
                cCard = alHiddenCards.get(iCol).get(iRow).get(0);
                sLineout = sLineout + (cCard.value < 10 ? " " : "") + cCard.value + ((cCard.color == Enums.BLACK) ? "B " : "R ");
            }
            LogDebug( sLineout + "\n" );
            sLineout = "";
        }
        iRow = 4;
        for( int iCol=0; iCol<4; iCol++ ) {
            cCard = alHiddenCards.get(iCol).get(iRow).get(0);
            sLineout = sLineout + (cCard.value < 10 ? " " : "") + cCard.value + ((cCard.color == Enums.BLACK) ? "B " : "R ");              
        }
        LogDebug( sLineout + "\n" );
    }
    
    /* LogColumns - write columns to the log file Show 10 columns of show n rows.
     * @param - csObj - CardState to write
     * @return - Enums
     */
    public void LogColumns( String sDid ) {
        String sLineout;
        ArrayList<ArrayList<Card>> alDstColumns;
        ArrayList<Card> alDstSet;
        ArrayList<ArrayList<Card>> alSrcSets;
        Card cCard;
        int iCol, iSet, iNumSrcSets, iCardCount, iCard;
        
        if( RunSim.cConfig.bLogColumns == false )
            return;
        sLineout = sDid;
        Log( sLineout + "\n" );
        sLineout = "";
        
       // for( iCol = 0; iCol<10; iCol++ ){
       //     sLineout = sLineout + " "+alTenColumns.get(iCol).size() + "  ";
       // }
       // LogDebug( sLineout + "\n" );
       // sLineout = "";
        
        for( iCol = 0; iCol<10; iCol++ ){
            sLineout = sLineout + (iCol+1)+"-"+aiHiddenIndex[iCol] + " ";
        }
        Log( sLineout + "\n" );
        sLineout = "";
        
        // move all cards in each column to a single set in new column
        // in new set of columns add one set to each column
        alDstColumns = new ArrayList<>();
        for( int i=0; i<10; i++) {
            alDstSet = new ArrayList<>();
            alDstColumns.add( alDstSet );
        }
        
        // transfer cards from existing sets to single set
        for( iCol = 0; iCol < 10; iCol++ ) {
            alDstSet = alDstColumns.get(iCol);
            alSrcSets = alTenColumns.get(iCol);
            iNumSrcSets = alSrcSets.size();
            for( iSet = 0; iSet < iNumSrcSets; iSet++ ) {
                iCardCount = alSrcSets.get(iSet).size();
                for( iCard = 0; iCard < iCardCount; iCard++ ) {
                    alDstSet.add(alSrcSets.get(iSet).get(iCard));
                }
            }
        }
        
        // get the number of cards in the longest column
        int iMaxCardCount = 0;
        for( iCol = 0; iCol< 10; iCol++ ) {
            iCardCount = alDstColumns.get(iCol).size();
            if( iCardCount > iMaxCardCount ) iMaxCardCount = iCardCount;
        }
 
       // if( aAction == Enums.MERGE ) sLineout =  "MERGE "; 
      //  else if( aAction == Enums.ATTACH ) sLineout =  "ATTACH "; 
      //  else sLineout =  "DEAL "; 
        for( int iRow=0; iRow < iMaxCardCount; iRow++) {
            for( iCol=0; iCol<10; iCol++ ) { // 10 columns per row
                iCardCount = alDstColumns.get(iCol).size();
                if( iCardCount == 0 ) {
                    sLineout = sLineout + "*** ";
                }
                else {
                    if( iRow < iCardCount ) {
                        cCard = alDstColumns.get(iCol).get(iRow);
                        sLineout = sLineout + (cCard.value < 10 ? " " : "") + cCard.value + ((cCard.color == Enums.BLACK) ? "B " : "R ");   
                     }
                     else
                         sLineout = sLineout + "    ";
                }
            }
            Log( sLineout + "\n" );
            sLineout = "";
        }
    }

   /**
    * LogRack - show the number of completed card sets - 8 is  a win
    */
    public void LogRack( ) {
        LogDebug( " The Rack size is :" + iRack +  "\n");
    }
    
    /** LogDebug - output to log file a line of text
     * @param - line of text
     */
    public void Log( String sText ) {
            RunSim.Log.Write(sText);
    }
        
    /** LogDebug - output to log file a line of text
     * @param - line of text
     */
    public void LogDebug( String sText ) {
        if( RunSim.cConfig.bDebugLogOn )
            RunSim.Log.Write(sText);
    }
       
    /** LogChild - output to log file a line of text
     * @param - line of text
     */
    public void LogChild( String sText ) {
        if( RunSim.cConfig.bLogChild )
            RunSim.Log.Write(sText);
    }
    
    /**
     * SaveStateToFile - save initial state to file enabling replay
     * a card is: n is 1-13 for red cards and 14-26 for black cards
     * Deck - 
     * 	13 columns, 8 rows
     */
    public Enums SaveStateToFile( Deck deck ) {
        String sFilename = "SpiderSimInitialState.txt";
        File file = new File(sFilename);
        String sLineout;
        ArrayList<Card> alSet;
        Card cCard;
        int i,iValue;
        
        // creates the file
        try{
            file.createNewFile();
            // creates a FileWriter Object
            FileWriter writer = new FileWriter(file); 
            
            // write Deck - deck is 104 sets.
            sLineout = "";
            for( i=0; i<104; i++ ) {
                alSet = deck.alalSets.get(i);
                cCard = alSet.get(0);
                iValue = cCard.value;
                if( cCard.color == Enums.BLACK ) iValue += 13;
                sLineout = sLineout + iValue + " ";
            }                
            writer.write(sLineout + "\n");                     
            writer.flush();
            writer.close();
        } catch(IOException e) {
            Log( "IO Exception at file SaveStateToFile");
            return Enums.FAILED;
        }
        return Enums.OK;
    }

    /**
     * LoadStateFromFile - load initial state from file enabling replay
     * a card is: n where n is 1-13 for red cards and 14-26 for black cards
     * Deck - 104 cards
     */
    public Enums LoadStateFromFile( Deck deck ) {
        String sFilename = "SpiderSimInitialState.txt";
        String sLine;
        Card cCard;
        int iValue;
        InputStream    fis;
        BufferedReader br;
        
        try {
            fis = new FileInputStream(sFilename);
            br = new BufferedReader(new InputStreamReader(fis));
            sLine = br.readLine(); // only one line of 104 values
            br.close();
        } catch( IOException e) {
            Log( "IO Exception "+e+" at file LoadStateFromFile");
            return Enums.FAILED;
        }
                // Deal with the line
        String[] result = sLine.split("\\s");
        if( result.length > 104 )
            Log( "ERROR - reading SpiderSimInitialState too many tokens - " + result.length + "\n");
        for (int i=0; i<result.length; i++) {
            iValue = Integer.parseInt(result[i]);
            cCard = deck.alalSets.get(i).get(0);
            if( iValue < 14 ) {
                cCard.value = iValue;
                cCard.color = Enums.RED;
            }
            else {
                cCard.value = iValue - 13;
                cCard.color = Enums.BLACK;
            }
        }
        return Enums.OK;
    }
}