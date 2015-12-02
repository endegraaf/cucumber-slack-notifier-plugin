package org.jenkinsci.plugins.slacknotifier;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class CucumberSlackBuildStepNotifier extends Builder {

	private static final Logger LOG = Logger.getLogger(CucumberSlackBuildStepNotifier.class.getName());

	private final String channel;

	@DataBoundConstructor
	public CucumberSlackBuildStepNotifier(String channel) {
		this.channel = channel;
	}

	public String getChannel() {
		return channel;
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
		String webhookUrl = ((CucumberSlackBuildStepNotifier.DescriptorImpl) Jenkins.getInstance().getDescriptor(
				CucumberSlackBuildStepNotifier.class)).getWebHookEndpoint();
		String jenkinsUrl = ((CucumberSlackBuildStepNotifier.DescriptorImpl) Jenkins.getInstance().getDescriptor(
				CucumberSlackBuildStepNotifier.class)).getJenkinsServerUrl();

		LOG.info("Posting cucumber reports to slack for '" + build.getParent().getDisplayName() + "'");

		SlackClient client = new SlackClient(webhookUrl, jenkinsUrl, channel);
		client.postToSlack(null, build.getParent().getDisplayName(), build.getNumber());

		listener.getLogger().printf("message posted to slack");
		return true;
	}

	public String escape(String string) {
		string = string.replace("&", "&amp;");
		string = string.replace("<", "&lt;");
		string = string.replace(">", "&gt;");

		return string;
	}

	// Overridden for better type safety.
	// If your plugin doesn't really define any property on Descriptor,
	// you don't have to do this.
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	/**
	 * Descriptor for {@link SlackBuildStepNotifier}. Used as a singleton. The
	 * class is marked as public so that it can be accessed from views.
	 *
	 * <p>
	 * See
	 * <tt>src/main/resources/jenkinsci/plugins/stepbuildinfo/SlackBuildStepNotifier/*.jelly</tt>
	 * for the actual HTML fragment for the configuration screen.
	 */
	@Extension
	// This indicates to Jenkins that this is an implementation of an extension
	// point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		/**
		 * To persist global configuration information, simply store it in a
		 * field and call save().
		 *
		 * <p>
		 * If you don't want fields to be persisted, use <tt>transient</tt>.
		 */
		private String webHookEndpoint;
		private String jenkinsServerUrl;

		/**
		 * In order to load the persisted global configuration, you have to call
		 * load() in the constructor.
		 */
		public DescriptorImpl() {
			load();
		}

		/**
		 * Performs on-the-fly validation of the form field 'name'.
		 *
		 * @param value
		 *            This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the
		 *         browser.
		 *         <p>
		 *         Note that returning {@link FormValidation#error(String)} does
		 *         not prevent the form from being saved. It just means that a
		 *         message will be displayed to the user.
		 */
		public FormValidation doCheckName(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please set a name");
			if (value.length() < 4)
				return FormValidation.warning("Isn't the name too short?");
			return FormValidation.ok();
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			// Indicates that this builder can be used with all kinds of project
			// types
			return true;
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return "Cucumber Report Slack Build Step Notifier";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			// To persist global configuration information,
			// set that to properties and call save().
			webHookEndpoint = formData.getString("webHookEndpoint");
			jenkinsServerUrl = formData.getString("jenkinsServerUrl");
			// ^Can also use req.bindJSON(this, formData);
			// (easier when there are many fields; need set* methods for this,
			// like setUseFrench)
			save();
			return super.configure(req, formData);
		}

		/**
		 * This method returns true if the global configuration says we should
		 * speak French.
		 *
		 * The method name is bit awkward because global.jelly calls this method
		 * to determine the initial state of the checkbox by the naming
		 * convention.
		 */
		public String getWebHookEndpoint() {
			return webHookEndpoint;
		}

		public String getJenkinsServerUrl() {
			return jenkinsServerUrl;
		}
	}
}
