/**
 * Author: Brad Denniston 7 Sept 2014
 *
 * Description - creates and writes text to a log file. 
 * The log file wraps after 4000 lines 
 */
 
package spidersim;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Logger - open a log file. Write to the file. Close the file.
 * When writing to the file count the lines and if lines are greater than a
 * limit then exit this program.
 */
public class Logger {
    private int         outLineCount = 0;
    int                 outLineCountMax = 10000;
    FileOutputStream    logFile = null;
    File                file;
    
    public Enums OpenLogFile( String logfilename )
    {
        if( logFile != null )
            CloseLog();
        try {
            file = new File( logfilename );
            if( !file.exists() )
            {
                file.createNewFile();
            }
            logFile = new FileOutputStream(file);
        } catch (IOException e) {
            System.out.println( "Logger:OpenLogfile - Unable to open the log file at "  + logfilename );
            return Enums.FAILED;
        }
        return Enums.OK;
    }
    
    /**
     * WriteToLog - if > 4000 lines then close file and exit to system
     * @param line - text to add to log file with a line number
     * @return Enums OK or FAILED
     */
    public Enums Write( String line )
    {
        if( logFile == null )
            return Enums.OK;
        if( ++outLineCount >= outLineCountMax )
        {
            line = "Log output blocked here to stop runaway.\nChange outLineCountMax and recompile to be able to write longer files.";
            CloseLog();
            System.exit(1);
        }

        // String output = String.format("%d: %s",outLineCount, line);
        String output = String.format("%s", line);
        byte[] contentInBytes = output.getBytes();
        try 
        {
            logFile.write( contentInBytes );
            } catch (IOException e)  {
             System.out.println( "Logger:WriteToLog - Unable to write to the log file.");  
             return Enums.FAILED;
        }
        return Enums.OK;
    }
    
    public Enums CloseLog()
    {
        if( logFile == null )
            return Enums.OK;
        try {
        logFile.flush();
        logFile.close();
        outLineCount = 0;
        logFile = null;
        } catch  (IOException e) {
            System.out.println( "Unable to close the log file.");
            return Enums.FAILED;
        }
        return Enums.OK;
    }
}
