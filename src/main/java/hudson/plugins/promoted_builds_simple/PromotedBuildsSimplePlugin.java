/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Alan Harder
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.promoted_builds_simple;

import hudson.Extension;
import hudson.Plugin;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Simply add a link in the main page sidepanel.
 * @author Alan.Harder@sun.com
 */
public class PromotedBuildsSimplePlugin extends Plugin {
    private List<PromotionLevel> levels = new ArrayList<PromotionLevel>();

    @Override public void start() throws Exception {
	// Default levels (load() will replace these if customized)
	levels.add(new PromotionLevel("QA build", "qa.gif"));
	levels.add(new PromotionLevel("QA approved", "qa-green.gif"));
	levels.add(new PromotionLevel("GA release", "ga.gif"));
	load();
    }

    public List<PromotionLevel> getLevels() { return levels; }

    @Override public void configure(StaplerRequest req, JSONObject formData)
	    throws IOException, ServletException, FormException {
	levels.clear();
	levels.addAll(req.bindJSONToList(PromotionLevel.class, formData.get("levels")));
	save();
    }

    public void doMakePromotable(StaplerRequest req, StaplerResponse rsp) throws IOException {
	req.findAncestorObject(Job.class).checkPermission(Run.UPDATE);
	Run run = req.findAncestorObject(Run.class);
	if (run != null) {
	    run.addAction(new PromoteAction());
	    run.save();
	    rsp.sendRedirect(
		req.getRequestURI().substring(0, req.getRequestURI().indexOf("parent/parent")));
	}
    }

    @Extension
    public static class PromotedBuildsRunListener extends RunListener<Run> {
	public PromotedBuildsRunListener() {
	    super(Run.class);
	}

	@Override
	public void onCompleted(Run run, TaskListener listener) {
	    Result res = run.getResult();
	    if (res != Result.FAILURE && res != Result.ABORTED) {
		run.addAction(new PromoteAction());
	    }
	}
    }

    @Extension public static Descriptor initBuildSelector() {
        // Add BuildSelector extension if Copy Artifact plugin is present
        try {
            Class.forName("hudson.plugins.copyartifact.BuildSelector");
            return PromotedBuildSelector.DESCRIPTOR;
        } catch (ClassNotFoundException ignore) {
            return null;
        }
    }
}
