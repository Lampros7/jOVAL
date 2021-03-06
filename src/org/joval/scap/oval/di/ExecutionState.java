// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.di;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;

import org.joval.intf.plugin.IPlugin;
import org.joval.intf.system.IBaseSession;
import org.joval.plugin.PluginFactory;
import org.joval.plugin.PluginConfigurationException;
import org.joval.util.JOVALSystem;

/**
 * The ExecutionState is responsible for parsing the command-line arguments, and providing data about the user's choices
 * to the Main class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ExecutionState {
    static final String DEFAULT_DEFINITIONS		= "definitions.xml";
    static final String DEFAULT_DATA			= "system-characteristics.xml";
    static final String DEFAULT_DIRECTIVES		= "directives.xml";
    static final String DEFAULT_RESULTS_XML		= "results.xml";
    static final String DEFAULT_RESULTS_HTML		= "results.html";
    static final String DEFAULT_VARIABLES		= "external-variables.xml";
    static final String DEFAULT_LOGFILE			= "jovaldi.log";
    static final String DEFAULT_XMLDIR			= "xml";
    static final String DEFAULT_XFORM			= "results_to_html.xsl";
    static final String DEFAULT_DEFS_SCHEMATRON		= "oval-definitions-schematron.xsl";
    static final String DEFAULT_SC_SCHEMATRON		= "oval-system-characteristics-schematron.xsl";
    static final String DEFAULT_RESULTS_SCHEMATRON	= "oval-results-schematron.xsl";
    static final String DEFAULT_PLUGIN			= "default";

    static File CWD = new File(".");
    static File BASE_DIR = new File(".");
    static {
	String s = System.getProperty("jovaldi.baseDir");
	if (s != null) {
	    BASE_DIR = new File(s);
	    System.setProperty("securityDir", new File(BASE_DIR, "security").toString());
	}
    }

    File inputFile;
    File variablesFile;
    File defsFile;
    File inputDefsFile;
    File directivesFile;
    File logFile;
    File xmlDir;
    private File xmlTransform;
    private File schematronDefsXform;
    private File schematronSCXform;
    private File schematronResultsXform;

    List<String> definitionIDs;
    String specifiedChecksum;

    IPlugin plugin;
    Properties pluginConfig = null;

    File dataFile;
    File resultsXML;
    File resultsHTML;

    boolean computeChecksum;
    boolean validateChecksum;
    boolean applyTransform;
    boolean schematronDefs;
    boolean schematronSC;
    boolean schematronResults;
    boolean printLogs;
    boolean printHelp;

    Level logLevel;

    // Internal

    /**
     * Create a new ExecutionState configured with all the default behaviors.
     */
    ExecutionState() {
	//
	// Inputs
	//
	inputFile = null;
	definitionIDs = null;
	specifiedChecksum = null;
	defsFile = new File(CWD, DEFAULT_DEFINITIONS);
	inputDefsFile = null;
	directivesFile = new File(CWD, DEFAULT_DIRECTIVES);
	variablesFile = new File(CWD, DEFAULT_VARIABLES);
	logFile = new File(CWD, DEFAULT_LOGFILE);
	xmlDir = new File(BASE_DIR, DEFAULT_XMLDIR);
	xmlTransform = null;
	schematronDefsXform = null;
	logLevel = Level.INFO;

	//
	// Outputs
	//
	dataFile = new File(CWD, DEFAULT_DATA);
	resultsXML = new File(CWD, DEFAULT_RESULTS_XML);
	resultsHTML = new File(CWD, DEFAULT_RESULTS_HTML);

	//
	// Behaviors
	//
	computeChecksum = false;
	validateChecksum = true;
	applyTransform = true;
	schematronDefs = false;
	printLogs = false;
	printHelp = false;
    }

    File getXMLTransform() {
	if (xmlTransform == null) {
	    return new File(xmlDir, DEFAULT_XFORM);
	} else {
	    return xmlTransform;
	}
    }

    File getDefsSchematron() {
	if (schematronDefsXform == null) {
	    return new File(xmlDir, DEFAULT_DEFS_SCHEMATRON);
	} else {
	    return schematronDefsXform;
	}
    }

    File getSCSchematron() {
	if (schematronSCXform == null) {
	    return new File(xmlDir, DEFAULT_SC_SCHEMATRON);
	} else {
	    return schematronSCXform;
	}
    }

    File getResultsSchematron() {
	if (schematronResultsXform == null) {
	    return new File(xmlDir, DEFAULT_RESULTS_SCHEMATRON);
	} else {
	    return schematronResultsXform;
	}
    }

    String getPath(File f) {
	String path = f.getAbsolutePath();
	String base = CWD.getAbsolutePath();
	if (!base.endsWith(File.separator)) {
	    base = base + File.separator;
	}
	if (path.startsWith(base)) {
	    return path.substring(base.length());
	} else {
	    return path;
	}
    }

    /**
     * Process the command-line arguments.
     *
     * @returns true if successful, false if there is a problem with the arguments.
     */
    boolean processArguments(String[] argv) {
	String pluginName = DEFAULT_PLUGIN;

	for (int i=0; i < argv.length; i++) {
	    if (argv[i].equals("-h")) {
		printHelp = true;
	    } else if (argv[i].equals("-z")) {
		computeChecksum = true;
	    } else if (argv[i].equals("-p")) {
		printLogs = true;
	    } else if (argv[i].equals("-s")) {
		applyTransform = false;
	    } else if (argv[i].equals("-m")) {
		validateChecksum = false;
	    } else if (argv[i].equals("-c")) {
		schematronDefs = true;
		int next = i+1;
		if (next < argv.length && !argv[next].startsWith("-")) {
		    schematronDefsXform = new File(argv[++i]);
		}
	    } else if (argv[i].equals("-j")) {
		schematronSC = true;
		int next = i+1;
		if (next < argv.length && !argv[next].startsWith("-")) {
		    schematronSCXform = new File(argv[++i]);
		}
	    } else if (argv[i].equals("-k")) {
		schematronResults = true;
		int next = i+1;
		if (next < argv.length && !argv[next].startsWith("-")) {
		    schematronResultsXform = new File(argv[++i]);
		}
	    } else if ((argv.length - 1) > i) {
    		//
    		// All the remaining arguments require a subsequent arg
    		//
		if (argv[i].equals("-o")) {
		    defsFile = new File(argv[++i]);
		} else if (argv[i].equals("-l")) {
		    try {
			switch(Integer.parseInt(argv[++i])) {
			  case 1:
			    logLevel = Level.FINEST;
			    break;
			  case 2:
			    logLevel = Level.FINE;
			    break;
			  case 3:
			    logLevel = Level.INFO;
			    break;
			  case 4:
			    logLevel = Level.SEVERE;
			    break;
			  default:
			    Main.print(Main.getMessage("ERROR_INVALID_LOG_LEVEL", argv[i]));
			    return false;
			}
		    } catch (NumberFormatException e) {
			Main.print(Main.getMessage("ERROR_INVALID_LOG_LEVEL", e.getMessage()));
			return false;
		    }
		} else if (argv[i].equals("-y")) {
		    logFile = new File(argv[++i]);
		} else if (argv[i].equals("-v")) {
		    variablesFile = new File(argv[++i]);
		} else if (argv[i].equals("-e")) {
		    definitionIDs = new Vector<String>();
		    StringTokenizer tok = new StringTokenizer(argv[++i], ",");
		    while(tok.hasMoreTokens()) {
			definitionIDs.add(tok.nextToken());
		    }
		} else if (argv[i].equals("-f")) {
		    inputDefsFile = new File(argv[++i]);
		} else if (argv[i].equals("-a")) {
		    File temp = new File(argv[++i]);
		    if (temp.isDirectory()) {
			xmlDir = temp;
		    } else {
			Main.print(Main.getMessage("ERROR_INVALID_SCHEMADIR", temp.toString()));
			return false;
		    }
		} else if (argv[i].equals("-i")) {
		    inputFile = new File(argv[++i]);
		    plugin = null;
		} else if (argv[i].equals("-d")) {
		    dataFile = new File(argv[++i]);
		} else if (argv[i].equals("-g")) {
		    directivesFile = new File(argv[++i]);
		} else if (argv[i].equals("-r")) {
		    resultsXML = new File(argv[++i]);
		} else if (argv[i].equals("-t")) {
		    xmlTransform = new File(argv[++i]);
		} else if (argv[i].equals("-x")) {
		    resultsHTML = new File(argv[++i]);
		} else if (argv[i].equals("-plugin")) {
		    pluginName = argv[++i];
		} else if (argv[i].equals("-config")) {
		    pluginConfig = new Properties();
		    try {
			pluginConfig.load(new FileInputStream(new File(argv[++i])));
		    } catch (IOException e) {
			Main.logException(e);
			Main.print(Main.getMessage("ERROR_PLUGIN_CONFIG", e.getMessage()));
			return false;
		    }
		} else {
		    Main.print(Main.getMessage("WARNING_ARG", argv[i]));
		}
	    } else if (validateChecksum && i == (argv.length - 1)) {
		specifiedChecksum = argv[i];
	    } else {
	 	Main.print(Main.getMessage("WARNING_ARG", argv[i]));
	    }
	}

	if (!printHelp) {
	    Main.configureLogging(logFile, logLevel);
	}
	if (printHelp || inputFile == null) {
	    try {
		plugin = PluginFactory.newInstance(new File(BASE_DIR, "plugin")).createPlugin(pluginName);
	    } catch (IllegalArgumentException e) {
		Main.print(Main.getMessage("ERROR_PLUGIN_DIR_NOT_FOUND", e.getMessage()));
		return false;
	    } catch (NoSuchElementException e) {
		Main.print(Main.getMessage("ERROR_PLUGIN_NOT_FOUND", e.getMessage()));
		return false;
	    } catch (PluginConfigurationException e) {
		Main.print(e.getMessage());
		return false;
	    }
	}

	return validState();
    }

    boolean processPluginArguments() {
	try {
	    if (plugin != null) {
		if (pluginConfig == null) {
		    File config = new File(CWD, IPlugin.DEFAULT_FILE);
		    if (config.exists()) {
			pluginConfig = new Properties();
			pluginConfig.load(new FileInputStream(config));
		    }
		}
		plugin.configure(pluginConfig);
	    }
	    return true;
	} catch (Exception e) {
	    Main.logException(e);
	    Main.print(Main.getMessage("ERROR_PLUGIN_CONFIG", e.getMessage(), logFile));
	}
	return false;
    }

    // Private

    private boolean validState() {
	if (printHelp) {
	    return true;
	}
	if (!defsFile.exists()) {
	    Main.print(Main.getMessage("ERROR_NOSUCHFILE", defsFile.toString()));
	    return false;
	}
	if (!computeChecksum && specifiedChecksum == null && validateChecksum) {
	    Main.print(Main.getMessage("ERROR_NOCHECKSUM"));
	    return false;
	}
	if (inputFile != null) {
	    if (inputFile.exists()) {
		return true;
	    } else {
		Main.print(Main.getMessage("ERROR_INPUTFILE", inputFile));
		return false;
	    }
	} else if (plugin == null) {
	    Main.print(Main.getMessage("ERROR_PLUGIN"));
	    return false;
	}
	return true;
    }

    private String[] getPluginArgs(String s) {
	if (s.startsWith("\"")) {
	    s = s.substring(1);
	}
	if (s.endsWith("\"")) {
	    s = s.substring(0, s.length()-1);
	}
	StringTokenizer tok = new StringTokenizer(s);
	String[] args = new String[tok.countTokens()];
	for (int i=0; tok.hasMoreTokens(); i++) {
	    args[i] = tok.nextToken();
	}
	return args;
    }
}
