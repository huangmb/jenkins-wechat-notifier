package com.huangmb.jenkins.wechat.chat;

import com.huangmb.jenkins.wechat.ContactsProvider;
import com.huangmb.jenkins.wechat.WeChatAPI;
import com.huangmb.jenkins.wechat.WechatAppConfiguration;
import com.huangmb.jenkins.wechat.WechatConfig;
import com.huangmb.jenkins.wechat.bean.Chat;
import hudson.Extension;
import hudson.security.Permission;
import hudson.util.FormApply;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bob.huang on 2018/7/21
 * 编辑群聊
 * 暂未支持修改
 */
@Extension
public class EditChat extends WechatConfig {

    private Chat chat;

    private String name;
    private String owner;
    private List<String> users = new ArrayList<>();

    public EditChat() {
    }

    public EditChat(Chat chat) {
        this.chat = chat;
        if (chat != null) {
            owner = chat.getOwner();
            name = chat.getName();
            users = chat.getUsers();
        }
    }

    @Override
    public String getDisplayName() {
        return "详情";
    }

    public String getName() {
        return name;
    }

    public String getChatId() {
        return this.chat.getChatId();
    }

    public List<String> getUsers() {
        return ContactsProvider.getInstance().getUserNames(users);
    }

    public String getOwner() {
        return ContactsProvider.getInstance().getUserName(owner);
    }

    @RequirePOST
    public HttpResponse doDelete(StaplerRequest req) throws ServletException {
        Jenkins.getInstance().checkPermission(Permission.WRITE);
        System.out.println("删除群聊" + name);
        List<Chat> chats = WechatAppConfiguration.get().getChats();
        chats.remove(chat);
        WechatAppConfiguration.get().setChats(chats);
        return FormApply.success("../");
    }

    @RequirePOST
    public HttpResponse doRefreshChat() {
        Jenkins.getInstance().checkPermission(Permission.WRITE);
        Chat newChat = WeChatAPI.getChat(this.chat.getChatId());
        if (newChat != null) {
            chat.setOwner(newChat.getOwner());
            chat.setName(newChat.getName());
            chat.setUsers(newChat.getUsers());
            WechatAppConfiguration.get().save();
        }
        return FormApply.success("../");
    }
}
