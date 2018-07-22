package com.huangmb.jenkins.wechat;

import com.huangmb.jenkins.wechat.bean.Chat;
import com.huangmb.jenkins.wechat.bean.CustomGroup;
import hudson.Extension;
import hudson.model.RootAction;
import hudson.security.Permission;
import hudson.util.FormApply;
import jenkins.model.Jenkins;
import jenkins.model.ModelObjectWithContextMenu;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
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

    @RequirePOST
    public HttpResponse doDeleteChat(StaplerRequest req) throws ServletException {
        Jenkins.getInstance().checkPermission(Permission.WRITE);
        System.out.println("删除群聊");
        return FormApply.success("./");
    }

    public List<Chat> getChats() {
        return WechatAppConfiguration.get().getChats();
    }

    public List<CustomGroup> getCustomGroups() {
        return WechatAppConfiguration.get().getGroups();
    }

    public List<WechatConfig> getAll() {
        return WechatConfig.all();
    }

    @Override
    public ContextMenu doContextMenu(StaplerRequest staplerRequest, StaplerResponse staplerResponse) throws Exception {
        return new ContextMenu().addAll(getAll());
    }
}
