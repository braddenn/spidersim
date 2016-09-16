/*
*   @author Brad Denniston, 6 Sept 2014
*   enums for card colors and return values
 */
package spidersim;

public enum Enums {
    RED,    // hearts
    BLACK,  // spaces
    OK,     // return value
    END_OF_DECK,    // return value
    FAILED,     // return value
    PARSE,      // XML parser error
    FILE_READ_ERROR,//
    CONTINUE,       // Move operation return
    WIN,            // 8 sets to the rack
    CHANGE,         // a cardset was moved
    LOSE,           // could not complete
    SPACE_EXISTS, // no empty columns found
    EMPTY_SOURCE,   // source of move is empty
    SOMETHING_MERGED,   // at least one set was merged
    MERGE,
    ATTACH,
    DEAL,
    SPAWN,
    EMPTY,
    CHILD_MAX,
}

