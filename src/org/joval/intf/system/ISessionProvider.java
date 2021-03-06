// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.system;

/**
 * Something that provides a session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ISessionProvider {
    IBaseSession getSession();
}
