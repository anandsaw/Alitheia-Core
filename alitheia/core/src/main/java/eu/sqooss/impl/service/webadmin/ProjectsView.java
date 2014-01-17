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

import static eu.sqooss.impl.service.webadmin.HTMLFormBuilder.POST;
import static eu.sqooss.impl.service.webadmin.HTMLFormBuilder.form;
import static eu.sqooss.impl.service.webadmin.HTMLInputBuilder.BUTTON;
import static eu.sqooss.impl.service.webadmin.HTMLInputBuilder.input;
import static eu.sqooss.impl.service.webadmin.HTMLNodeBuilder.node;
import static eu.sqooss.impl.service.webadmin.HTMLTableBuilder.table;
import static eu.sqooss.impl.service.webadmin.HTMLTableBuilder.tableColumn;
import static eu.sqooss.impl.service.webadmin.HTMLTableBuilder.tableRow;
import static eu.sqooss.impl.service.webadmin.HTMLTextBuilder.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.VelocityContext;
import org.osgi.framework.BundleContext;

import eu.sqooss.core.AlitheiaCore;
import eu.sqooss.impl.service.webadmin.HTMLTableBuilder.HTMLTableRowBuilder;
import eu.sqooss.service.abstractmetric.AlitheiaPlugin;
import eu.sqooss.service.admin.AdminAction;
import eu.sqooss.service.admin.AdminService;
import eu.sqooss.service.admin.actions.AddProject;
import eu.sqooss.service.admin.actions.UpdateProject;
import eu.sqooss.service.db.Bug;
import eu.sqooss.service.db.ClusterNode;
import eu.sqooss.service.db.MailMessage;
import eu.sqooss.service.db.ProjectVersion;
import eu.sqooss.service.db.StoredProject;
import eu.sqooss.service.logging.Logger;
import eu.sqooss.service.metricactivator.MetricActivator;
import eu.sqooss.service.pa.PluginAdmin;
import eu.sqooss.service.pa.PluginInfo;
import eu.sqooss.service.scheduler.Scheduler;
import eu.sqooss.service.scheduler.SchedulerException;
import eu.sqooss.service.updater.Updater;
import eu.sqooss.service.updater.UpdaterService.UpdaterStage;

public class ProjectsView extends AbstractView {
    // Script for submitting this page
    protected static String SUBMIT = "document.projects.submit();";

    // Action parameter's values
    protected static String ACT_REQ_ADD_PROJECT   = "reqAddProject";
    protected static String ACT_CON_ADD_PROJECT   = "conAddProject";
    protected static String ACT_REQ_REM_PROJECT   = "reqRemProject";
    protected static String ACT_CON_REM_PROJECT   = "conRemProject";
    protected static String ACT_REQ_SHOW_PROJECT  = "conShowProject";
    protected static String ACT_CON_UPD_ALL       = "conUpdateAll";
    protected static String ACT_CON_UPD           = "conUpdate";
    protected static String ACT_CON_UPD_ALL_NODE  = "conUpdateAllOnNode";

    // Servlet parameters
    protected static String REQ_PAR_ACTION        = "reqAction";
    protected static String REQ_PAR_PROJECT_ID    = "projectId";
    protected static String REQ_PAR_PRJ_NAME      = "projectName";
    protected static String REQ_PAR_PRJ_WEB       = "projectHomepage";
    protected static String REQ_PAR_PRJ_CONT      = "projectContact";
    protected static String REQ_PAR_PRJ_BUG       = "projectBL";
    protected static String REQ_PAR_PRJ_MAIL      = "projectML";
    protected static String REQ_PAR_PRJ_CODE      = "projectSCM";
    protected static String REQ_PAR_SYNC_PLUGIN   = "reqParSyncPlugin";
    protected static String REQ_PAR_UPD           = "reqUpd";
    
    /**
     * Instantiates a new projects view.
     *
     * @param bundlecontext the <code>BundleContext</code> object
     * @param vc the <code>VelocityContext</code> object
     */
    public ProjectsView(BundleContext bundlecontext, VelocityContext vc) {
        super(bundlecontext, vc);
    }

