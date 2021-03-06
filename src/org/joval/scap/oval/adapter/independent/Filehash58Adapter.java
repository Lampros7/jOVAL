// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.independent;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.independent.EntityObjectHashTypeType;
import oval.schemas.definitions.independent.Filehash58Object;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.independent.EntityItemHashTypeType;
import oval.schemas.systemcharacteristics.independent.Filehash58Item;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFileEx;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.ISession;
import org.joval.intf.unix.io.IUnixFileInfo;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.windows.io.IWindowsFileInfo;
import org.joval.io.StreamTool;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.Checksum;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;

/**
 * Collects items for filehash58 OVAL objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Filehash58Adapter extends BaseFileAdapter<Filehash58Item> {
    enum Algorithm {
	MD5("MD5", "md5"),
	SHA1("SHA-1", "sha1"),
	SHA224("SHA-224", "sha224"),
	SHA256("SHA-256", "sha256"),
	SHA384("SHA-384", "sha384"),
	SHA512("SHA-512", "sha512");

	String ovalId, osId;

	Algorithm(String ovalId, String osId) {
	    this.ovalId = ovalId;
	    this.osId = osId;
	}

	static Algorithm fromOval(String id) throws IllegalArgumentException {
	    for (Algorithm alg : values()) {
		if (id.equals(alg.ovalId)) {
		    return alg;
		}
	    }
	    throw new IllegalArgumentException(id);
	}
    }

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof ISession) {
	    super.init((ISession)session);
	    classes.add(Filehash58Object.class);
	}
	return classes;
    }

    // Protected

    protected Class getItemClass() {
	return Filehash58Item.class;
    }

    /**
     * Parse the file as specified by the Object, and decorate the Item.
     */
    protected Collection<Filehash58Item> getItems(ObjectType obj, ItemType base, IFile f, IRequestContext rc)
		throws IOException, CollectException {

	Filehash58Item baseItem = null;
	if (base instanceof Filehash58Item) {
	    baseItem = (Filehash58Item)base;
	} else {
	    String message = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ITEM, base.getClass().getName());
	    throw new CollectException(message, FlagEnumeration.ERROR);
	}

	HashSet<Algorithm> algorithms = new HashSet<Algorithm>();
	EntityObjectHashTypeType hashType = ((Filehash58Object)obj).getHashType();
	String hash = (String)hashType.getValue();
	OperationEnumeration op = hashType.getOperation();
	switch(op) {
	  case EQUALS:
	    try {
		algorithms.add(Algorithm.fromOval(hash));
	    } catch (IllegalArgumentException e) {
		String message = JOVALMsg.getMessage(JOVALMsg.ERROR_CHECKSUM_ALGORITHM, hashType);
		throw new CollectException(message, FlagEnumeration.ERROR);
	    }
	    break;

	  case NOT_EQUAL:
	    for (Algorithm alg : Algorithm.values()) {
		if (!hash.equals(alg.ovalId)) {
		    algorithms.add(alg);
		}
	    }
	    break;

	  case PATTERN_MATCH:
	    try {
		Pattern p = Pattern.compile(hash);
		for (Algorithm alg : Algorithm.values()) {
		    if (p.matcher(alg.ovalId).find()) {
			algorithms.add(alg);
		    }
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}

	Collection<Filehash58Item> items = new Vector<Filehash58Item>();
	File temp = null;
	try {
	    //
	    // If the file is remote and we're not on Unix, then create a temporary local copy.
	    //
	    if (session.getType() != IBaseSession.Type.UNIX && !IBaseSession.LOCALHOST.equals(session.getHostname())) {
		temp = File.createTempFile("cksum", "dat", session.getWorkspace());
		StreamTool.copy(f.getInputStream(), new FileOutputStream(temp), true);
	    }
	    for (Algorithm alg : algorithms) {
		try {
		    if (temp == null) {
			items.add(getItem(baseItem, alg, computeChecksum(f, alg)));
		    } else {
			items.add(getItem(baseItem, alg, computeChecksum(new FileInputStream(temp), alg)));
		    }
		} catch (Exception e) {
		    MessageType msg = Factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(e.getMessage());
		    rc.addMessage(msg);
		    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	} finally {
	    if (temp != null) {
		temp.delete();
	    }
	}
	return items;
    }

    // Internal

    protected Filehash58Item getItem(Filehash58Item baseItem, Algorithm alg, String checksum) {
	Filehash58Item item = Factories.sc.independent.createFilehash58Item();
	item.setPath(baseItem.getPath());
	item.setFilename(baseItem.getFilename());
	item.setFilepath(baseItem.getFilepath());
	item.setWindowsView(baseItem.getWindowsView());

	EntityItemHashTypeType hashType = Factories.sc.independent.createEntityItemHashTypeType();
	hashType.setValue(alg.ovalId);
	item.setHashType(hashType);

	EntityItemStringType hash = Factories.sc.core.createEntityItemStringType();
	hash.setValue(checksum);
	item.setHash(hash);

	return item;
    }

    private String computeChecksum(IFile f, Algorithm alg) throws Exception {
	IFileEx ext = f.getExtended();
	boolean typecheck = false;
	if (ext instanceof IWindowsFileInfo) {
	    typecheck = ((IWindowsFileInfo)ext).getWindowsFileType() == IWindowsFileInfo.FILE_TYPE_DISK;
	} else if (ext instanceof IUnixFileInfo) {
	    typecheck = ((IUnixFileInfo)ext).getUnixFileType().equals(IUnixFileInfo.FILE_TYPE_REGULAR);
	}
	if (!typecheck) {
	    throw new IllegalArgumentException(f.getPath());
	}
	String checksum = null;
	switch(session.getType()) {
	  case UNIX:
	    IUnixSession us = (IUnixSession)session;
	    switch(us.getFlavor()) {
	      case AIX:
	      case LINUX:
	      case MACOSX: {
		String cmd = "openssl dgst -hex -" + alg.osId + " " + f.getPath();
		String temp = SafeCLI.exec(cmd, session, IUnixSession.Timeout.M);
		int ptr = temp.indexOf("= ");
		if (ptr > 0) {
		    checksum = temp.substring(ptr+2).trim();
		}
		break;
	      }

	      case SOLARIS: {
		String cmd = "digest -a " + alg.osId + " " + f.getPath();
		checksum = SafeCLI.exec(cmd, session, IUnixSession.Timeout.M);
		break;
	      }
	    }
	    break;

	  default:
	    checksum = computeChecksum(f.getInputStream(), alg);
	    break;
	}
	return checksum;
    }

    private String computeChecksum(InputStream in, Algorithm alg) throws Exception {
	Checksum.Algorithm ca = null;
	switch(alg) {
	  case MD5:
	    ca = Checksum.Algorithm.MD5;
	    break;
	  case SHA1:
	    ca = Checksum.Algorithm.SHA1;
	    break;
	  case SHA224:
	    ca = Checksum.Algorithm.SHA224;
	    break;
	  case SHA256:
	    ca = Checksum.Algorithm.SHA256;
	    break;
	  case SHA384:
	    ca = Checksum.Algorithm.SHA384;
	    break;
	  case SHA512:
	    ca = Checksum.Algorithm.SHA512;
	    break;
	}
	return Checksum.getChecksum(in, ca);
    }
}
