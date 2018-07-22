package com.huangmb.jenkins.wechat.group;

import com.huangmb.jenkins.wechat.ContactsProvider;
import com.huangmb.jenkins.wechat.Utils;
import com.huangmb.jenkins.wechat.WechatAppConfiguration;
import com.huangmb.jenkins.wechat.WechatConfig;
import com.huangmb.jenkins.wechat.bean.CustomGroup;
import com.huangmb.jenkins.wechat.bean.WechatUser;
import com.huangmb.jenkins.wechat.chat.CreateChat;
import hudson.Extension;
import hudson.model.User;
import hudson.security.Permission;
import hudson.util.FormApply;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by bob.huang on 2018/7/22
 */
@Extension
public class EditGroup extends WechatConfig {
    private static final Logger LOGGER = Logger.getLogger(EditGroup.class.getSimpleName());

    private CustomGroup group;
    private String name;
    private List<String> users;

    public EditGroup() {
    }

    public void setGroup(CustomGroup group) {
        if (group != null) {
            name = group.getName();
            users = group.getUsers();
            this.group = group;
        }


    }

    @Override
    public String getDisplayName() {
        return "详情";
    }

    public String getName() {
        return name;
    }

    @DataBoundSetter
    public void setName(String name) {
        this.name = name;
    }

    public List<String> getUserNames() {
        return ContactsProvider.getInstance().getUserNames(users);
    }

    public List<String> getUsers() {
        return users;
    }

    @DataBoundSetter
    public void setUsers(List<String> users) {
        this.users = users;
    }

    @RequirePOST
    public HttpResponse doConfigSubmit(StaplerRequest req) throws ServletException {
        Jenkins.getInstance().checkPermission(Permission.WRITE);
        setErrorMsg(null);
        JSONObject form = req.getSubmittedForm();
        this.name = form.optString("name");
        this.users = Utils.getUsersFromReq(form);
        if (StringUtils.isBlank(name) || users.isEmpty()) {
            return failOnCheckParameters(req, "分组名或成员不可为空");
        }
        group.setName(name);
        group.setUsers(users);
        WechatAppConfiguration.get().save();
        group = null;

        return FormApply.success("../");
    }

    @Override
    protected HttpResponse failOnCheckParameters(StaplerRequest req, String reason) {
        HttpResponse response = super.failOnCheckParameters(req, reason);
        if (!FormApply.isApply(req)) {
            return FormApply.success("./config");
        }
        return response;
    }

    @RequirePOST
    public HttpResponse doDelete(StaplerRequest req) throws ServletException {
        Jenkins.getInstance().checkPermission(Permission.WRITE);

        LOGGER.warning(String.format("用户 %s 删除了群组 %s", Utils.getCurrentUserName(), name));

        WechatAppConfiguration config = WechatAppConfiguration.get();
        List<CustomGroup> groups = config.getGroups();
        groups.remove(group);
        config.setGroups(groups);

        return FormApply.success("../");
    }

    @Extension
    public static class DescriptorImpl extends WechatConfigDescriptor {

        public ListBoxModel doFillUserItems() {
            return Utils.createUserItems();
        }
    }
}