    /**
     * Renders the various project's views.
     *
     * @param req the servlet's request object
     *
     * @return The HTML presentation of the generated view.
     */
    public String render(HttpServletRequest req) {
        // Stores the assembled HTML content
        StringBuilder b = new StringBuilder("\n");
        // Stores the accumulated error messages
        StringBuilder e = new StringBuilder();
        // Indentation spacer
        int in = 6;

        // Initialize the resource bundles with the request's locale
        initializeResources(req);

        // Request values
        String reqValAction        = "";
        Long   reqValProjectId     = null;

        // Selected project
        StoredProject selProject = null;

        // ===============================================================
        // Parse the servlet's request object
        // ===============================================================
        if (req != null) {
            // DEBUG: Dump the servlet's request parameter
            if (DEBUG) {
                b.append(debugRequest(req));
            }

            // Retrieve the selected editor's action (if any)
            reqValAction = (req.getParameter(REQ_PAR_ACTION) != null) ? req.getParameter(REQ_PAR_ACTION) : "";
            
            // Retrieve the selected project's DAO (if any)
            reqValProjectId = fromString(req.getParameter(REQ_PAR_PROJECT_ID));
            if (reqValProjectId != null) {
                selProject = getProjectById(reqValProjectId);
            }
            
            if (ACT_CON_ADD_PROJECT.equals(reqValAction)) {
            	selProject = addProject(e, req, in);
            } else if (ACT_CON_REM_PROJECT.equals(reqValAction)) {
            	selProject = removeProject(e, selProject, in);
            } else if (ACT_CON_UPD.equals(reqValAction)) {
            	triggerUpdate(e, selProject, in, req.getParameter(REQ_PAR_UPD));
            } else if (ACT_CON_UPD_ALL.equals(reqValAction)) {
            	triggerAllUpdate(e, selProject, in);
            } else if (ACT_CON_UPD_ALL_NODE.equals(reqValAction)) {
            	triggerAllUpdateNode(e, selProject, in);
            } else {
            	// Retrieve the selected plug-in's hash-code
        		String reqValSyncPlugin = req.getParameter(REQ_PAR_SYNC_PLUGIN);
        		syncPlugin(e, selProject, reqValSyncPlugin);
            }
        }
        createForm(b, e, selProject, reqValAction, in);
        return b.toString();
    }

	protected StoredProject getProjectById(long reqValProjectId) {
		return sobjDB.findObjectById(
		        StoredProject.class, reqValProjectId);
	}

	protected void initializeResources(HttpServletRequest req) {
		initResources(req.getLocale());
	}
  
    protected StoredProject addProject(StringBuilder e, HttpServletRequest r, int indent) {
        AdminService as = getAdminService();
    	AdminAction aa = as.create(AddProject.MNEMONIC);
    	aa.addArg("scm", r.getParameter(REQ_PAR_PRJ_CODE));
    	aa.addArg("name", r.getParameter(REQ_PAR_PRJ_NAME));
    	aa.addArg("bts", r.getParameter(REQ_PAR_PRJ_BUG));
    	aa.addArg("mail", r.getParameter(REQ_PAR_PRJ_MAIL));
    	aa.addArg("web", r.getParameter(REQ_PAR_PRJ_WEB));
    	as.execute(aa);
    	
    	if (aa.hasErrors()) {
            getVelocityContext().put("RESULTS", aa.errors());
            return null;
    	} else { 
            getVelocityContext().put("RESULTS", aa.results());
            return getProjectByName(r.getParameter(REQ_PAR_PRJ_NAME));
    	}
    }

	protected StoredProject getProjectByName(String parameter) {
		return StoredProject.getProjectByName(parameter);
	}
    
    // ---------------------------------------------------------------
    // Remove project
    // ---------------------------------------------------------------
    protected StoredProject removeProject(StringBuilder e, 
    		StoredProject selProject, int indent) {
    	if (selProject != null) {
			// Deleting large projects in the foreground is
			// very slow
			ProjectDeleteJob pdj = new ProjectDeleteJob(sobjCore, selProject);
			try {
				getScheduler().enqueue(pdj);
			} catch (SchedulerException e1) {
				e.append(sp(indent)).append(getErr("e0034")).append("<br/>\n");
			}
			selProject = null;
		} else {
			e.append(sp(indent) + getErr("e0034") + "<br/>\n");
		}
    	return selProject;
    }

