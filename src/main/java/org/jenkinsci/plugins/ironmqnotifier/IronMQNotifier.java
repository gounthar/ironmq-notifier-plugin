package org.jenkinsci.plugins.ironmqnotifier;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ExceptionCatchingThreadFactory;
import hudson.util.FormValidation;
import io.iron.ironmq.Client;
import io.iron.ironmq.Cloud;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.jenkinsci.plugins.ironmqnotifier.ironwrapper.ClientWrapper;
import org.jenkinsci.plugins.ironmqnotifier.ironwrapper.IronConstants;
import org.jenkinsci.plugins.ironmqnotifier.ironwrapper.IronMQSender;
import org.jenkinsci.plugins.ironmqnotifier.ironwrapper.IronMessageSettings;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

/**
 * <p>IronMQNotifier class.</p>
 *
 * @author Mike Caspar
 * @version $Id: $
 */
public class IronMQNotifier extends Notifier {

    private static final Logger LOGGER
            = Logger.getLogger("IronMQNotifier");

    private String preferredServerName;
    private String defaultPreferredServerName;

    public boolean send_success;
    public boolean send_failure;
    public boolean send_unstable;

    private String token;
    private long expirySeconds;
    private String projectId;
    private String queueName;
    private String jobName = "";

    private String resultString = "UNKNOWN";

    private Client client;

    @DataBoundConstructor
    public IronMQNotifier(final String projectId,
                          final String token,
                          final String queueName,
                          final String preferredServerName,
                          final boolean send_success,
                          final boolean send_failure,
                          final boolean send_unstable,
                          final long expirySeconds) {

        this.projectId = projectId;
        this.token = token;
        this.queueName = queueName;
        this.send_success = send_success;
        this.send_failure = send_failure;
        this.send_unstable = send_unstable;
        this.preferredServerName = preferredServerName;
        this.expirySeconds = expirySeconds;


        adjustDataToAvoidCrashes();

    }


    private void adjustDataToAvoidCrashes() {

        if (this.queueName.trim().length() == 0) {
            this.queueName = IronConstants.DEF_QUEUE_NAME;
        }

        if (this.preferredServerName.trim().length() == 0) {
            this.preferredServerName
                    = IronConstants.DEFAULT_PREFERRED_SERVER_NAME;
        }

        if (this.expirySeconds <= 0) {
            setExpirySeconds(IronConstants.DEF_EXPIRY_SEC);
        }
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build,
                           Launcher launcher,
                           BuildListener listener)
            throws InterruptedException, IOException {

        this.jobName = build.getFullDisplayName();

        if (build.getResult() == Result.SUCCESS) {
            if (!send_success) {
                return true;
            }
            this.resultString = "succeeded";
        } else if (build.getResult() == Result.UNSTABLE) {
            if (!send_unstable) {
                return true;
            }
            this.resultString = "was unstable";
        } else if (build.getResult() == Result.FAILURE) {
            if (!send_failure) {
                return true;
            }
            this.resultString = "failed";
        } else {
            return true;
        }


        try {

            SendMessageToIronMQ();

        } catch (Exception ex) {

            logConfigurationWarning(ex);


            return false;

        }

        return true;
    }

    private void SendMessageToIronMQ() throws IOException {

        client = generateClientToUse();

        IronMessageSettings ironMessageSettings = generateMessageSettings();

        IronMQSender sender = new IronMQSender();

        sender.send(client, ironMessageSettings);
    }

    private Client generateClientToUse() {
        return new ClientWrapper(
                this.projectId,
                this.token,
                new Cloud(IronConstants.DEF_PREFERRED_SERVER_SCHEME,
                        this.preferredServerName,
                        IronConstants.DEF_PREFERRED_SERVER_PORT)
        );
    }


