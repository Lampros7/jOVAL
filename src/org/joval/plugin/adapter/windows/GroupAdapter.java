// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.windows;

import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.windows.GroupObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.GroupItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.identity.windows.ActiveDirectory;
import org.joval.identity.windows.LocalDirectory;
import org.joval.identity.windows.Group;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;
import org.joval.windows.wmi.WmiException;

/**
 * Evaluates Group OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class GroupAdapter implements IAdapter {
    private IWmiProvider wmi;
    private String hostname;
    private LocalDirectory local = null;
    private ActiveDirectory ad = null;

    public GroupAdapter(String hostname, IWmiProvider wmi) {
	this.wmi = wmi;
	this.hostname = hostname.toUpperCase();
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return GroupObject.class;
    }

    public boolean connect() {
	if (wmi.connect()) {
	    local = new LocalDirectory(hostname, wmi);
	    ad = new ActiveDirectory(wmi);
	    return true;
	}
	return false;
    }

    public void disconnect() {
	wmi.disconnect();
	local = null;
	ad = null;
    }

    public List<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	GroupObject gObj = (GroupObject)rc.getObject();
	String group = (String)gObj.getGroup().getValue();

	try {
	    switch(gObj.getGroup().getOperation()) {
	      case EQUALS:
		if (local.isMember(group)) {
		    items.add(makeGroupItem(local.queryGroup(group)));
		} else if (ad.isMember(group)) {
		    items.add(makeGroupItem(ad.queryGroup(group)));
		} else {
		    MessageType msg = JOVALSystem.factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.WARNING);
		    String s = JOVALSystem.getMessage("ERROR_AD_DOMAIN_UNKNOWN", group);
		    JOVALSystem.getLogger().log(Level.WARNING, s);
		    msg.setValue(s);
		    rc.addMessage(msg);
		}
		break;
    
	      case NOT_EQUAL:
		for (Group g : local.queryAllGroups()) {
		    JAXBElement<GroupItem> item = makeGroupItem(g);
		    if (!group.equals((String)item.getValue().getGroup().getValue())) {
			items.add(item);
		    }
		}
		break;
    
	      case PATTERN_MATCH:
		try {
		    Pattern p = Pattern.compile(group);
		    for (Group g : local.queryAllGroups()) {
			JAXBElement<GroupItem> item = makeGroupItem(g);
			if (p.matcher((String)item.getValue().getGroup().getValue()).find()) {
			    items.add(item);
			}
		    }
		} catch (PatternSyntaxException e) {
		    MessageType msg = JOVALSystem.factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(JOVALSystem.getMessage("ERROR_PATTERN", e.getMessage()));
		    rc.addMessage(msg);
		    JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
		}
		break;
    
	      default:
		throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", gObj.getGroup().getOperation()));
	    }
	} catch (NoSuchElementException e) {
	    // No match.
	} catch (WmiException e) {
	    MessageType msg = JOVALSystem.factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALSystem.getMessage("ERROR_WINWMI_GENERAL", e.getMessage()));
	    rc.addMessage(msg);
	}
	return items;
    }

    // Private

    private JAXBElement<GroupItem> makeGroupItem(Group group) {
	GroupItem item = JOVALSystem.factories.sc.windows.createGroupItem();
	EntityItemStringType groupType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	if (local.isBuiltinGroup(group.getNetbiosName())) {
	    groupType.setValue(group.getName());
	} else {
	    groupType.setValue(group.getNetbiosName());
	}
	item.setGroup(groupType);
	List<String> userNetbiosNames = group.getMemberUserNetbiosNames();
	if (userNetbiosNames.size() == 0) {
	    EntityItemStringType userType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    userType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    item.getUser().add(userType);
	} else {
	    for (String userNetbiosName : userNetbiosNames) {
		EntityItemStringType userType = JOVALSystem.factories.sc.core.createEntityItemStringType();
		if (local.isBuiltinUser(userNetbiosName)) {
		    userType.setValue(local.getName(userNetbiosName));
		} else {
		    userType.setValue(userNetbiosName);
		}
		item.getUser().add(userType);
	    }
	}
	List<String> groupNetbiosNames = group.getMemberGroupNetbiosNames();
	if (groupNetbiosNames.size() == 0) {
	    EntityItemStringType subgroupType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    subgroupType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    item.getSubgroup().add(subgroupType);
	} else {
	    for (String groupNetbiosName : groupNetbiosNames) {
		EntityItemStringType subgroupType = JOVALSystem.factories.sc.core.createEntityItemStringType();
		if (local.isBuiltinGroup(groupNetbiosName)) {
		    subgroupType.setValue(local.getName(groupNetbiosName));
		} else {
		    subgroupType.setValue(groupNetbiosName);
		}
		item.getSubgroup().add(subgroupType);
	    }
	}
	return JOVALSystem.factories.sc.windows.createGroupItem(item);
    }
}