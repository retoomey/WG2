package org.wdssii.gui.products;

import org.wdssii.gui.ColorMap.ColorMapOutput;
import org.wdssii.storage.Array1Dfloat;

/**
 * A subclass of ColorMapOutput that knows how to write to our internal
 * Array1Dfloat class directly as UNSIGNED_BYTE
 * 
 * @author Robert Toomey
 */
public class ColorMapFloatOutput extends ColorMapOutput {

    /** Add colors as unsigned bytes to Array1Dfloat.  Java doesn't have unsigned bytes, but open GL does.
     * Basically we store the colors into one 8 byte float, 1 unsigned byte per color RGBA
     * then glColorPointer(....unsignedbyte) works
     * 
     * Note: This technique ONLY works with a 32 bit color buffer.  But any modern system
     * can create this easy.
     */
    public int putUnsignedBytes(Array1Dfloat in, int counter) {

        // In opengl, we want byte order to be RGBA, but float in java is Big-Endian...so
        // we create an integer of the form:
        // 0xAABBGGRR (32 bits of data)
        // Stuff our 4 colors into one float in java, which is 32 bits.  Each 8 bits needs to be
        // the unsigned byte version.  We know the alpha, blue, green is positive range 0-255
        int theInt = 0;
        theInt = (alpha << 24);
        theInt |= (blue << 16);
        theInt |= (green << 8);
        theInt |= red;
        in.set(counter, Float.intBitsToFloat(theInt));

        return ++counter;
    }
}