    private IronMessageSettings generateMessageSettings() {
        IronMessageSettings ironMessageSettings = new IronMessageSettings();
        ironMessageSettings.setExpirySeconds(this.expirySeconds);
        ironMessageSettings.setJobName(this.jobName);
        ironMessageSettings.setBuildResultString(this.resultString);
        ironMessageSettings.setQueueName(this.queueName);
        return ironMessageSettings;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    /**
     * <p>Getter for the field <code>jobName</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     * @since 1.0.6
     */
    protected String getJobName() {
        return this.jobName;
    }

    /**
     * <p>Getter for the field <code>queueName</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     * @since 1.0.6
     */
    public String getQueueName() {
        return this.queueName;
    }

    /**
     * <p>Setter for the field <code>queueName</code>.</p>
     *
     * @param queueName a {@link java.lang.String} object.
     * @since 1.0.6
     */
    public void setQueueName(final String queueName) {
        this.queueName = queueName;
    }

    /**
     * <p>Getter for the field <code>projectId</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     * @since 1.0.6
     */
    public String getProjectId() {
        return this.projectId;
    }

    /**
     * <p>Setter for the field <code>projectId</code>.</p>
     *
     * @param projectId a {@link java.lang.String} object.
     * @since 1.0.6
     */
    public void setProjectId(final String projectId) {
        this.projectId = projectId;
    }

    /**
     * <p>Getter for the field <code>token</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     * @since 1.0.6
     */
    public String getToken() {
        return token;
    }

    /**
     * <p>Setter for the field <code>token</code>.</p>
     *
     * @param token a {@link java.lang.String} object.
     * @since 1.0.6
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * <p>Getter for the field <code>expirySeconds</code>.</p>
     *
     * @return a {@link long} object.
     * @since 1.0.6
     */
    public long getExpirySeconds() {


        return this.expirySeconds;
    }

    /**
     * <p>Setter for the field <code>expirySeconds</code>.</p>
     *
     * @param expirySeconds a {@link java.lang.Long} object.
     * @since 1.0.6
     */
    public void setExpirySeconds(Long expirySeconds) {
        this.expirySeconds = expirySeconds;
    }


    /**
     * <p>Setter for the field <code>preferredServerName</code>.</p>
     *
     * @param preferredServerName a {@link java.lang.String} object.
     * @since 1.0.6
     */
    public void setPreferredServerName(final String preferredServerName) {

        this.preferredServerName = preferredServerName;

    }


    /**
     * <p>Getter for the field <code>preferredServerName</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     * @since 1.0.6
     */
    public String getPreferredServerName() {

        return this.preferredServerName;
    }

    /**
     * <p>Getter for the field <code>defaultPreferredServer</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     * @since 1.0.6
     */
    public String getDefaultPreferredServerName() {

        if (this.defaultPreferredServerName == null) {
            this.defaultPreferredServerName = IronConstants.DEFAULT_PREFERRED_SERVER_NAME;

        }
        return this.defaultPreferredServerName;
    }

    /**
     * <p>Setter for the field <code>defaultPreferredServerName</code>.</p>
     *
     * @param defaultPreferredServerName a {@link java.lang.String} object.
     * @since 1.0.6
     */
    public void setDefaultPreferredServerName(final String defaultPreferredServerName) {

        this.defaultPreferredServerName = defaultPreferredServerName;

    }


    private void logConfigurationWarning(Exception ex) {


        LOGGER.error("Check Configuration Settings - " + ex.getMessage());

    }


    // ...............................................  Descriptor .............................................

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String defaultPreferredServerName = IronConstants.DEFAULT_PREFERRED_SERVER_NAME;
        private String defaultProjectId = "";
        private String defaultToken = "";
        private String defaultQueueName = IronConstants.DEF_QUEUE_NAME;
        private Long defaultExpirySeconds = IronConstants.DEF_EXPIRY_SEC;


        public DescriptorImpl() {
            super(IronMQNotifier.class);
            load();
        }

        @Override
        public String getDisplayName() {

            return Messages.IronMQNotifier_DisplayName();

        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData)
                throws FormException {
            return super.newInstance(req, formData);
        }


        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDefaultPreferredServerName() {
            return defaultPreferredServerName;
        }

        public String getDefaultProjectId() {
            return defaultProjectId;
        }

        public String getDefaultToken() {
            return defaultToken;
        }

        public String getDefaultQueueName() {
            return defaultQueueName;
        }

        public Long getDefaultExpirySeconds() {
            return defaultExpirySeconds;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formdata) throws FormException {

            JSONObject json = formdata.getJSONObject("ironmqNotifier");

            defaultPreferredServerName = json.getString("defaultPreferredServerName");
            defaultProjectId = json.getString("defaultProjectId");
            defaultToken = json.getString("defaultToken");
            defaultQueueName = json.getString("defaultQueueName");
            try {
                defaultExpirySeconds = json.getLong("defaultExpirySeconds");
            } catch (Exception exception) {
                defaultExpirySeconds = IronConstants.DEF_EXPIRY_SEC;
            }

            save();
            return true;
        }


        public FormValidation doCheckQueueName(@QueryParameter final String value) {

            IronMQFormValidations validations = new IronMQFormValidations();

            hudson.util.FormValidation validationReturn;

            if (value == null) {
                validationReturn = validations.isValidQueueName("");
            } else {
                validationReturn = validations.isValidQueueName(value);
            }


            return validationReturn;

        }

        public FormValidation doCheckExpirySeconds(@QueryParameter final Long value) {

            IronMQFormValidations validations = new IronMQFormValidations();

            return validations.isValidExpirySeconds(value);

        }

    }


}



