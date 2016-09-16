/**
 * Author: Brad Denniston 8 Sept 2014
 *
 * Based on Java SE 6 Documentation: XML tutorial at
 * http://docs.oracle.com/javaee/1.4/tutorial/doc/index.html
 */
package spidersim;

// import java.net.InetAddress;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author braddenn
 */
public class XMLConfigHandler extends DefaultHandler 
{

    private String          currentElement = "validate";

    @Override
    public void startDocument() throws SAXException {

    }

    /**
     * startElement - overrides - sends element name to characters()
     *
     * @param sNameSpaceURI
     * @param sSimpleName - seem to always be empty
     * @param sQualifiedName - the name of the element
     * @param attrs - array, if not null, of attributes
     * @throws SAXException - catch this in XMLFilterReader
     */
    @Override
    public void startElement(
            String sNameSpaceURI,
            String sSimpleName,
            String sQualifiedName,
            Attributes attrs)
            throws SAXException 
    {
        currentElement = sQualifiedName; // sends element name to characters()
    }

    /**
     * endElement - an element is closed.
     *
     * @param sNameSpaceURI
     * @param sSimpleName
     * @param sQualifiedName - the name of the element
     * @throws SAXException
     */
    @Override
    public void endElement(
            String sNameSpaceURI,
            String sSimpleName,
            String sQualifiedName)
            throws SAXException {
          // do nothing
    }

    @Override
    public void endDocument() {
        // do nothing
    }

    @Override
    /**
     * characters
     * all elements are sent to here for simple processing
     */
    public void characters(char bBuf[], int iOffset, int iLen)
            throws SAXException 
    {
        String sTmp = new String(bBuf, iOffset, iLen).trim();

        if (sTmp.length() == 0)
            return;
 
        switch( currentElement ) 
        {
            case "NumberOfPlays":
                RunSim.cConfig.iNumberOfPlays = Integer.parseInt(sTmp); break;

            case "Randomize":
                if( sTmp.equalsIgnoreCase( "yes" ) )
                    RunSim.cConfig.bRandomize = true; break;

            case "LogFile":
                RunSim.cConfig.sLogfileName = sTmp; break;

            case "OutputFileName":
                RunSim.cConfig.sOutputFileName = sTmp; break;

            case "Debug":
                if( sTmp.equalsIgnoreCase( "on" ) )
                    RunSim.cConfig.bDebugLogOn = true; break;
                
            case "AttachMax":
                RunSim.cConfig.iAttachMax = Integer.parseInt(sTmp); break;
                
            case "LogColumns":
                if( sTmp.equalsIgnoreCase( "on" ) )
                    RunSim.cConfig.bLogColumns = true; break;
                
            case "LogChild":
                if( sTmp.equalsIgnoreCase( "on" ) )
                    RunSim.cConfig.bLogChild = true; break;
                
            case "ReadDeck":
                if( sTmp.equalsIgnoreCase( "on" ) )
                    RunSim.cConfig.bReadState = true; break;
            
            default: 
               RunSim.Log.Write("Severe: Parse error. " + currentElement + " is not a recognized element.");
               throw new SAXException("Parse error. " + currentElement + " is not a recognized element.");            
        }

        if (sTmp.matches("s*")) 
        {
            RunSim.Log.Write("Severe: Parse error. " + sTmp + " looks like a character string.");
            throw new SAXException("Parse error. " + sTmp + " looks like character string.");
        }
    }
}
