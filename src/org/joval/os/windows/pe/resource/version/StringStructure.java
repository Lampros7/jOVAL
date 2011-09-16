// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.pe.resource.version;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.joval.io.LittleEndian;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

public class StringStructure {
    short length;
    short valueLength;
    short type;
    String key;
    byte[] padding;
    String value;

    StringStructure(byte[] buff, int offset, int fileOffset) throws IOException {
	int initialOffset = offset;
	length		= LittleEndian.getUShort(buff, offset);
	offset += 2;
	valueLength	= LittleEndian.getUShort(buff, offset);
	offset += 2;
	type		= LittleEndian.getUShort(buff, offset);
	offset += 2;
	key		= LittleEndian.getSzUTF16LEString(buff, offset, -1);
	offset += (2 * key.length()) + 2;
	padding		= LittleEndian.get32BitAlignPadding(buff, offset, fileOffset);
	offset += padding.length;
	value		= LittleEndian.getSzUTF16LEString(buff, offset, 2*valueLength);
	offset += 2*valueLength;

	int computedLength = offset - initialOffset;
	if (length < computedLength) {
	    throw new IOException(JOVALSystem.getMessage(JOVALMsg.ERROR_PE_STRINGSTR_OVERFLOW, computedLength, length));
	} else {
	    //
	    // Sometimes the specified length is less than the actual length of the structure, so we correct it.
	    //
	    length = (short)computedLength;
	}
    }

    public String toString() {
	return key + "=" + value;
    }

    public void debugPrint(PrintStream out) {
	out.println("STRING:");
	out.println("  length:           " + LittleEndian.toHexString(length));
	out.println("  valueLength:      " + LittleEndian.toHexString(valueLength));
	out.println("  type:             " + LittleEndian.toHexString(type));
	out.println("  key:              " + key);
	out.print("  padding:         {");
	for (int i=0; i < padding.length; i++) {
	    if (i > 0) {
		out.print(", ");
	    }
 	    out.print(LittleEndian.toHexString(padding[i]));
	}
	out.println("}");
	out.println("  value:            \"" + value + "\"");
    }

    public String getKey() {
	return key;
    }

    public String getValue() {
	return value;
    }
}
