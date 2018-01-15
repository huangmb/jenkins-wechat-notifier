package com.huangmb.jenkins.wechat;

import com.huangmb.jenkins.wechat.bean.CustomGroup;
import com.huangmb.jenkins.wechat.bean.WechatUser;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置企业微信消息接口必要的参数
 */
@Extension
public class WechatAppConfiguration extends GlobalConfiguration {
    /**
     * @return the singleton instance
     */
    public static WechatAppConfiguration get() {
        return GlobalConfiguration.all().get(WechatAppConfiguration.class);
    }

    private String corpId;
    private String agentId;
    private String secret;
    private List<CustomGroup> customGroups = new ArrayList<>();

    public WechatAppConfiguration() {
        // When Jenkins is restarted, load any saved configuration from disk.
        load();
    }

    public String getCorpId() {
        return corpId;
    }

    @DataBoundSetter
    public void setCorpId(String corpId) {
        this.corpId = corpId;
        save();
    }

    public String getAgentId() {
        return agentId;
    }

    @DataBoundSetter
    public void setAgentId(String agentId) {
        this.agentId = agentId;
        save();
    }

    public String getSecret() {
        return secret;
    }

    public List<CustomGroup> getCustomGroups() {
        return customGroups;
    }

    @DataBoundSetter
    public void setCustomGroups(List<CustomGroup> customGroups) {
        this.customGroups = customGroups;
        save();
    }

    @DataBoundSetter
    public void setSecret(String secret) {
        this.secret = secret;
        save();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        String oldCorpId = corpId;
        String oldSecret = secret;
        req.bindJSON(this, json);
        if (!StringUtils.equals(oldCorpId,corpId) || !StringUtils.equals(oldSecret,secret)) {
            invalidateToken();
        }
        save();
        return super.configure(req, json);
    }

    private void invalidateToken() {
        WeChatAPI.invalidateToken();
        ContactsProvider.getInstance().invalidateCache();
    }

    public ListBoxModel doFillIdAndNameItems() {
        List<WechatUser> users = ContactsProvider.getInstance().getAllUsers();
        ListBoxModel model = new ListBoxModel();
        model.add("选择一个用户","-1");
        for (WechatUser user : users) {
            model.add(user.getName(),user.getId()+ "@"+user.getName());
        }
        return model;
    }

    public FormValidation doCheckCorpId(@QueryParameter String value) {
        return checkNotNull(value);
    }

    public FormValidation doCheckAgentId(@QueryParameter String value) {
        return checkNotNull(value);
    }

    public FormValidation doCheckSecret(@QueryParameter String value) {
        return checkNotNull(value);
    }

    private FormValidation checkNotNull(String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("不能为空");
        }
        return FormValidation.ok();
    }

}
