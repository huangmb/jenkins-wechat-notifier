package com.huangmb.jenkins.wechat.bean;

import org.kohsuke.stapler.DataBoundConstructor;

public class Receiver {
    private String type;
    private String id;

    @DataBoundConstructor
    public Receiver(String type, String id) {
        this.type = type;
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
