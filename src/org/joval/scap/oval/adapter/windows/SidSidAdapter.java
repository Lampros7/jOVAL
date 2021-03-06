// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.windows.SidSidObject;
import oval.schemas.definitions.windows.SidSidBehaviors;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.SidSidItem;

import org.joval.intf.system.IBaseSession;
import org.joval.intf.windows.identity.IGroup;
import org.joval.intf.windows.identity.IPrincipal;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.os.windows.wmi.WmiException;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Collects items for the sid_sid_object.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SidSidAdapter extends UserAdapter {
    // Implement IAdapter

    @Override
    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(SidSidObject.class);
	}
	return classes;
    }

    @Override
    public Collection<SidSidItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	directory = session.getDirectory();
	Collection<SidSidItem> items = new Vector<SidSidItem>();
	SidSidObject sObj = (SidSidObject)obj;
	OperationEnumeration op = sObj.getTrusteeSid().getOperation();
	String sid = (String)sObj.getTrusteeSid().getValue();
	SidSidBehaviors behaviors = sObj.getBehaviors();

	try {
	    switch(op) {
	      case EQUALS:
		items.addAll(makeItems(directory.queryPrincipalBySid(sid), behaviors));
		break;

	      case NOT_EQUAL:
		for (IPrincipal p : directory.queryAllPrincipals()) {
		    if (!p.getSid().equals(sid)) {
			items.addAll(makeItems(p, behaviors));
		    }
		}
		break;
    
	      case PATTERN_MATCH:
		try {
		    Pattern p = Pattern.compile(sid);
		    for (IPrincipal principal : directory.queryAllPrincipals()) {
			if (p.matcher(principal.getSid()).find()) {
			    items.addAll(makeItems(principal, behaviors));
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
	} catch (NoSuchElementException e) {
	    // No match.
	} catch (WmiException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINWMI_GENERAL, obj.getId(), e.getMessage()));
	    rc.addMessage(msg);
	}
	return items;
    }

    // Private

    private List<SidSidItem> makeItems(IPrincipal principal, SidSidBehaviors behaviors)
		throws WmiException {

	List<SidSidItem> items = new Vector<SidSidItem>();
	boolean includeGroups = true;
	boolean resolveGroups = false;
	if (behaviors != null) {
	    includeGroups = behaviors.getIncludeGroup();
	    resolveGroups = behaviors.getResolveGroup();
	}
	for (IPrincipal p : directory.getAllPrincipals(principal, includeGroups, resolveGroups)) {
	    items.add(makeItem(p));
	}
	return items;
    }

    private SidSidItem makeItem(IPrincipal principal) {
	SidSidItem item = Factories.sc.windows.createSidSidItem();
	EntityItemStringType trusteeSidType = Factories.sc.core.createEntityItemStringType();
	trusteeSidType.setValue(principal.getSid());
	trusteeSidType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	item.setTrusteeSid(trusteeSidType);

	boolean builtin = false;
	switch(principal.getType()) {
	  case USER:
	    if (directory.isBuiltinUser(principal.getNetbiosName())) {
		builtin = true;
 	    }
	    break;
	  case GROUP:
	    if (directory.isBuiltinGroup(principal.getNetbiosName())) {
		builtin = true;
 	    }
	    break;
	}
	EntityItemStringType trusteeNameType = Factories.sc.core.createEntityItemStringType();
	if (builtin) {
	    trusteeNameType.setValue(principal.getName());
	} else {
	    trusteeNameType.setValue(principal.getNetbiosName());
	}
	trusteeNameType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	item.setTrusteeName(trusteeNameType);

	EntityItemStringType trusteeDomainType = Factories.sc.core.createEntityItemStringType();
	trusteeDomainType.setValue(principal.getDomain());
	trusteeDomainType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	item.setTrusteeDomain(trusteeDomainType);
	return item;
    }
}
