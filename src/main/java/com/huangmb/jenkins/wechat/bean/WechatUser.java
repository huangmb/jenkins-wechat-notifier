package com.huangmb.jenkins.wechat.bean;

import org.kohsuke.stapler.DataBoundConstructor;

public class WechatUser {
    private String id;
    private String name;

    public WechatUser() {
    }

    @DataBoundConstructor
    public WechatUser(String idAndName) {
        String[] pair = idAndName.split("@");
        if (pair.length == 2) {
            id = pair[0];
            name = pair[1];
        } else {
            id = idAndName;
        }
    }
    public WechatUser(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
