package com.huangmb.jenkins.wechat.chat;

import com.huangmb.jenkins.wechat.Utils;
import com.huangmb.jenkins.wechat.WeChatAPI;
import com.huangmb.jenkins.wechat.WechatAppConfiguration;
import com.huangmb.jenkins.wechat.WechatConfig;
import com.huangmb.jenkins.wechat.bean.Chat;
import hudson.Extension;
import hudson.security.Permission;
import hudson.util.FormApply;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by bob.huang on 2018/7/16
 */
@Extension
public class CreateChat extends WechatConfig {
    private static final Logger LOGGER = Logger.getLogger(CreateChat.class.getName());
    protected String name;
    protected List<String> users = new ArrayList<>();


    @Override
    public String getDisplayName() {
        return "新建";
    }

    @Override
    public String getUrlName() {
        return "create";
    }

    @RequirePOST
    public HttpResponse doConfigSubmit(StaplerRequest req) throws ServletException {
        Jenkins.getInstance().checkPermission(Permission.WRITE);
        setErrorMsg(null);//重置错误提示

        LOGGER.info("正在新建群聊");
        JSONObject form = req.getSubmittedForm();
        bindForm(form);
        String sayHello = form.optString("sayHello");
        if (StringUtils.isBlank(name)) {
            return failOnCheckParameters(req, "群名称不能为空");
        }
        if (users.size() < 2) {
            return failOnCheckParameters(req, "群成员不得少于2人");
        }
        LOGGER.info("群名称 " + name);
        LOGGER.info("群成员 " + users);

        if (!FormApply.isApply(req)) {

            String owner = users.get(0);
            String chatId = WeChatAPI.createChat(name, users, owner);
            if (StringUtils.isEmpty(chatId)) {
                return failOnCheckParameters(req, "创建失败");
            }
            Chat chat = new Chat(chatId,name,owner, users);
            WechatAppConfiguration.get().addChat(chat);
            if (StringUtils.isNotBlank(sayHello)) {
                WeChatAPI.sendChatMsg(chatId,sayHello);
            }
            //成功提交表单后情况
            reset();
        }
        return FormApply.success("../");
    }

    @RequirePOST
    public HttpResponse doCreateFromId(StaplerRequest req) throws ServletException {
        Jenkins.getInstance().checkPermission(Permission.WRITE);
        setErrorMsg(null);//重置错误提示
        String chatId = req.getSubmittedForm().optString("chatId");
        if (StringUtils.isBlank(chatId)) {
            return failOnCheckParameters(req, "群ID为空");
        }
        List<Chat> chats = WechatAppConfiguration.get().getChats();
        for (Chat chat : chats) {
            if (StringUtils.equals(chatId,chat.getChatId())) {
                return failOnCheckParameters(req, "已在群列表中");
            }
        }
        Chat chat = WeChatAPI.getChat(chatId);
        if (chat == null) {
            return failOnCheckParameters(req,"群ID无效");
        }
        WechatAppConfiguration.get().addChat(chat);
        reset();
        return FormApply.success("../");
    }

    protected void bindForm(JSONObject form) {
        name = form.optString("name");
        users = Utils.getUsersFromReq(form);
    }

    protected void reset() {
        name = null;
        users = new ArrayList<>();
    }

    public String getName() {
        return name;
    }


    @DataBoundSetter
    public void setName(String name) {
        this.name = name;
    }

    public List<String> getUsers() {
        return users;
    }

    @DataBoundSetter
    public void setUsers(List<String> users) {
        this.users = users;
    }

    @Extension
    public static class DescriptorImpl extends WechatConfigDescriptor {

        public ListBoxModel doFillUserItems() {
           return Utils.createUserItems();
        }

        public FormValidation doCheckName(@QueryParameter String name) {
            return checkNotNull(name);
        }

        public FormValidation doCheckChatId(@QueryParameter String chatId) {
            List<Chat> chats = WechatAppConfiguration.get().getChats();
            for (Chat chat : chats) {
                if (StringUtils.equals(chatId,chat.getChatId())) {
                    return FormValidation.error("该群已在列表中");
                }
            }
            return FormValidation.ok();
        }


        private FormValidation checkNotNull(String value) {
            if (StringUtils.isEmpty(value)) {
                return FormValidation.error("不能为空");
            }
            return FormValidation.ok();
        }

    }

}
