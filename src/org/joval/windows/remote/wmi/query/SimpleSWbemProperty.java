// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.remote.wmi.query;

import java.util.Iterator;

import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.common.JIException;

import com.h9labs.jwbem.SWbemProperty;

import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.windows.wmi.WmiException;

/**
 * Wrapper for an SWbemProperty.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SimpleSWbemProperty implements ISWbemProperty {
    private SWbemProperty property;

    SimpleSWbemProperty(SWbemProperty property) {
	this.property = property;
    }

    // Implement ISWbemProperty

    public String getName() throws WmiException {
	try {
	    return property.getName();
	} catch (JIException e) {
	    throw new WmiException(e);
	}
    }

    public Object getValue() throws WmiException {
	try {
	    return property.getValue();
	} catch (JIException e) {
	    throw new WmiException(e);
	}
    }

    public Integer getValueAsInteger() throws WmiException {
	try {
	    return property.getValueAsInteger();
	} catch (JIException e) {
	    throw new WmiException(e);
	}
    }
    
    public Long getValueAsLong() throws WmiException {
	try {
	    return property.getValueAsLong();
	} catch (JIException e) {
	    throw new WmiException(e);
	}
    }

    public Boolean getValueAsBoolean() throws WmiException {
	try {
	    return property.getValueAsBoolean();
	} catch (JIException e) {
	    throw new WmiException(e);
	}
    }

    /**
     * Returns null if the value is not a String.
     */
    public String getValueAsString() throws WmiException {
	try {
	    Object obj = property.getValue();
	    if (obj instanceof JIString) {
		return ((JIString)obj).getString();
	    } else {
		return null;
	    }
	} catch (JIException e) {
	    throw new WmiException(e);
	}
    }

    /**
     * Returns null if the value is not an Array.
     */
    public String[] getValueAsArray() throws WmiException {
	try {
	    Object obj = property.getValue();
	    if (obj instanceof JIArray) {
		JIVariant[] va = (JIVariant[])((JIArray)obj).getArrayInstance();
		int len = va.length;
		String[] sa = new String[len];
		for (int i=0; i < len; i++) {
		    sa[i] = va[i].getObjectAsString2();
		}
		return sa;
	    } else {
		return null;
	    }
	} catch (JIException e) {
	    throw new WmiException(e);
	}
    }
}