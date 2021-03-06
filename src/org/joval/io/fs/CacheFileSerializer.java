// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io.fs;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serializable;

import org.apache.jdbm.Serializer;

import org.joval.intf.io.IFile;

/**
 * JDBM object serializer for the generic CacheFile abstract class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class CacheFileSerializer implements Serializer<IFile>, Serializable {
    private transient CacheFilesystem fs;

    protected CacheFileSerializer(CacheFilesystem fs) {
	this.fs = fs;
    }

    // Implement Serializer

    public IFile deserialize(DataInput in) throws IOException {
	String path = in.readUTF();
	return new DefaultFile(fs, new FileInfo(in), path);
    }

    public void serialize(DataOutput out, IFile file) throws IOException {
	if (file instanceof CacheFile) {
	    out.writeUTF(file.getPath());
	    CacheFile cfile = (CacheFile)file;
	    cfile.info.write(out);
	} else {
	    throw new NotSerializableException(file.getClass().getName());
	}
    }
}