	protected ProjectDeleteJob createProjectDeleteJob(StoredProject selProject) {
		return new ProjectDeleteJob(sobjCore, selProject);
	}

	protected Scheduler getScheduler() {
		return sobjSched;
	}

	// ---------------------------------------------------------------
	// Trigger an update
	// ---------------------------------------------------------------
	protected void triggerUpdate(StringBuilder e,
			StoredProject selProject, int indent, String mnem) {
		AdminService as = getAdminService();
		AdminAction aa = as.create(UpdateProject.MNEMONIC);
		aa.addArg("project", selProject.getId());
		aa.addArg("updater", mnem);
		as.execute(aa);

		if (aa.hasErrors()) {
            getVelocityContext().put("RESULTS", aa.errors());
        } else { 
            getVelocityContext().put("RESULTS", aa.results());
        }
	}

	protected VelocityContext getVelocityContext() {
		return vc;
	}

	protected AdminService getAdminService() {
		return AlitheiaCore.getInstance().getAdminService();
	}

	// ---------------------------------------------------------------
	// Trigger update on all resources for that project
	// ---------------------------------------------------------------
	protected void triggerAllUpdate(StringBuilder e,
			StoredProject selProject, int indent) {
	    AdminService as = getAdminService();
        AdminAction aa = as.create(UpdateProject.MNEMONIC);
        aa.addArg("project", selProject.getId());
        as.execute(aa);

        if (aa.hasErrors()) {
            getVelocityContext().put("RESULTS", aa.errors());
        } else {
            getVelocityContext().put("RESULTS", aa.results());
        }
	}
	
	// ---------------------------------------------------------------
	// Trigger update on all resources on all projects of a node
	// ---------------------------------------------------------------
    protected void triggerAllUpdateNode(StringBuilder e,
			StoredProject selProject, int in) {
		Set<StoredProject> projectList = getThisNodeProjects();
		
		// RENG: would this not only put the results of the last update in velocity?
		for (StoredProject project : projectList) {
			triggerAllUpdate(e, project, in);
		}
	}
	
	// ---------------------------------------------------------------
	// Trigger synchronize on the selected plug-in for that project
	// ---------------------------------------------------------------
    protected void syncPlugin(StringBuilder e, StoredProject selProject, String reqValSyncPlugin) {
		if ((reqValSyncPlugin != null) && (selProject != null)) {
			PluginInfo pInfo = getPluginAdmin().getPluginInfo(reqValSyncPlugin);
			if (pInfo != null) {
				AlitheiaPlugin pObj = getPluginAdmin().getPlugin(pInfo);
				if (pObj != null) {
					getMetricActivator().syncMetric(pObj, selProject);
					getLogger().debug("Syncronise plugin (" + pObj.getName()
							+ ") on project (" + selProject.getName() + ").");
				}
			}
		}
    }

	protected Logger getLogger() {
		return sobjLogger;
	}

	protected MetricActivator getMetricActivator() {
		return compMA;
	}

	protected PluginAdmin getPluginAdmin() {
		return sobjPA;
	}
    
