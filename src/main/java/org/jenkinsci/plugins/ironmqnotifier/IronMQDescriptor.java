package org.jenkinsci.plugins.ironmqnotifier;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class IronMQDescriptor extends BuildStepDescriptor<Publisher> {

    public IronMQDescriptor() {
        super(IronMQNotifier.class);
        load();
    }

    @Override
    public String getDisplayName() {
        return "Send Message to IronMQ";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        save();
        return super.configure(req, formData);
    }

    @Override
    public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> arg0) {
        return true;
    }

    @Override
    public IronMQNotifier newInstance(StaplerRequest req, JSONObject formData) throws FormException {

        String projectId = formData.optString("projectId");
        String tokenID = formData.optString("token");
        String queueName = formData.optString("queueName");
        String preferredServer = formData.optString("preferredServer");
        boolean success = formData.optBoolean("send_success");
        boolean failure = formData.optBoolean("send_failure");
        boolean unstable = formData.optBoolean("send_unstable");
        int expirySeconds = formData.optInt("expirySeconds");

        return new IronMQNotifier(projectId, tokenID, queueName, preferredServer, success, failure, unstable, expirySeconds);
    }

   /*                      Form Validation Logic Goes Here                */

    public static FormValidation doCheckQueueName(@QueryParameter String value) {
        if (value == null) { value = ""; }
        if (isValidQueueName(value)) return FormValidation.ok();
        else return FormValidation.error("QueueName must be Alpha characters only");
    }

    public static FormValidation doCheckPreferredServer(@QueryParameter String value) {
        if (value == null ) { value = ""; }
        if (isValidServerName(value)) return FormValidation.ok();
        else return FormValidation.error("Server Name cannot be empty");
    }

    public static FormValidation doCheckExpirySeconds(@QueryParameter Integer value) {
        if (value == null) { value = 0; }
        if (value > 0) return FormValidation.ok();
        else return FormValidation.error("Expiry Seconds should not be zero. Default will be set");
    }

    private static boolean isValidQueueName(String name) {
        return !name.isEmpty() && isAlpha(name);

    }

    private static boolean isValidServerName(String name) {
        return !name.isEmpty();

    }

    private static boolean isAlpha(String name) {
        return name.matches("[a-zA-Z]+");
    }
}