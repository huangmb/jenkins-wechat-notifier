package com.huangmb.jenkins.wechat.chat;

import com.huangmb.jenkins.wechat.WechatAppConfiguration;
import com.huangmb.jenkins.wechat.WechatConfig;
import com.huangmb.jenkins.wechat.bean.Chat;
import hudson.Extension;
import hudson.ExtensionList;
import jenkins.model.Jenkins;
import jenkins.model.ModelObjectWithContextMenu;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.util.List;

/**
 * Created by bob.huang on 2018/7/21
 */
@Extension
public class ChatConfig  extends WechatConfig implements ModelObjectWithContextMenu {
    @Override
    public String getDisplayName() {
        return "群聊";
    }

    @Override
    public String getUrlName() {
        return "chat";
    }

    public WechatConfig getDynamic(String name) {
        CreateChat createChat = getCreateChat();
        if (createChat.getUrlName().equals(name)) {
            return createChat;
        }
        try {
            int index = Integer.parseInt(name);
            List<Chat> chats = getChats();
            if (index >= 0 && index < chats.size()) {
                return new EditChat(chats.get(index));
            }
        } catch (Exception e) {

        }
        return null;
    }

    public List<Chat> getChats() {
        return WechatAppConfiguration.get().getChats();
    }

    private CreateChat getCreateChat() {
        return Jenkins.getInstance().getExtensionList(CreateChat.class).get(CreateChat.class);
    }

    @Override
    public ContextMenu doContextMenu(StaplerRequest request, StaplerResponse response) throws Exception {
        return new ContextMenu().add(getCreateChat());
    }

    @Extension
    public static final class DescriptorImpl extends WechatConfigDescriptor {

    }
}