    protected void createForm(StringBuilder b, StringBuilder e, 
    		StoredProject selProject, String reqValAction, int in) {

    	StringBuilder b_internal = new StringBuilder();
    	
        // ===============================================================
        // Create the form
        // ===============================================================
    	HTMLFormBuilder formBuilder = form().withId("projects").withName("projects").withMethod(POST).withAction("/projects");

        // ===============================================================
        // Display the accumulated error messages (if any)
        // ===============================================================
        b_internal.append(errorFieldset(e, ++in));

        // Get the complete list of projects stored in the SQO-OSS framework
        Set<StoredProject> projects = getThisNodeProjects();
        Collection<PluginInfo> metrics = getPluginAdmin().listPlugins();

        // ===================================================================
        // "Show project info" view
        // ===================================================================
        if ((ACT_REQ_SHOW_PROJECT.equals(reqValAction))
                && (selProject != null)) {
            // Create the input fields
            StringBuilder infoRows = new StringBuilder();            
            infoRows.append(normalInfoRow(
                    "Project name", selProject.getName(), in));
            infoRows.append(normalInfoRow(
                    "Homepage", selProject.getWebsiteUrl(), in));
            infoRows.append(normalInfoRow(
                    "Contact e-mail", selProject.getContactUrl(), in));
            infoRows.append(normalInfoRow(
                    "Bug database", selProject.getBtsUrl(), in));
            infoRows.append(normalInfoRow(
                    "Mailing list", selProject.getMailUrl(), in));
            infoRows.append(normalInfoRow(
                    "Source code", selProject.getScmUrl(), in));
            b_internal.append(infoRows.toString());

            b_internal.append(
        		node("fieldset")
        			.with(node("legend")
        				.appendContent("Project information")
        			)
        			.with(table().withClass("borderless")
        				.appendContent(infoRows.toString())
        				// toolbar
        				.with(
        					tableRow()
        					.with(
        						tableColumn().withColspan(2).withClass("borderless")
        						.with(defaultButton()
        							.withValue(getLbl("btn_back"))
        							.withAttribute("onclick", doSubmitString())
        						)
        					)
        				)
        			)
        		.build()
        	);
        }
        // ===================================================================
        // "Add project" editor
        // ===================================================================
        else if (ACT_REQ_ADD_PROJECT.equals(reqValAction)) {
            // Create the field-set
            // Create the input fields
            StringBuilder infoRows = new StringBuilder();  
            infoRows.append(normalInputRow(
                    "Project name", REQ_PAR_PRJ_NAME, "", in));
            infoRows.append(normalInputRow(
                    "Homepage", REQ_PAR_PRJ_WEB, "", in));
            infoRows.append(normalInputRow(
                    "Contact e-mail", REQ_PAR_PRJ_CONT, "", in));
            infoRows.append(normalInputRow(
                    "Bug database", REQ_PAR_PRJ_BUG, "", in));
            infoRows.append(normalInputRow(
                    "Mailing list", REQ_PAR_PRJ_MAIL, "", in));
            infoRows.append(normalInputRow(
                    "Source code", REQ_PAR_PRJ_CODE, "", in));

            b_internal.append(
            	table().withClass("borderless").withStyle("width: 100%").
		        with(
		        	text(infoRows.toString()),
		        	// toolbar
		        	tableRow().with(
		        		tableColumn().withColspan(2).withClass("borderless").
		        		with(
		        			defaultButton()
		        				.withValue(getLbl("project_add"))
		        				.withAttribute("onclick", doSetActionAndSubmitString(ACT_CON_ADD_PROJECT)),
		        			defaultButton()
		        				.withValue(getLbl("cancel"))
		        				.withAttribute("onclick", doSubmitString())
		        		)
		        	)
		        ).build()
        	);
        }
        // ===================================================================
        // "Delete project" confirmation view
        // ===================================================================
        else if ((ACT_REQ_REM_PROJECT.equals(reqValAction))
                && (selProject != null)) {
            b_internal.append(
            	node("fieldset").with(
	            	node("legend").with(
	            		text(getLbl("l0059") + ": " + selProject.getName())),
	            	table().withClass("borderless").with(
	            		// confirmation message
	            		tableRow().with(
            				tableColumn().withClass("borderless").with(
            					node("b").with(text(getMsg("delete_project")))
            				)
	            		),
            			// toolbar
	            		tableRow().with(
	            			tableColumn().withClass("borderless").with(
	            				defaultButton()
	            					.withValue(getLbl("l0006"))
	            					.withAttribute("onclick", doSetActionAndSubmitString(ACT_CON_REM_PROJECT)),
	            				defaultButton()
		            				.withValue(getLbl("l0004"))
		            				.withAttribute("onclick", doSubmitString())
	            			)
	            		)
	            	)
            	).build()
            );
        }
        // ===================================================================
        // Projects list view
        // ===================================================================
        else {
        	StringBuilder projectsList = new StringBuilder();

            if (projects.isEmpty()) {
                projectsList.append(sp(in++) + "<tr>\n");
                projectsList.append(sp(in) + "<td colspan=\"6\" class=\"noattr\">\n"
                        + getMsg("no_projects")
                        + "</td>\n");
                projectsList.append(sp(--in) + "</tr>\n");
            }
            else {
                //------------------------------------------------------------
                // Create the content rows
                //------------------------------------------------------------
                for (StoredProject nextPrj : projects) {
                    boolean selected = false;
                    if ((selProject != null)
                            && (selProject.getId() == nextPrj.getId())) {
                        selected = true;
                    }
                    projectsList.append(sp(in++) + "<tr class=\""
                            + ((selected) ? "selected" : "edit") + "\""
                            + " onclick=\"javascript:"
                            + "document.getElementById('"
                            + REQ_PAR_PROJECT_ID + "').value='"
                            + ((selected) ? "" : nextPrj.getId())
                            + "';"
                            + SUBMIT + "\">\n");
                    // Project Id
                    projectsList.append(sp(in) + "<td class=\"trans\">"
                            + nextPrj.getId()
                            + "</td>\n");
                    // Project name
                    projectsList.append(sp(in) + "<td class=\"trans\">"
                            + ((selected)
                                    ? "<input type=\"button\""
                                        + " class=\"install\""
                                        + " style=\"width: 100px;\""
                                        + " value=\""
                                        + getLbl("btn_info")
                                        + "\""
                                        + " onclick=\"javascript:"
                                        + "document.getElementById('"
                                        + REQ_PAR_ACTION + "').value='" 
                                        + ACT_REQ_SHOW_PROJECT + "';"
                                        + SUBMIT + "\" />"
                                    : "<img src=\"/edit.png\""
                                        + " alt=\"[Edit]\"/>")
                            + "&nbsp;"
                            + nextPrj.getName()
                            + "</td>\n");
                    // Last project version
                    String lastVersion = getLbl("l0051");
                    ProjectVersion v = getLastProjectVersion(nextPrj);
                    if (v != null) {
                        lastVersion = String.valueOf(v.getSequence()) + "(" + v.getRevisionId() + ")";
                    }
                    projectsList.append(sp(in) + "<td class=\"trans\">"
                            + lastVersion
                            + "</td>\n");
                    // Date of the last known email
                    MailMessage mm = getLastMailMessage(nextPrj);
                    projectsList.append(sp(in) + "<td class=\"trans\">"
                            + ((mm == null)?getLbl("l0051"):mm.getSendDate())
                            + "</td>\n");
                    // ID of the last known bug entry
                    Bug bug = getLastBug(nextPrj);
                    projectsList.append(sp(in) + "<td class=\"trans\">"
                            + ((bug == null)?getLbl("l0051"):bug.getBugID())
                            + "</td>\n");
                    // Evaluation state
                    String evalState = getLbl("project_not_evaluated");
                    if (nextPrj.isEvaluated()) {
                    	evalState = getLbl("project_is_evaluated");
                    }
                    projectsList.append(sp(in) + "<td class=\"trans\">"
                            + evalState
                            + "</td>\n");
                    
                    // Cluster node
                    String nodename = null;
                    if (null != nextPrj.getClusternode()) {
                        nodename = nextPrj.getClusternode().getName();
                    } else {
                        nodename = "(local)";
                    }
                    projectsList.append(sp(in) + "<td class=\"trans\">" + nodename + "</td>\n");
                    projectsList.append(sp(--in) + "</tr>\n");
                    if ((selected) && (metrics.isEmpty() == false)) {
                        showLastAppliedVersion(nextPrj, metrics, projectsList);
                    }
                }
            }
            //----------------------------------------------------------------
            // Tool-bar
            //----------------------------------------------------------------
            Collection<HTMLTableRowBuilder> toolbar = toolbar(selProject);
			for (HTMLTableRowBuilder row : toolbar) {
				projectsList.append(row.build((long) in));
			}

            b_internal.append(
            	table().with(
            		headerRow(),
            		node("tbody").with(
            			text(projectsList.toString())
            		)
            	)
            	.build()
            );
        }

        // ===============================================================
        // INPUT FIELDS
        // ===============================================================
        addHiddenFields(selProject,b_internal,in);

        formBuilder.appendContent(b_internal.toString());
        
        b.append(formBuilder.build());
    }

