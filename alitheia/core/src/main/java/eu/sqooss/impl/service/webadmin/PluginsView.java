/*
 * This file is part of the Alitheia system, developed by the SQO-OSS
 * consortium as part of the IST FP6 SQO-OSS project, number 033331.
 *
 * Copyright 2007 - 2010 - Organization for Free and Open Source Software,  
 *                Athens, Greece.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package eu.sqooss.impl.service.webadmin;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.VelocityContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import eu.sqooss.service.db.DAObject;
import eu.sqooss.service.db.Metric;
import eu.sqooss.service.db.Plugin;
import eu.sqooss.service.db.PluginConfiguration;
import eu.sqooss.service.metricactivator.MetricActivator;
import eu.sqooss.service.pa.PluginAdmin;
import eu.sqooss.service.pa.PluginInfo;
import eu.sqooss.service.pa.PluginInfo.ConfigurationType;
import eu.sqooss.service.util.StringUtils;

public class PluginsView extends AbstractView{

	protected static final String ACT_VAL_UNINSTALL_PLUGIN = "uninstallPlugin";
	protected static final String ACT_VAL_CON_PROP = "confirmProperty";
	protected static final String ACT_VAL_CON_REM_PROP = "removeProperty";
	protected static final String ACT_VAL_SYNC_PLUGIN = "syncPlugin";
	protected static final String REQ_PAR_SHOW_ACTV = "showActivators";
	protected static final String ACT_VAL_INSTALL_PLUGIN = "installPlugin";
	protected static final String REQ_PAR_ACTION = "action";
    protected static final String REQ_PAR_PROP_VALUE = "propertyValue";
	protected static final String REQ_PAR_PROP_DESC = "propertyDescription";
	protected static final String REQ_PAR_PROP_TYPE = "propertyType";
	protected static final String REQ_PAR_PROP_NAME = "propertyName";
	protected static final String REQ_PAR_HASHCODE = "pluginHashcode";
	protected static final String REQ_PAR_SHOW_PROP = "showProperties";
	
	protected static final String ACT_VAL_REQ_ADD_PROP = "createProperty";
	protected static final String ACT_VAL_REQ_UPD_PROP = "updateProperty";

	public PluginsView(BundleContext bundlecontext, VelocityContext vc) {
        super(bundlecontext, vc);
    }

    /**
     * Renders the various plug-in's views.
     * 
     * @param req the servlet's request object
     * 
     * @return The HTML presentation of the generated view.
     */
    public String render(HttpServletRequest req) {
    	// Indentation spacer
    	long in = 6;

        // Proceed only when at least one plug-in is registered
        if (getPluginAdmin().listPlugins().isEmpty()) {
            return normalFieldset(
                    "All plug-ins",
                    null,
                    new StringBuilder("<span>"
                            + "No plug-ins found!&nbsp;"
                            + "<input type=\"button\""
                            + " class=\"install\""
                            + " style=\"width: 100px;\""
                            + " value=\"Refresh\""
                            + " onclick=\"javascript:"
                            + "window.location.reload(true);"
                            + "\" />"
                            + "</span>"),
                    in);
        }
        else {
        	// Stores the accumulated error messages
        	StringBuilder e = new StringBuilder();
        	
        	// Request values
        	String reqValAction        = "";
        	String reqValHashcode      = null;
        	String reqValPropName      = null;
        	String reqValPropDescr     = null;
        	String reqValPropType      = null;
        	String reqValPropValue     = null;
        	boolean reqValShowProp     = false;         // Show plug-in properties
        	boolean reqValShowActv     = false;         // Show plug-in activators
        	// Info object of the selected plug-in
        	PluginInfo selPI           = null;

        	
        	// ===============================================================
            // Parse the servlet's request object
            // ===============================================================
            if (req != null) {
                // DEBUG: Dump the servlet's request parameter

                // Retrieve the selected editor's action (if any)
                reqValAction = req.getParameter(REQ_PAR_ACTION);
                if (reqValAction == null) {
                    reqValAction = "";
                }
                // Retrieve the various display flags
                if ("true".equals(req.getParameter(REQ_PAR_SHOW_PROP))) {
                    reqValShowProp = true;
                }
                if ("true".equals(req.getParameter(REQ_PAR_SHOW_ACTV))) {
                    reqValShowActv = true;
                }
                // Retrieve the selected configuration property's values
                if ((ACT_VAL_CON_PROP.equals(reqValAction))
                        || (ACT_VAL_REQ_UPD_PROP.equals(reqValAction))
                        || (ACT_VAL_CON_REM_PROP.equals(reqValAction))) {
                    // Name, description, type and value
                    reqValPropName  = req.getParameter(REQ_PAR_PROP_NAME);
                    reqValPropDescr = req.getParameter(REQ_PAR_PROP_DESC);
                    reqValPropType  = req.getParameter(REQ_PAR_PROP_TYPE);
                    reqValPropValue = req.getParameter(REQ_PAR_PROP_VALUE);
                }
                // Retrieve the selected plug-in's hash code
                reqValHashcode = req.getParameter(REQ_PAR_HASHCODE);
                // Plug-in based actions
                if (reqValHashcode != null) {
                    // =======================================================
                    // Plug-in install request
                    // =======================================================
                    if (ACT_VAL_INSTALL_PLUGIN.equals(reqValAction)) {
                        if (getPluginAdmin().installPlugin(reqValHashcode) == false) {
                            e.append("Plug-in can not be installed!"
                                    + " Check log for details.");
                        }
                        // Persist the DB changes
                        else {
                            PluginInfo pInfo =
                                getPluginAdmin().getPluginInfo(reqValHashcode);
                            getPluginAdmin().pluginUpdated(getPluginAdmin().getPlugin(pInfo));
                        }
                    }
                    // =======================================================
                    // Plug-in un-install request
                    // =======================================================
                    else if (ACT_VAL_UNINSTALL_PLUGIN.equals(reqValAction)) {
                        if (getPluginAdmin().uninstallPlugin(reqValHashcode) == false) {
                            e.append("Plug-in can not be uninstalled."
                                    + " Check log for details.");
                        } else {
                            e.append("A job was scheduled to remove the plug-in");
                        }
                    } 
                }
                // Retrieve the selected plug-in's info object
                if (reqValHashcode != null) {
                    selPI = getPluginAdmin().getPluginInfo(reqValHashcode);
                }
                // Plug-in info based actions
                if ((selPI != null) && (selPI.installed)) {
                    // =======================================================
                    // Plug-in synchronize (on all projects) request
                    // =======================================================
                    if (ACT_VAL_SYNC_PLUGIN.equals(reqValAction)) {
                        getMetricActivator().syncMetrics(getPluginAdmin().getPlugin(selPI));
                    }
                    // =======================================================
                    // Plug-in's configuration property removal
                    // =======================================================
                    else if (ACT_VAL_CON_REM_PROP.equals(reqValAction)) {
                        if (selPI.hasConfProp(
                                reqValPropName, reqValPropType)) {
                            try {
                                if (selPI.removeConfigEntry(
                                        sobjDB,
                                        reqValPropName,
                                        reqValPropType)) {
                                    // Update the Plug-in Admin's information
                                    getPluginAdmin().pluginUpdated(
                                            getPluginAdmin().getPlugin(selPI));
                                    // Reload the PluginInfo object
                                    selPI = getPluginAdmin().getPluginInfo(
                                            reqValHashcode);
                                }
                                else {
                                    e.append("Property removal"
                                            + " has failed!"
                                            + " Check log for details.");
                                }
                            }
                            catch (Exception ex) {
                                e.append(ex.getMessage());
                            }
                        }
                        else {
                            e.append ("Unknown configuration property!");
                        }
                        // Return to the update view upon error
                        if (e.toString().length() > 0) {
                            reqValAction = ACT_VAL_REQ_UPD_PROP;
                        }
                    }
                    // =======================================================
                    // Plug-in's configuration property creation/update
                    // =======================================================
                    else if (ACT_VAL_CON_PROP.equals(reqValAction)) {
                        // Check for a property update
                        boolean update = selPI.hasConfProp(
                                reqValPropName, reqValPropType);
                        // Update configuration property
                        if (update) {
                            try {
                                if (selPI.updateConfigEntry(
                                        sobjDB,
                                        reqValPropName,
                                        reqValPropValue)) {
                                    // Update the Plug-in Admin's information
                                    getPluginAdmin().pluginUpdated(
                                            getPluginAdmin().getPlugin(selPI));
                                    // Reload the PluginInfo object
                                    selPI =
                                        getPluginAdmin().getPluginInfo(reqValHashcode);
                                }
                                else {
                                    e.append("Property update"
                                            + " has failed!"
                                            + " Check log for details.");
                                }
                            }
                            catch (Exception ex) {
                                e.append(ex.getMessage());
                            }
                        }
                        // Create configuration property
                        else {
                            try {
                                if (selPI.addConfigEntry(
                                        sobjDB,
                                        reqValPropName,
                                        reqValPropDescr,
                                        reqValPropType,
                                        reqValPropValue)) {
                                    // Update the Plug-in Admin's information
                                    getPluginAdmin().pluginUpdated(
                                            getPluginAdmin().getPlugin(selPI));
                                    // Reload the PluginInfo object
                                    selPI =
                                        getPluginAdmin().getPluginInfo(reqValHashcode);
                                }
                                else {
                                    e.append("Property creation"
                                            + " has failed!"
                                            + " Check log for details.");
                                }
                            }
                            catch (Exception ex) {
                                e.append(ex.getMessage());
                            }
                        }
                        // Return to the create/update view upon error
                        if (e.toString().length() > 0) {
                            if (update) reqValAction = ACT_VAL_REQ_UPD_PROP;
                            else reqValAction = ACT_VAL_REQ_ADD_PROP;
                        }
                    }
                }
            }

            // Stores the assembled HTML content
            StringBuilder b = new StringBuilder("\n");
            if (req != null) {
            	if (DEBUG) {
            		b.append(debugRequest(req));
            	}
            }
            // ===============================================================
            // Create the form
            // ===============================================================
            renderForm(b, e, reqValAction, reqValHashcode, reqValPropName,
					reqValPropDescr, reqValPropType, reqValPropValue,
					reqValShowProp, reqValShowActv, selPI, in);
            
            return b.toString();
        }
    }

	protected void renderForm(StringBuilder b, StringBuilder e,
			String reqValAction, String reqValHashcode, String reqValPropName,
			String reqValPropDescr, String reqValPropType,
			String reqValPropValue, boolean reqValShowProp,
			boolean reqValShowActv, PluginInfo selPI, long in) {
		b.append(sp(in++) + "<form id=\"metrics\""
		        + " name=\"metrics\""
		        + " method=\"post\""
		        + " action=\"/index\">\n");

		// ===============================================================
		// Display the accumulated error messages (if any)
		// ===============================================================
		b.append(errorFieldset(e, in));

		// ===============================================================
		// "Create/update configuration property" editor
		// ===============================================================
		if ((selPI != null) && (selPI.installed)
		        && ((ACT_VAL_REQ_ADD_PROP.equals(reqValAction))
		                || (ACT_VAL_REQ_UPD_PROP.equals(reqValAction)))) {
		    // Input field values are stored here
		    String value = null;
		    // Create the field-set
		    b.append(sp(in) + "<fieldset>\n");
		    // Check for a property update request
		    boolean update = selPI.hasConfProp(
		            reqValPropName, reqValPropType);
		    b.append(sp(++in) + "<legend>"
		            + ((update)
		                    ? "Update property of "
		                    : "Create property for ")
		            + selPI.getPluginName()
		            + "</legend>\n");
		    b.append(sp(in) + "<table class=\"borderless\">");
		    // Property's name
		    value = ((reqValPropName != null) ? reqValPropName : "");
		    b.append(sp(in) + "<tr>\n"
		            + sp(++in)
		            + "<td class=\"borderless\" style=\"width:100px;\">"
		            + "<b>Name</b>"
		            + "</td>\n"
		            + sp(in)
		            + "<td class=\"borderless\">"
		            + ((update) ? value
		                    : "<input type=\"text\""
		                        + " class=\"form\""
		                        + " id=\"" + REQ_PAR_PROP_NAME + "\""
		                        + " name=\"" + REQ_PAR_PROP_NAME + "\""
		                        + " value=\"" + value + "\" />")
		                        + "</td>\n"
		                        + sp(--in) + "</tr>\n");
		    // Property's description
		    value = ((reqValPropDescr != null) ? reqValPropDescr : "");
		    b.append(sp(in) + "<tr>\n"
		            + sp(++in)
		            + "<td class=\"borderless\" style=\"width:100px;\">"
		            + "<b>Description</b>"
		            + "</td>\n"
		            + sp(in)
		            + "<td class=\"borderless\">"
		            + ((update) ? value
		                    : "<input type=\"text\""
		                        + " class=\"form\""
		                        + " id=\"" + REQ_PAR_PROP_DESC + "\""
		                        + " name=\"" + REQ_PAR_PROP_DESC + "\""
		                        + " value=\"" + value + "\" />")
		                        + "</td>\n"
		                        + sp(--in) + "</tr>\n");
		    // Property's type
		    value = ((reqValPropType != null) ? reqValPropType : "");
		    b.append(sp(in) + "<tr>\n"
		            + sp(++in)
		            + "<td class=\"borderless\" style=\"width:100px;\">"
		            + "<b>Type</b>"
		            + "</td>\n"
		            + sp(in)
		            + "<td class=\"borderless\">\n"
		            + sp(++in));
		    if (update) {
		        b.append(value);
		    }
		    else {
		        b.append("<select class=\"form\""
		                + " id=\"" + REQ_PAR_PROP_TYPE + "\""
		                + " name=\"" + REQ_PAR_PROP_TYPE + "\">\n");
		        for (ConfigurationType type : ConfigurationType.values()) {
		            boolean selected = type.toString().equals(value);
		            b.append(sp(in) + "<option"
		                    + " value=\"" + type.toString() + "\""
		                    + ((selected) ? " selected" : "")
		                    + ">"
		                    + type.toString()
		                    + "</option>\n");
		        }
		        b.append(sp(in) + "</select>\n");
		    }
		    b.append(sp(--in)
		            + "</td>\n"
		            + sp(--in)
		            + "</tr>\n");
		    // Property's value
		    value = ((reqValPropValue != null) ? reqValPropValue : "");
		    b.append(sp(in) + "<tr>\n"
		            + sp(++in)
		            + "<td class=\"borderless\" style=\"width:100px;\">"
		            + "<b>Value</b>"
		            + "</td>\n"
		            + sp(in)
		            + "<td class=\"borderless\">"
		            + "<input type=\"text\""
		            + " class=\"form\""
		            + " id=\"" + REQ_PAR_PROP_VALUE + "\""
		            + " name=\"" + REQ_PAR_PROP_VALUE + "\""
		            + " value=\"" + value +"\" />"
		            + "</td>\n"
		            + sp(--in)
		            + "</tr>\n");
		    // Command tool-bar
		    value = ((update) ? "Update" : "Create");
		    b.append(sp(in) + "<tr>\n"
		            + sp(++in)
		            + "<td colspan=\"2\" class=\"borderless\">"
		            + "<input type=\"button\""
		            + " class=\"install\""
		            + " style=\"width: 100px;\""
		            + " value=\"" + value + "\""
		            + " onclick=\"javascript:"
		            + "document.getElementById('"
		            + REQ_PAR_ACTION + "').value='"
		            + ACT_VAL_CON_PROP + "';"
		            + "document.metrics.submit();\" />"
		            + "&nbsp;");
		    if (update) {
		        b.append(sp(in) + "<input type=\"button\""
		                + " class=\"install\""
		                + " style=\"width: 100px;\""
		                + " value=\"Remove\""
		                + " onclick=\"javascript:"
		                + "document.getElementById('"
		                + REQ_PAR_ACTION + "').value='"
		                + ACT_VAL_CON_REM_PROP + "';"
		                + "document.metrics.submit();\" />"
		                + "&nbsp;");
		    }
		    b.append(sp(in) + "<input type=\"button\""
		            + " class=\"install\""
		            + " style=\"width: 100px;\""
		            + " value=\"Cancel\""
		            + " onclick=\"javascript:"
		            + "document.metrics.submit();\" />"
		            + "</td>\n"
		            + sp(--in)
		            + "</tr>\n");
		    b.append(sp(--in) + "</table>");
		    b.append(sp(--in) + "</fieldset>\n");
		}
		// ===============================================================
		// Plug-in editor
		// ===============================================================
		else if (selPI != null) {
		    // Create the plug-in field-set
		    b.append(sp(in) + "<fieldset>\n");
		    b.append(sp(++in) + "<legend>"
		            + selPI.getPluginName()
		            + "</legend>\n");
		    //------------------------------------------------------------
		    // Create the plug-in info table
		    //------------------------------------------------------------
		    b.append(sp(in) + "<table>\n");
		    b.append(sp(++in) + "<thead>\n");
		    b.append(sp(++in) + "<tr class=\"head\">\n");
		    b.append(sp(++in) + "<td class=\"head\""
		            + " style=\"width: 80px;\">"
		            + "Status</td>\n");
		    b.append(sp(in) + "<td class=\"head\""
		            + " style=\"width: 30%;\">"
		            + "Name</td>\n");
		    b.append(sp(in) + "<td class=\"head\""
		            + " style=\"width: 40%;\">"
		            + "Class</td>\n");
		    b.append(sp(in) + "<td class=\"head\">Version</td>\n");
		    b.append(sp(--in) + "</tr>\n");
		    b.append(sp(--in) + "</thead>\n");
		    // Display the plug-in's info
		    b.append(sp(in) + "<tbody>\n");
		    b.append(sp(in++) + "<tr>\n");
		    // Plug-in state
		    b.append(sp(++in) + "<td>"
		            + ((selPI.installed) 
		                    ? "Installed" : "Registered")
		                    + "</td>\n");
		    // Plug-in name
		    b.append(sp(in) + "<td>"
		            + selPI.getPluginName() + "</td>\n");
		    //  Plug-in class
		    b.append(sp(in) + "<td>"
		            + StringUtils.join((String[]) (
		                    selPI.getServiceRef().getProperty(
		                            Constants.OBJECTCLASS)),",")
		                            + "</td>\n");
		    // Plug-in version
		    b.append(sp(in) + "<td>"
		            + selPI.getPluginVersion() + "</td>\n");
		    b.append(sp(--in) + "</tr>\n");
		    // Plug-in tool-bar
		    b.append(sp(in++) + "<tr>\n");
		    b.append(sp(in++) + "<td colspan=\"4\">\n");
		    b.append(sp(in) + "<input type=\"button\""
		            + " class=\"install\""
		            + " style=\"width: 100px;\""
		            + " value=\"Plug-ins list\""
		            + " onclick=\"javascript:"
		            + "document.getElementById('"
		            + REQ_PAR_HASHCODE + "').value='';"
		            + "document.metrics.submit();\""
		            + " />\n");
		    if (selPI.installed) {
		        b.append(sp(in) + "<input type=\"button\""
		                + " class=\"install\""
		                + " style=\"width: 100px;\""
		                + " value=\"Uninstall\""
		                + " onclick=\"javascript:"
		                + "document.getElementById('"
		                + REQ_PAR_ACTION + "').value='"
		                + ACT_VAL_UNINSTALL_PLUGIN + "';"
		                + "document.getElementById('"
		                + REQ_PAR_HASHCODE +"').value='"
		                + selPI.getHashcode() + "';"
		                + "document.metrics.submit();\""
		                + " />\n");
		        b.append(sp(in) + "<input type=\"button\""
		                + " class=\"install\""
		                + " style=\"width: 100px;\""
		                + " value=\"Synchronise\""
		                + " onclick=\"javascript:"
		                + "document.getElementById('"
		                + REQ_PAR_ACTION + "').value='"
		                + ACT_VAL_SYNC_PLUGIN + "';"
		                + "document.getElementById('"
		                + REQ_PAR_HASHCODE +"').value='"
		                + selPI.getHashcode() + "';"
		                + "document.metrics.submit();\""
		                + " />\n");
		    }
		    else {
		        b.append(sp(in) + "<input type=\"button\""
		                + " class=\"install\""
		                + " style=\"width: 100px;\""
		                + " value=\"Install\""
		                + " onclick=\"javascript:"
		                + "document.getElementById('"
		                + REQ_PAR_ACTION + "').value='"
		                + ACT_VAL_INSTALL_PLUGIN + "';"
		                + "document.getElementById('"
		                + REQ_PAR_HASHCODE +"').value='"
		                + selPI.getHashcode() + "';"
		                + "document.metrics.submit();\""
		                + " />\n");
		    }
		    b.append(sp(--in) + "</td>\n");
		    b.append(sp(--in) + "</tr>\n");
		    // Close the plug-in table
		    b.append(sp(--in) + "</tbody>\n");
		    b.append(sp(--in) + "</table>\n");

		    //------------------------------------------------------------
		    // Registered metrics, activators and configuration 
		    //------------------------------------------------------------
		    if (selPI.installed) {
		        //--------------------------------------------------------
		        // Create the metrics field-set
		        //--------------------------------------------------------
		        b.append(sp(++in) + "<fieldset>\n");
		        b.append(sp(++in) + "<legend>"
		                + "Supported metrics"
		                + "</legend>\n");
		        // Create the metrics table
		        b.append(sp(in) + "<table>\n");
		        b.append(sp(++in) + "<thead>\n");
		        b.append(sp(++in) + "<tr class=\"head\">\n");
		        b.append(sp(++in) + "<td class=\"head\""
		                + " style=\"width: 10%;\">"
		                + "Id</td>\n");
		        b.append(sp(in) + "<td class=\"head\""
		                + " style=\"width: 25%;\">"
		                + "Name</td>\n");
		        b.append(sp(in) + "<td class=\"head\""
		                + " style=\"width: 25%;\">"
		                + "Type</td>\n");
		        b.append(sp(in) + "<td class=\"head\""
		                + " style=\"width: 40%;\">"
		                + "Description</td>\n");
		        b.append(sp(--in) + "</tr>\n");
		        b.append(sp(--in) + "</thead>\n");
		        // Display the list of supported metrics
		        b.append(sp(in++) + "<tbody>\n");
		        // Get the list of supported metrics
		        List<Metric> metrics =
		            getPluginAdmin().getPlugin(selPI).getAllSupportedMetrics();
		        if ((metrics == null) || (metrics.isEmpty())) {
		            b.append(sp(in++) + "<tr>");
		            b.append(sp(in) + "<td colspan=\"4\" class=\"noattr\">"
		                    + "This plug-in does not support metrics."
		                    + "</td>\n");
		            b.append(sp(--in)+ "</tr>\n");
		        }
		        else {
		            for (Metric metric: metrics) {
		                b.append(sp(in++) + "<tr>\n");
		                b.append(sp(in) + "<td>"
		                        + metric.getId() + "</td>\n");
		                b.append(sp(in) + "<td>"
		                        + metric.getMnemonic() + "</td>\n");
		                b.append(sp(in) + "<td>"
		                        + metric.getMetricType().getType()
		                        + "</td>\n");
		                b.append(sp(in) + "<td>"
		                        + metric.getDescription() + "</td>\n");
		                b.append(sp(--in)+ "</tr>\n");
		            }
		        }
		        // Close the metrics table
		        b.append(sp(--in) + "</tbody>\n");
		        // Close the metrics table
		        b.append(sp(--in) + "</table>\n");
		        // Close the metric field-set
		        b.append(sp(--in) + "</fieldset>\n");
		        //--------------------------------------------------------
		        // Create the properties field-set
		        //--------------------------------------------------------
		        b.append(sp(++in) + "<fieldset>\n");
		        b.append(sp(++in) + "<legend>"
		                + "Configuration properties"
		                + "</legend>\n");
		        // Create the properties table
		        b.append(sp(in) + "<table>\n");
		        b.append(sp(++in) + "<thead>\n");
		        b.append(sp(++in) + "<tr class=\"head\">\n");
		        b.append(sp(++in) + "<td class=\"head\""
		                + " style=\"width: 30%;\">"
		                + "Name</td>\n");
		        b.append(sp(in) + "<td class=\"head\""
		                + " style=\"width: 20%;\">"
		                + "Type</td>\n");
		        b.append(sp(in) + "<td class=\"head\""
		                + " style=\"width: 50%;\">"
		                + "Value</td>\n");
		        b.append(sp(--in) + "</tr>\n");
		        b.append(sp(--in) + "</thead>\n");
		        // Display the set of configuration properties
		        b.append(sp(in++) + "<tbody>\n");
		        // Get the plug-in's configuration set
		        Set<PluginConfiguration> config = getPluginByHashcode(selPI.getHashcode()).getConfigurations();
		        if ((config == null) || (config.isEmpty())) {
		            b.append(sp(in++) + "<tr>");
		            b.append(sp(in) + "<td colspan=\"3\" class=\"noattr\">"
		                    + "This plug-in has no configuration properties."
		                    + "</td>\n");	
		            b.append(sp(--in)+ "</tr>\n");
		        }
		        else {
		            for (PluginConfiguration param : config) {
		                b.append(sp(in++) + "<tr class=\"edit\""
		                        + " onclick=\"javascript:"
		                        + "document.getElementById('"
		                        + REQ_PAR_ACTION + "').value='"
		                        + ACT_VAL_REQ_UPD_PROP + "';"
		                        + "document.getElementById('"
		                        + REQ_PAR_PROP_NAME + "').value='"
		                        + param.getName() + "';"
		                        + "document.getElementById('"
		                        + REQ_PAR_PROP_TYPE + "').value='"
		                        + param.getType() + "';"
		                        + "document.getElementById('"
		                        + REQ_PAR_PROP_DESC + "').value='"
		                        + param.getMsg() + "';"
		                        + "document.getElementById('"
		                        + REQ_PAR_PROP_VALUE + "').value='"
		                        + param.getValue() + "';"
		                        + "document.metrics.submit();\""
		                        + ">\n");
		                // Property's name and description
		                String description = param.getMsg();
		                if (param.getMsg() == null)
		                    description = "No description available.";
		                b.append(sp(in) + "<td class=\"trans\""
		                        + " title=\"" + description + "\">"
		                        + "<img src=\"/edit.png\" alt=\"[Edit]\"/>"
		                        + "&nbsp;" + param.getName()
		                        + "</td>\n");
		                // Property's type
		                b.append(sp(in) + "<td class=\"trans\">"
		                        + param.getType()
		                        + "</td>\n");
		                // Property's value
		                b.append(sp(in) + "<td class=\"trans\">"
		                        + param.getValue()
		                        + "</td>\n");
		                b.append(sp(--in)+ "</tr>\n");
		            }
		        }
		        // Command tool-bar
		        b.append(sp(in) + "<tr>\n");
		        b.append(sp(++in) + "<td colspan=\"3\">\n");
		        b.append(sp(++in) + "<input type=\"button\""
		                + " class=\"install\""
		                + " style=\"width: 100px;\""
		                + " value=\"Add property\""
		                + " onclick=\"javascript:"
		                + "document.getElementById('"
		                + REQ_PAR_ACTION + "').value='"
		                + ACT_VAL_REQ_ADD_PROP + "';"
		                + "document.metrics.submit();\""
		                + " />\n");
		        b.append(sp(--in) + "</td>\n");
		        b.append(sp(--in) + "</tr>\n");
		        // Close the properties table
		        b.append(sp(--in) + "</tbody>\n");
		        // Close the properties table
		        b.append(sp(--in) + "</table>\n");
		        // Close the properties field-set
		        b.append(sp(--in) + "</fieldset>\n");
		    }

		    // Close the plug-in field-set
		    b.append(sp(--in) + "</fieldset>\n");
		}
		// ===============================================================
		// Plug-ins list
		// ===============================================================
		else {
		    // Create the field-set
		    b.append(sp(in) + "<fieldset>\n");
		    b.append(sp(++in) + "<legend>All plug-ins</legend>\n");
		    // Retrieve information for all registered metric plug-ins
		    Collection<PluginInfo> l = getPluginAdmin().listPlugins();
		    //------------------------------------------------------------
		    // Create the header row
		    //------------------------------------------------------------
		    b.append(sp(in) + "<table>\n");
		    b.append(sp(++in) + "<thead>\n");
		    b.append(sp(++in) + "<tr class=\"head\">\n");
		    b.append(sp(++in) + "<td class=\"head\""
		            + " style=\"width: 80px;\">"
		            + "Status</td>\n");
		    b.append(sp(in) + "<td class=\"head\""
		            + " style=\"width: 30%;\">"
		            + "Name</td>\n");
		    b.append(sp(in) + "<td class=\"head\""
		            + " style=\"width: 40%;\">"
		            + "Class</td>\n");
		    b.append(sp(in) + "<td class=\"head\">Version</td>\n");
		    b.append(sp(--in) + "</tr>\n");
		    b.append(sp(--in) + "</thead>\n");
		    //------------------------------------------------------------
		    // Create the content row
		    //------------------------------------------------------------
		    b.append(sp(in++) + "<tbody>\n");
		    //------------------------------------------------------------
		    // Display not-installed plug-ins first
		    //------------------------------------------------------------
		    for(PluginInfo i : l) {
		        if (i.installed == false) {
		            b.append(sp(in) + "<tr class=\"edit\""
		                    + " onclick=\"javascript:"
		                    + "document.getElementById('"
		                    + REQ_PAR_HASHCODE + "').value='"
		                    + i.getHashcode() + "';"
		                    + "document.metrics.submit();\""
		                    + ">\n");
		            // Plug-in state
		            b.append(sp(++in) + "<td class=\"trans\">"
		                    + "<img src=\"/edit.png\" alt=\"[Edit]\"/>"
		                    + "&nbsp;Registered</td>\n");
		            // Plug-in name
		            b.append(sp(in) + "<td class=\"trans\">"
		                + i.getPluginName()
		                + "</td>\n");
		            // Plug-in class
		            b.append(sp(in) + "<td class=\"trans\">"
		                    + StringUtils.join((String[]) (
		                            i.getServiceRef().getProperty(
		                                    Constants.OBJECTCLASS)),",")
		                                    + "</td>\n");
		            // Plug-in version
		            b.append(sp(in) + "<td class=\"trans\">"
		                    + i.getPluginVersion() + "</td>\n");
		            b.append(sp(--in) + "</tr>\n");
		            // Extended plug-in information
		            b.append(renderPluginAttributes(
		                    i, reqValShowProp, reqValShowActv, in));
		        }
		    }
		    //------------------------------------------------------------
		    // Installed plug-ins
		    //------------------------------------------------------------
		    for(PluginInfo i : l) {
		        if (i.installed) {
		            b.append(sp(in) + "<tr class=\"edit\""
		                    + " onclick=\"javascript:"
		                    + "document.getElementById('"
		                    + REQ_PAR_HASHCODE + "').value='"
		                    + i.getHashcode() + "';"
		                    + "document.metrics.submit();\""
		                    + ">\n");
		            // Plug-in state
		            b.append(sp(++in) + "<td class=\"trans\">"
		                    + "<img src=\"/edit.png\" alt=\"[Edit]\"/>"
		                    + "&nbsp;Installed</td>\n");
		            // Plug-in name
		            b.append(sp(in) + "<td class=\"trans\">"
		                    + i.getPluginName()
		                    + "</td>\n");
		            // Plug-in class
		            b.append(sp(in) + "<td class=\"trans\">"
		                    + StringUtils.join((String[]) (
		                            i.getServiceRef().getProperty(
		                                    Constants.OBJECTCLASS)),",")
		                                    + "</td>\n");
		            // Plug-in version
		            b.append(sp(in) + "<td class=\"trans\">"
		                    + i.getPluginVersion() + "</td>\n");
		            b.append(sp(--in) + "</tr>\n");	
		            // Extended plug-in information
		            b.append(renderPluginAttributes(
		                    i, reqValShowProp, reqValShowActv, in));
		        }
		    }
		    //------------------------------------------------------------
		    // Close the table
		    //------------------------------------------------------------
		    b.append(sp(--in) + "</tbody>\n");
		    b.append(sp(--in) + "</table>\n");
		    //------------------------------------------------------------
		    // Display flags
		    //------------------------------------------------------------
		    b.append(sp(in) + "<span>\n");
		    b.append(sp(++in) + "<input"
		            + " type=\"checkbox\""
		            + ((reqValShowProp) ? "checked" : "")
		            + " onclick=\"javascript:"
		            + "document.getElementById('"
		            + REQ_PAR_SHOW_PROP + "').value = this.checked;"
		            + "document.getElementById('"
		            + REQ_PAR_HASHCODE + "').value='';"
		            + "document.metrics.submit();\""
		            + " />Display properties\n");
		    b.append(sp(++in) + "<input"
		            + " type=\"checkbox\""
		            + ((reqValShowActv) ? "checked" : "")
		            + " onclick=\"javascript:"
		            + "document.getElementById('"
		            + REQ_PAR_SHOW_ACTV + "').value = this.checked;"
		            + "document.getElementById('"
		            + REQ_PAR_HASHCODE + "').value='';"
		            + "document.metrics.submit();\""
		            + " />Display activators\n");
		    b.append(sp(--in) + "</span>\n");
		    // Close the field-set
		    b.append(sp(--in) + "</fieldset>\n");
		}

		// ===============================================================
		// INPUT FIELDS
		// ===============================================================
		// "Action type" input field
		b.append(sp(in) + "<input type=\"hidden\""
		        + " id=\"" + REQ_PAR_ACTION + "\""
		        + " name=\"" + REQ_PAR_ACTION + "\""
		        + " value=\"\" />\n");
		// "Selected plug-in's hash code" input field
		b.append(sp(in) + "<input type=\"hidden\""
		        + " id=\"" + REQ_PAR_HASHCODE + "\""
		        + " name=\"" + REQ_PAR_HASHCODE + "\""
		        + " value=\""
		        + ((reqValHashcode != null) ? reqValHashcode : "")
		        + "\" />\n");
		// "Configuration attribute's name" input field
		b.append(sp(in) + "<input type=\"hidden\""
		        + " id=\"" + REQ_PAR_PROP_NAME + "\""
		        + " name=\"" + REQ_PAR_PROP_NAME + "\""
		        + " value=\""
		        + ((reqValPropName != null) ? reqValPropName : "")
		        + "\" />\n");
		// "Configuration attribute's description" input field
		b.append(sp(in) + "<input type=\"hidden\""
		        + " id=\"" + REQ_PAR_PROP_DESC + "\""
		        + " name=\"" + REQ_PAR_PROP_DESC + "\""
		        + " value=\""
		        + ((reqValPropDescr != null) ? reqValPropDescr : "")
		        + "\" />\n");
		// "Configuration attribute's type" input field
		b.append(sp(in) + "<input type=\"hidden\""
		        + " id=\"" + REQ_PAR_PROP_TYPE + "\""
		        + " name=\"" + REQ_PAR_PROP_TYPE + "\""
		        + " value=\""
		        + ((reqValPropType != null) ? reqValPropType : "")
		        + "\" />\n");
		// "Configuration attribute's value" input field
		b.append(sp(in) + "<input type=\"hidden\""
		        + " id=\"" + REQ_PAR_PROP_VALUE + "\""
		        + " name=\"" + REQ_PAR_PROP_VALUE + "\""
		        + " value=\""
		        + ((reqValPropValue != null) ? reqValPropValue : "")
		        + "\" />\n");
		// "Show configuration properties" input field
		b.append(sp(in) + "<input type=\"hidden\""
		        + " id=\"" + REQ_PAR_SHOW_PROP + "\""
		        + " name=\"" + REQ_PAR_SHOW_PROP + "\""
		        + " value=\""
		        + reqValShowProp
		        + "\" />\n");
		// "Show activators" input field
		b.append(sp(in) + "<input type=\"hidden\""
		        + " id=\"" + REQ_PAR_SHOW_ACTV + "\""
		        + " name=\"" + REQ_PAR_SHOW_ACTV + "\""
		        + " value=\""
		        + reqValShowActv
		        + "\" />\n");

		// ===============================================================
		// Close the form
		// ===============================================================
		b.append(sp(--in) + "</form>\n");
	}

	protected MetricActivator getMetricActivator() {
		return compMA;
	}

	protected Plugin getPluginByHashcode(String hashcode) {
		return Plugin.getPluginByHashcode(hashcode);
	}

	protected PluginAdmin getPluginAdmin() {
		return sobjPA;
	}

    /**
     * Creates a set of table rows populated with the plug-in properties and
     * activators, as found in the given <code>PluginInfo</code> object
     * 
     * @param pluginInfo the plug-in's <code>PluginInfo</code> object
     * @param showProperties display flag
     * @param showActivators display flag
     * @param in indentation value for the generated HTML content
     * 
     * @return The table as HTML presentation.
     */
    protected static String renderPluginAttributes(
            PluginInfo pluginInfo,
            boolean showProperties,
            boolean showActivators,
            long in) {
        // Stores the assembled HTML content
        StringBuilder b = new StringBuilder();
        // List the metric plug-in's configuration properties
        if (showProperties) {
            Set<PluginConfiguration> l =
                pluginInfo.getConfiguration();
            // Skip if this plug-ins has no configuration
            if ((l != null) && (l.isEmpty() == false)) {
                for (PluginConfiguration property : l) {
                    b.append(sp(in++) + "<tr>");
                    b.append(sp(in) + "<td>&nbsp;</td>\n");
                    b.append(sp(in) + "<td colspan=\"3\" class=\"attr\">"
                            + "<b>Property:</b> " + property.getName()
                            + "&nbsp;<b>Type:</b> " + property.getType()
                            + "&nbsp;<b>Value:</b> " + property.getValue()
                            + "</td>\n");
                    b.append(sp(--in)+ "</tr>\n");
                }
            }
        }
        // List the metric plug-in's activator types
        if (showActivators) {
            Set<Class<? extends DAObject>> activators =
                pluginInfo.getActivationTypes();
            // Skip if this plug-ins has no activators
            if (activators != null) {
                for (Class<? extends DAObject> activator : activators) {
                    b.append("<tr>");
                    b.append("<td>&nbsp;</td>\n");
                    b.append("<td colspan=\"3\" class=\"attr\">"
                            + "<b>Activator:</b> "
                            + activator.getName()
                            + "</td>");
                    b.append("</tr>\n");
                }
            }
        }
        return b.toString();
    }
}

//vi: ai nosi sw=4 ts=4 expandtab
