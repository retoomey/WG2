/* Experimental read Chinese CINRAD-SA data
 * Reverse engineer binary data files
 * 
 */
package org.wdssii.datatypes.builders;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * Read 
 * @author Robert Toomey
 */
public class ConradSA2 extends BinaryFile {

    private final static Logger LOG = LoggerFactory.getLogger(ConradSA2.class);
    // Channel Terminal Manager (12 bytes on disk)
    public int[] myCTMHeader;
    // Message Header (16 Bytes on disk)
    public int MessageSize;
    public int ChannelID;
    public int MessageType;
    public int IDSequence;
    public Date MessageGenerationDate;
    //public int MessageGenerationTime;  Part of Date 
    public int MessageSegmentsNumber;
    public int MessageSegmentNumber;
    
    // UNSIGNED byte arrays.  Just using the byte directly will false show
    // negative values.  Have to (b & 0xFF) upcast to int to get correct positive
    // value
    public byte[] dBZ;
    public byte[] Vel;
    public byte[] SW;
    

    public static Date readJulianDateFromLong(InputStream i) throws IOException {
        // Do the date...
        int days = readUnsignedShort(i);
        long time = readUnsignedLong(i);  // The c++ binary file is using 4 bytes for long
        
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        try {
            Date d = sdf.parse("01/01/1970");
            Calendar c = Calendar.getInstance();
            c.setTime(d); // Now use today date.
            c.add(Calendar.DATE, days); // Adding  days
            
            long u = time/1000; // Ahhh the C++ must do this to go from ms to secs...
                                // FIXME: We could make our date more accurate
            int hour = (int)(u/3600);
            int min = (int)((u-hour*3600)/60);
            int sec = (int)((u-hour*3600)-min*60);
            
            c.set(Calendar.HOUR_OF_DAY,hour);
            c.set(Calendar.MINUTE,min);
            c.set(Calendar.SECOND,sec);
            c.set(Calendar.MILLISECOND,0);  // Zero for the moment...

            SimpleDateFormat out = new SimpleDateFormat("MM/dd/yyyy hh::mm::ss");
            LOG.debug("Calculated data date test: " + out.format(c.getTime()));
            // FIXME:? Seems greater by 1 day...possibly need a -1 here
            return c.getTime();
        } catch (Exception dontcare) {
            LOG.debug("Exception doing date " + dontcare.toString());
        }
        return new Date();
    }

    public void readRecord(File f){
        
    }
    public void readData(File f) {

        // Don't want this.  We just want the header...
        // byte[] result = new byte[(int) f.length()];
        int recordCount = 0;
        try {
            InputStream input = null;
            try {
                // Why not use url.openStream instead?
                // FIXME: do the compressed wrapper thingie
                input = new BufferedInputStream(new FileInputStream(f));

                for(int y = 0; y< 10000000; y++){
                //LOG.debug("Read CTM");
                // Channel Terminal Manager 12 bytes
                myCTMHeader = readUShortArray(input, 6);

                // Message header 16 bytes
                //LOG.debug("Read Message Header");
                MessageSize = readUnsignedShort(input);
                ChannelID = readChar(input);
                MessageType = readChar(input);
                IDSequence = readShort(input);

                MessageGenerationDate = readJulianDateFromLong(input);
                
                //readUnsignedLong(input);    // MessageGenerationTime
                readShort(input);           // MessageSegmentsNumber
                readShort(input);           // MessageSegmentNumber

                // Digital Radar Data Header 100 bytes
                //LOG.debug("Read Radar Data Header");
                readUnsignedLong(input);    // RadialColectionTime
                readUnsignedShort(input);   // RadialCollectionDate
                readUnsignedShort(input);   // UnambiguousRange
                readUnsignedShort(input);   // AzimuthAngle
                readShort(input);           // AzimuthNumber
                //LOG.debug("RadialStatus is ");
                readShort(input);           // RadialStatus  (Should be 3)
                
                int ea = readShort(input);           // ElevationAngle
                LOG.debug("Elevation angle is "+ea);
                readShort(input);           // ElevationNumber
                readShort(input);           // FirstGateRangeOfRef
                readShort(input);           // FirstGateRangeOfDoppler
                readShort(input);           // ReflectivityGateSize
                readShort(input);           // DopplerGateSize
                readShort(input);           // ReflectivityGates
                readShort(input);           // DopplerGates
                readShort(input);           // SectorNumber

                readUnsignedLong(input);         // CalibrationConstant   ***** FLOAT NOT LONG

                // Offset into the data I think....
                readUnsignedShort(input);   // ReflectivityPointer
                readUnsignedShort(input);   // VelocityPointer
                readUnsignedShort(input);   // SpectrumWidthPointer
                readShort(input);           // VelocityResolution
                LOG.debug("This is the VCP:");  // We get 21.  Sweet.  Closer
                readShort(input);           // VCP

                readShortArray(input, 4);   // Used[4]


                readUnsignedShort(input);   // RefPlaybackPointer
                readUnsignedShort(input);   // VelPlaybackPointer
                readUnsignedShort(input);   // SWPlaybackPointer

                readShort(input);           // NyquistVelocity
                readShort(input);           // AAF
                readShort(input);           // TOVER
                readShortArray(input, 17);           // Spares[17]
                
                
                // Non-header stuff...
                
                dBZ = readRawCharArray(input, 460);
                Vel = readRawCharArray(input, 920);
                SW = readRawCharArray(input, 920);
                
                readUnsignedLong(input);         // Trailer
                recordCount++;
                }
// Try to kill it...
                //readRawCharArray(input, 500000);
                //}
                /*
                 the above style is a bit tricky: it places bytes into the 'result' array; 
                 'result' is an output parameter;
                 the while loop usually has a single iteration only.
                 */
                // log("Num bytes read: " + totalBytesRead);
            } finally {
                // log("Closing input stream.");
                input.close();
            }
        } catch (FileNotFoundException ex) {
            //log("File not found.");
        } catch (IOException ex) {
            //log(ex);
        }
LOG.debug("Final record count of "+recordCount);
    }
}
