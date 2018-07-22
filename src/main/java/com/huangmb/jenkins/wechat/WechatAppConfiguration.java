package com.huangmb.jenkins.wechat;

import com.huangmb.jenkins.wechat.bean.CustomGroup;
import com.huangmb.jenkins.wechat.bean.Chat;
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
    private List<Chat> chats = new ArrayList<>();
    private List<CustomGroup> groups = new ArrayList<>();

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

    public List<CustomGroup> getGroups() {
        return groups;
    }

    @DataBoundSetter
    public void setGroups(List<CustomGroup> groups) {
        this.groups = groups;
        save();
    }

    public List<Chat> getChats() {
        return chats;
    }

    @DataBoundSetter
    public void setChats(List<Chat> chats) {
        this.chats = chats;
        save();
    }

    public void addChat(Chat chat) {
        if (chats == null) {
            chats = new ArrayList<>();
        }
        chats.add(chat);
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
        return Utils.createUserItems();
    }

    public ListBoxModel doFillChatUserItems() {
       return doFillIdAndNameItems();
    }

    public FormValidation doRefreshContacts(@QueryParameter String corpId, @QueryParameter String secret) {
        System.out.println("重置缓存");
        try {
            WeChatAPI.fetchAccessToken(corpId,secret);
            ContactsProvider.getInstance().getAllUsers();
            return FormValidation.ok("刷新完成");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return FormValidation.error("企业id或应用密钥不正确");
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