	protected static String doSubmitString() {
		return "javascript:" + SUBMIT;
	}
	
	protected static String doSetActionAndSubmitString(String action) {
		return "javascript:document.getElementById('" + REQ_PAR_ACTION + "').value='" + action + "';" + SUBMIT;
	}

	protected static HTMLInputBuilder defaultButton() {
		return input()
			.withType(BUTTON)
			.withClass("install")
			.withStyle("width: 100px;");
	}

	protected Bug getLastBug(StoredProject project) {
		return Bug.getLastUpdate(project);
	}

	protected MailMessage getLastMailMessage(StoredProject project) {
		return MailMessage.getLatestMailMessage(project);
	}

	protected ProjectVersion getLastProjectVersion(StoredProject project) {
		return ProjectVersion.getLastProjectVersion(project);
	}

	protected Set<StoredProject> getThisNodeProjects() {
		return ClusterNode.thisNode().getProjects();
	}


    protected void addHiddenFields(StoredProject selProject,
            StringBuilder b,
            long in) {
        // "Action type" input field
        b.append(sp(in) + "<input type='hidden' id='" + REQ_PAR_ACTION + 
                "' name='" + REQ_PAR_ACTION + "' value='' />\n");
        // "Project Id" input field
        b.append(sp(in) + "<input type='hidden' id='" + REQ_PAR_PROJECT_ID +
                "' name='" + REQ_PAR_PROJECT_ID +
                "' value='" + ((selProject != null) ? selProject.getId() : "") +
                "' />\n");
        // "Plug-in hashcode" input field
        b.append(sp(in) + "<input type='hidden' id='" + REQ_PAR_SYNC_PLUGIN +
                "' name='" + REQ_PAR_SYNC_PLUGIN + 
                "' value='' />\n");
    }
    
