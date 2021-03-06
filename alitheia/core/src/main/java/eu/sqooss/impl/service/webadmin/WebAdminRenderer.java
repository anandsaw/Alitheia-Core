/*
 * This file is part of the Alitheia system, developed by the SQO-OSS
 * consortium as part of the IST FP6 SQO-OSS project, number 033331.
 *
 * Copyright 2008 - 2010 - Organization for Free and Open Source Software,  
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

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.osgi.framework.BundleContext;

import eu.sqooss.service.scheduler.Job;
import eu.sqooss.service.util.StringUtils;

/**
 * The WebAdminRender class provides functions for rendering content
 * to be displayed within the WebAdmin interface.
 *
 * @author, Paul J. Adams <paul.adams@siriusit.co.uk>
 * @author, Boryan Yotov <b.yotov@prosyst.com>
 */
public class WebAdminRenderer  extends AbstractView {
    /**
     * Represents the system time at which the WebAdminRender (and
     * thus the system) was started. This is required for the system
     * uptime display.
     */
    private static long startTime = new Date().getTime();

    public WebAdminRenderer(BundleContext bundlecontext, VelocityContext vc) {
        super(bundlecontext, vc);
    }

    /**
     * Creates and HTML table displaying the details of all the jobs
     * that have failed whilst the system has been up
     *
     * @return a String representing the HTML table
     */
    public String renderJobFailStats() {
        StringBuilder result = new StringBuilder();
        HashMap<String, Integer> fjobs = getFailedJobs();
        result.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n");
        result.append("\t<thead>\n");
        result.append("\t\t<tr>\n");
        result.append("\t\t\t<td>Job Type</td>\n");
        result.append("\t\t\t<td>Num Jobs Failed</td>\n");
        result.append("\t\t</tr>\n");
        result.append("\t</thead>\n");
        result.append("\t<tbody>\n");

        String[] jobfailures = fjobs.keySet().toArray(new String[1]);
        for(String key : jobfailures) {
            result.append("\t\t<tr>\n\t\t\t<td>");
            result.append(key==null ? "No failures" : key);
            result.append("</td>\n\t\t\t<td>");
            result.append(key==null ? "&nbsp;" : fjobs.get(key));
            result.append("\t\t\t</td>\n\t\t</tr>");
        }
        result.append("\t</tbody>\n");
        result.append("</table>");
        return result.toString();
    }

	protected HashMap<String, Integer> getFailedJobs() {
		HashMap<String,Integer> fjobs = sobjSched.getSchedulerStats().getFailedJobTypes();
		return fjobs;
	}
	
	

    /**
     * Creates and HTML table with information about the jobs that
     * failed and the recorded exceptions
     * @return
     */
    public String renderFailedJobs() {
        StringBuilder result = new StringBuilder();
        Job[] jobs = getFailedQueue();
        result.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n");
        result.append("\t<thead>\n");
        result.append("\t\t<tr>\n");
        result.append("\t\t\t<td>Job Type</td>\n");
        result.append("\t\t\t<td>Exception type</td>\n");
        result.append("\t\t\t<td>Exception text</td>\n");
        result.append("\t\t\t<td>Exception backtrace</td>\n");
        result.append("\t\t</tr>\n");
        result.append("\t</thead>\n");
        result.append("\t<tbody>\n");

        if ((jobs != null) && (jobs.length > 0)) {
            for(Job j: jobs) {
                if (j == null) continue;
                result.append("\t\t<tr>\n\t\t\t<td>");
                if (j.getClass() != null) {
                    try {
                        //result.append(j.getClass().getPackage().getName());
                        //result.append(". " + j.getClass().getSimpleName());
			result.append(j.toString());
                    }
                    catch (NullPointerException ex) {
                        result.append("<b>NA</b>");
                    }
                }
                else {
                    result.append("<b>NA</b>");
                }
                result.append("</td>\n\t\t\t<td>");
                Exception e = getException(j);
                if (e != null) {
                    try {
                        result.append(e.getClass().getPackage().getName());
                        result.append(". " + e.getClass().getSimpleName());
                    }
                    catch (NullPointerException ex) {
                        result.append("<b>NA</b>");
                    }
                }
                else {
                    result.append("<b>NA</b>");
                }
                result.append("</td>\n\t\t\t<td>");
                try {
                    result.append(e.getMessage());
                }
                catch (NullPointerException ex) {
                    result.append("<b>NA</b>");
                }
                result.append("</td>\n\t\t\t<td>");
                if ((e != null)
                        && (e.getStackTrace() != null)) {
                    for(StackTraceElement m: e.getStackTrace()) {
                        if (m == null) continue;
                        result.append(m.getClassName());
                        result.append(". ");
                        result.append(m.getMethodName());
                        result.append("(), (");
                        result.append(m.getFileName());
                        result.append(":");
                        result.append(m.getLineNumber());
                        result.append(")<br/>");
                    }
                }
                else {
                    result.append("<b>NA</b>");
                }
                result.append("\t\t\t</td>\n\t\t</tr>");
            }
        }
        else {
            result.append ("<tr><td colspan=\"4\">No failed jobs.</td></tr>");
        }
        result.append("\t</tbody>\n");
        result.append("</table>");

        return result.toString();
    }

