/**
*   @author Brad Denniston, 7 Sept 2014
 */

package spidersim;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;


/**
 *
 * @author braddenn
 */
public class XMLConfigReader {

    private String              sFilename;
    private DefaultHandler      handler;
    private SAXParserFactory    factory;
    private boolean             DEBUG;
//    private LogMsg              mLogMsg;
    
    public XMLConfigReader( String fn ) 
    {
        sFilename = fn;
        // create catcher for the SAX generated events
        handler = new XMLConfigHandler( );

        // Use the default (non-validating) parser
        factory = SAXParserFactory.newInstance();
    }
    
    /**
     * LoadFilter - read file, build filter
     * @param AppIntView the GUI window for message output
     * @return  Returns.OK
     * @return  Returns.PARSE
     * @return  Returns.IO
     */
    public Enums LoadConfig () {
       // Writer  out;

        try {
            // parse the input
            // out = new OutputStreamWriter( System.out, "UTF8");
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse( new File( sFilename ), handler );
        } catch( SAXParseException e ) {
            RunSim.Log.Write( "SAX ParseException: " + "** Parsing error" +
                    ", line " + e.getLineNumber()
                    + ", uri " + e.getSystemId());
            RunSim.Log.Write( "SAXParseException: " +e.getMessage());
             return Enums.PARSE;
        } catch( ParserConfigurationException e ) {
            RunSim.Log.Write( "SAX ParserConfigurationException: " + e.getMessage() );
            // e.printStackTrace();
            return Enums.PARSE;
        } catch( UnsupportedEncodingException e ) {
            RunSim.Log.Write( "UnsupportedEncodingException: " + e.getMessage() );
            // e.printStackTrace();
            return Enums.PARSE;
        } catch( SAXException e ){
            // Error generated by this application
            // (or a parser-initialization error)
            Exception  x = e; // check for included exceptions
            if (e.getException() != null)
                x = e.getException();
            RunSim.Log.Write( "SAXException: " + x.getMessage() );
            // x.printStackTrace();
            return Enums.PARSE;
        } catch( IOException e ) {
            RunSim.Log.Write( "IOException: " + e.getMessage() );
            return Enums.FILE_READ_ERROR;
        }
        
        return Enums.OK;
    }
}
