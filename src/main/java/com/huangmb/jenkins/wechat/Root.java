package com.huangmb.jenkins.wechat;

import com.huangmb.jenkins.wechat.chat.ChatConfig;
import com.huangmb.jenkins.wechat.group.CustomGroupConfig;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.RootAction;
import jenkins.model.Jenkins;
import jenkins.model.ModelObjectWithContextMenu;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by bob.huang on 2018/7/15
 * 微信群聊入口
 */
@Extension
public class Root implements RootAction,ModelObjectWithContextMenu {
    @Override
    public String getIconFileName() {
        return "gear.png";
    }

    @Override
    public String getDisplayName() {
        return "企业微信";
    }

    @Override
    public String getUrlName() {
        return "wechat";
    }

    public WechatConfig getDynamic(String name) {
        for (WechatConfig config : getAll())
            if (config.getUrlName().equals(name))
                return config;
        return null;
    }

    public List<WechatConfig> getAll() {
        ExtensionList<WechatConfig> list = Jenkins.getInstance().getExtensionList(WechatConfig.class);
        CustomGroupConfig groupConfig = list.get(CustomGroupConfig.class);
        ChatConfig chatConfig = list.get(ChatConfig.class);
        return Arrays.asList(groupConfig, chatConfig);
    }

    @Override
    public ContextMenu doContextMenu(StaplerRequest staplerRequest, StaplerResponse staplerResponse) throws Exception {
        return new ContextMenu().addAll(getAll());
    }
}
