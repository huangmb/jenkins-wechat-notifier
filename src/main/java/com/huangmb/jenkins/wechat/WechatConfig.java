package com.huangmb.jenkins.wechat;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Action;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormApply;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.util.Collections;
import java.util.List;

/**
 * Created by bob.huang on 2018/7/15
 */
public abstract class WechatConfig implements ExtensionPoint, Action, Describable<WechatConfig> {
    private String errorMsg;

    @Override
    public String getIconFileName() {
        return "gear.png";
    }


    @Override
    public String getUrlName() {
        return getClass().getSimpleName();
    }

    @Override
    public WechatConfigDescriptor getDescriptor() {
        return (WechatConfigDescriptor) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    protected HttpResponse failOnCheckParameters(StaplerRequest req, String reason) {
        if (FormApply.isApply(req)) {
            return FormApply.applyResponse("notificationBar.show('" + reason + "',notificationBar.ERROR)");
        }
        errorMsg = reason;
        return FormApply.success("./");
    }

    public abstract static class WechatConfigDescriptor extends Descriptor<WechatConfig> {
        public WechatConfigDescriptor(Class<? extends WechatConfig> clazz) {
            super(clazz);
        }

        public WechatConfigDescriptor() {

        }
    }


    public static ExtensionList<WechatConfig> all() {
        return Jenkins.getInstance().getExtensionList(WechatConfig.class);
    }

}