    protected Collection<HTMLTableRowBuilder> toolbar(StoredProject project) {
    	String projectID = (project != null) ? "?" + REQ_PAR_PROJECT_ID + "=" + project.getId() : "";
		return Arrays.asList(
    		tableRow().withClass("subhead").with(
    			tableColumn().with(text("View")),
    			tableColumn().withColspan(6).with(
    				defaultButton()
    					.withValue(getLbl("l0008"))
    					.withAttribute("onclick",
    							"javascript:window.location='/projects" + projectID + "';")
    			)
    		),
    		tableRow().withClass("subhead").with(
    			tableColumn().with(text("Manage")),
    			tableColumn().withColspan(6).with(
    				defaultButton()
    					.withValue(getLbl("add_project"))
    					.withAttribute("onclick", doSetActionAndSubmitString(ACT_REQ_ADD_PROJECT)),
    				defaultButton()
	    				.withValue(getLbl("l0059"))
	    				.withAttribute("onclick", doSetActionAndSubmitString(ACT_REQ_REM_PROJECT))
	    				.withDisabled(project == null)
    			)
    		),
    		tableRow().withClass("subhead").with(
    			tableColumn().with(text("Update")),
    			tableColumn().withColspan(4)
    				.with(
    					(project == null) ? GenericHTMLBuilder.EMPTY_ARRAY : new GenericHTMLBuilder<?>[]{updaterSelector(project)}
    				).with(
    					input()
		    				.withType(BUTTON)
		    				.withClass("install")
		    				.withValue("Run Updater")
		    				.withAttribute("onclick", doSetActionAndSubmitString(ACT_CON_UPD))
		    				.withDisabled(project == null),
						input()
		    				.withType(BUTTON)
		    				.withClass("install")
		    				.withValue("Run All Updaters")
		    				.withAttribute("onclick", doSetActionAndSubmitString(ACT_CON_UPD_ALL))
		    				.withDisabled(project == null)
	    			),
	    		tableColumn().withColspan(2).withAttribute("align", "right").with(
    				input()
	    				.withType(BUTTON)
	    				.withClass("install")
	    				.withValue("Update all on " + getClusterNodeName())
	    				.withAttribute("onclick", doSetActionAndSubmitString(ACT_CON_UPD_ALL_NODE))
	    		)
    		)
    	);
    }
    