	protected Exception getException(Job j) {
		return j.getErrorException();
	}

	protected Job[] getFailedQueue() {
		Job[] jobs = sobjSched.getFailedQueue();
		return jobs;
	}

    /**
     * Creates an HTML unordered list displaying the contents of the current system log
     *
     * @return a String representing the HTML unordered list items
     */
    public String renderLogs() {
        String[] names = getLogs();

        if ((names != null) && (names.length > 0)) {
            StringBuilder b = new StringBuilder();
            for (String s : names) {
                b.append("\t\t\t\t\t<li>" + StringUtils.makeXHTMLSafe(s) + "</li>\n");
            }

            return b.toString();
        } else {
            return "\t\t\t\t\t<li>&lt;none&gt;</li>\n";
        }
    }

	protected String[] getLogs() {
		String[] names = sobjLogManager.getRecentEntries();
		return names;
	}

    /**
     * Returns a string representing the uptime of the Alitheia core
     * in dd:hh:mm:ss format
     */
    public static String getUptime() {
        long remainder;
        long timeRunning = getRunningTime();
        // Get the elapsed time in days, hours, mins, secs
        int days = new Long(timeRunning / 86400000).intValue();
        remainder = timeRunning % 86400000;
        int hours = new Long(remainder / 3600000).intValue();
        remainder = remainder % 3600000;
        int mins = new Long(remainder / 60000).intValue();
        remainder = remainder % 60000;
        int secs = new Long(remainder / 1000).intValue();

        return String.format("%d:%02d:%02d:%02d", days, hours, mins, secs);
    }

	protected static long getRunningTime() {
		long currentTime = new Date().getTime();
        long timeRunning = currentTime - startTime;
		return timeRunning;
	}

	   

    public String renderJobWaitStats() {
        StringBuilder result = new StringBuilder();
        HashMap<String, Integer> wjobs = getWaitingJobs();
        result.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n");
        result.append("\t<thead>\n");
        result.append("\t\t<tr>\n");
        result.append("\t\t\t<td>Job Type</td>\n");
        result.append("\t\t\t<td>Num Jobs Waiting</td>\n");
        result.append("\t\t</tr>\n");
        result.append("\t</thead>\n");
        result.append("\t<tbody>\n");

        String[] jobfailures = wjobs.keySet().toArray(new String[1]);
        for(String key : jobfailures) {
            result.append("\t\t<tr>\n\t\t\t<td>");
            result.append(key==null ? "No failures" : key);
            result.append("</td>\n\t\t\t<td>");
            result.append(key==null ? "&nbsp;" : wjobs.get(key));
            result.append("\t\t\t</td>\n\t\t</tr>");
        }
        result.append("\t</tbody>\n");
        result.append("</table>");
        return result.toString();
    }

	protected HashMap<String, Integer> getWaitingJobs() {
		HashMap<String,Integer> wjobs = sobjSched.getSchedulerStats().getWaitingJobTypes();
		return wjobs;
	}

    public String renderJobRunStats() {
        StringBuilder result = new StringBuilder();
        List<String> rjobs = getRunJobs();
        if (rjobs.size() == 0) {
            return "No running jobs";
        }
        result.append("<ul>\n");
        for(String s : rjobs) {
            result.append("\t<li>");
            result.append(s);
            result.append("\t</li>\n");
        }
        result.append("</ul>\n");
        return result.toString();
    }

	protected List<String> getRunJobs() {
		List<String> rjobs = sobjSched.getSchedulerStats().getRunJobs();
		return rjobs;
	}
    
protected class TestableWebAdminRenderer extends WebAdminRenderer {
		
		HashMap<String, Integer> failedJobs;
		Job[] jobs;
		String[] logs;
		HashMap<String, Integer> waitingJobs;
		List<String> runJobs;
		Exception exception;
		
		public TestableWebAdminRenderer(BundleContext bundlecontext,
				VelocityContext vc, HashMap<String, Integer> failedJobs, Job[] jobs, String[] logs, HashMap<String, Integer> waitingJobs, List<String> runJobs, Exception exception) {
			super(bundlecontext, vc);
			this.failedJobs = failedJobs;
			this.jobs = jobs;
			this.logs = logs;
			this.waitingJobs = waitingJobs;
			this.runJobs = runJobs;
			this.exception = exception;
			
		}
		
		@Override
		protected HashMap<String, Integer> getFailedJobs() {
			return this.failedJobs;
		}
		
		@Override
		protected Job[] getFailedQueue() {
			return this.jobs;
		}

		
		@Override
		protected String[] getLogs() {
			return this.logs;
		}
		
		@Override
		protected List<String> getRunJobs() {
			return this.runJobs;
		}
		
		@Override
		protected HashMap<String, Integer> getWaitingJobs() {
			return waitingJobs;
		}
		
		@Override
		protected Exception getException(Job j) {
			return this.exception;
		}
		
		
	}
}

//vi: ai nosi sw=4 ts=4 expandtab
