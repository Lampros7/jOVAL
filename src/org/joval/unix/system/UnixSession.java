// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.unix.system;

import java.io.File;

import org.joval.intf.unix.system.IUnixSession;
import org.joval.io.LocalFilesystem;
import org.joval.unix.UnixSystemInfo;
import org.joval.util.BaseSession;

/**
 * A simple session implementation for Unix machines.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnixSession extends BaseSession implements IUnixSession {
    private Flavor flavor = Flavor.UNKNOWN;

    public UnixSession() {
	super();
    }

    // Implement ISession

    public boolean connect() {
	env = new Environment(this);
	fs = new LocalFilesystem(env, null);
	cwd = new File(".");
	flavor = Flavor.flavorOf(this);
	return true;
    }

    public void disconnect() {
    }

    public int getType() {
	return UNIX;
    }

    // Implement IUnixSession

    public Flavor getFlavor() {
	return flavor;
    }
}