    private GenericHTMLBuilder<?> updaterSelector(StoredProject project) {
		return
			node("select").withName(REQ_PAR_UPD).withId(REQ_PAR_UPD).with(
				node("optgroup").withAttribute("label", "Import Stage").with(
					getUpdaterOptions(project, UpdaterStage.IMPORT).toArray(GenericHTMLBuilder.EMPTY_ARRAY)
				),
				node("optgroup").withAttribute("label", "Parse Stage").with(
					getUpdaterOptions(project, UpdaterStage.PARSE).toArray(GenericHTMLBuilder.EMPTY_ARRAY)
				),
				node("optgroup").withAttribute("label", "Inference Stage").with(
					getUpdaterOptions(project, UpdaterStage.INFERENCE).toArray(GenericHTMLBuilder.EMPTY_ARRAY)
				),
				node("optgroup").withAttribute("label", "Default Stage").with(
					getUpdaterOptions(project, UpdaterStage.DEFAULT).toArray(GenericHTMLBuilder.EMPTY_ARRAY)
				)
			);
	}

	private Collection<GenericHTMLBuilder<?>> getUpdaterOptions(
			StoredProject project, UpdaterStage stage) {
		Collection<GenericHTMLBuilder<?>> options = new ArrayList<GenericHTMLBuilder<?>>();
		
		for (Updater u : getUpdaters(project, stage)) {
			options.add(
				node("option").withAttribute("value", u.mnem()).with(text(u.descr()))
			);
		}
		
		return options;
	}

	protected Set<Updater> getUpdaters(StoredProject selProject,
			UpdaterStage importStage) {
		return sobjUpdater.getUpdaters(selProject, importStage);
	}

	protected String getClusterNodeName() {
		return sobjClusterNode.getClusterNodeName();
	}
    
    protected void showLastAppliedVersion(
            StoredProject project,
            Collection<PluginInfo> metrics,
            StringBuilder b) {
        for(PluginInfo m : metrics) {
            if (m.installed) {
                b.append("<tr>\n");
                b.append(sp(1) + "<td colspan=\"7\""
                        + " class=\"noattr\">\n"
                        + "<input type=\"button\""
                        + " class=\"install\""
                        + " style=\"width: 100px;\""
                        + " value=\"Synchronise\""
                        + " onclick=\"javascript:"
                        + "document.getElementById('"
                        + REQ_PAR_SYNC_PLUGIN + "').value='"
                        + m.getHashcode() + "';"
                        + SUBMIT + "\""
                        + "/>"
                        + "&nbsp;"
                        + m.getPluginName()
                        + "</td>\n");
                b.append("</tr>\n");
            }
        }
    }
    
    protected static HTMLNodeBuilder headerRow() {
    	return
    		node("thead").with(
				tableRow().withClass("head").with(
					tableColumn().withClass("head").withStyle("width: 10%").with(text(getLbl("l0066"))),
					tableColumn().withClass("head").withStyle("width: 35%").with(text(getLbl("l0067"))),
					tableColumn().withClass("head").withStyle("width: 15%").with(text(getLbl("l0068"))),
					tableColumn().withClass("head").withStyle("width: 15%").with(text(getLbl("l0069"))),
					tableColumn().withClass("head").withStyle("width: 15%").with(text(getLbl("l0070"))),
					tableColumn().withClass("head").withStyle("width: 10%").with(text(getLbl("l0071"))),
					tableColumn().withClass("head").withStyle("width: 10%").with(text(getLbl("l0073")))
				)
			);
    }
}

// vi: ai nosi sw=4 ts=4 expandtab

