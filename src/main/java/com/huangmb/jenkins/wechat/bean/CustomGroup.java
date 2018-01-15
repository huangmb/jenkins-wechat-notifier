package com.huangmb.jenkins.wechat.bean;

import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;
import java.util.List;

public class CustomGroup {
    private String name;
    private List<WechatUser> users = new ArrayList<>();

    public CustomGroup() {
    }

    @DataBoundConstructor
    public CustomGroup(String name, List<WechatUser> users) {
        this.name = name;
        this.users = users;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<WechatUser> getUsers() {
        return users;
    }

    public void setUsers(List<WechatUser> users) {
        this.users = users;
    }
}
