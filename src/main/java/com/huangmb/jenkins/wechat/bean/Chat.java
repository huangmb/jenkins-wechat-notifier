package com.huangmb.jenkins.wechat.bean;

import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bob.huang on 2018/7/9
 */
public class Chat {
    private String chatId;
    private String name;
    private String owner;
    private List<String> users = new ArrayList<>();

    public Chat() {
    }

    @DataBoundConstructor
    public Chat(String chatId, String name, String owner, List<String> users) {
        this.chatId = chatId;
        this.name = name;
        this.owner = owner;
        this.users = users;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }
}
