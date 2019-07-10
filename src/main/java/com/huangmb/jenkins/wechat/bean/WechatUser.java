package com.huangmb.jenkins.wechat.bean;

import org.kohsuke.stapler.DataBoundConstructor;

public class WechatUser {
    private String id;
    private String email;
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
    public WechatUser(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.setEmail(email);
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public boolean equals(Object target) {
        return this.id != null && ((WechatUser)target).getId() != null && this.id.equals(((WechatUser)target).getId());
    }
}
