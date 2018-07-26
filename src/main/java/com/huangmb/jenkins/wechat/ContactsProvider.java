package com.huangmb.jenkins.wechat;

import com.huangmb.jenkins.wechat.bean.CustomGroup;
import com.huangmb.jenkins.wechat.bean.Chat;
import com.huangmb.jenkins.wechat.bean.WechatDepartment;
import com.huangmb.jenkins.wechat.bean.WechatTag;
import com.huangmb.jenkins.wechat.bean.WechatUser;
import hudson.util.ListBoxModel;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class ContactsProvider {
    private List<WechatUser> users;
    private List<WechatDepartment> departments;
    private List<WechatTag> tags;
    //部门-部门人员
    private Map<String, List<WechatUser>> departmentAndUserMap;
    private Map<String, String> userIdAndNameMap;

    private ListBoxModel userModel;

    private static class ContactsProviderHolder {
        private static ContactsProvider INSTANCE = new ContactsProvider();
    }

    private ContactsProvider() {
    }

    public static ContactsProvider getInstance() {
        return ContactsProviderHolder.INSTANCE;
    }

    public List<WechatDepartment> getDepartments() {
        loadUserAndDepartmentListIfNeeded();
        return departments == null ? new ArrayList<WechatDepartment>() : departments;
    }

    public List<WechatTag> getTags() {
        loadTagIfNeeded();
        return tags == null ? new ArrayList<WechatTag>() : tags;
    }

    public List<WechatUser> getAllUsers() {
        loadUserAndDepartmentListIfNeeded();
        return users == null ? new ArrayList<WechatUser>() : users;
    }

    public ListBoxModel createUserItems() {
        if (userModel == null) {
            userModel = new ListBoxModel();
            userModel.add("选择一个用户", "-1");

            for (WechatUser user : getAllUsers()) {
                userModel.add(user.getName(), user.getId());
            }
        }
        return userModel;
    }

    public List<String> getUserNames(List<String> ids) {
        loadUserAndDepartmentListIfNeeded();
        List<String> names = new ArrayList<>();
        for (String id : ids) {
            String name = userIdAndNameMap.get(id);
            if (name == null) {
                name = id;
            }
            names.add(name);
        }
        return names;
    }

    public String getUserName(String id) {
        loadUserAndDepartmentListIfNeeded();
        String name = userIdAndNameMap.get(id);
        if (name == null) {
            name = id;
        }
        return name;
    }

    public List<CustomGroup> getCustomGroups() {
        List<CustomGroup> groups = WechatAppConfiguration.get().getGroups();
        return groups == null ? new ArrayList<CustomGroup>() : groups;
    }

    public List<Chat> getChats() {
        List<Chat> chats = WechatAppConfiguration.get().getChats();
        return chats == null ? new ArrayList<Chat>() : chats;
    }


    public void invalidateCache() {
        users = null;
        departmentAndUserMap = null;
        userIdAndNameMap = null;
        departments = null;
        tags = null;
        userModel = null;
    }

    private void loadUserAndDepartmentListIfNeeded() {
        if (departmentAndUserMap == null) {
            if (departments == null) {
                departments = WeChatAPI.getDepartments();
            }
            if (departments == null) {
                return;
            }
            departmentAndUserMap = WeChatAPI.getAllUsers(departments);
            for (WechatDepartment department : departments) {
                String displayName = department.getName();
                List<WechatUser> list = departmentAndUserMap.get(department.getId());
                if (list != null && !list.isEmpty()) {
                    int n = Math.min(list.size(), 3);
                    List<String> names = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        names.add(list.get(i).getName());
                    }
                    displayName += "(包括 " + StringUtils.join(names, ",") + " 等人）";
                }
                department.setDisplayName(displayName);
            }
        }
        if (users == null && departmentAndUserMap != null) {
            users = new ArrayList<>();
            userIdAndNameMap = new HashMap<>();
            for (List<WechatUser> userList : departmentAndUserMap.values()) {
                users.addAll(userList);
            }
            for (WechatUser user : users) {
                userIdAndNameMap.put(user.getId(), user.getName());
            }
            Collections.sort(users, new Comparator<WechatUser>() {
                @Override
                public int compare(WechatUser u1, WechatUser u2) {
                    return u1.getName().compareToIgnoreCase(u2.getName());
                }
            });


        }
    }

    private void loadTagIfNeeded() {
        if (tags == null) {
            tags = WeChatAPI.getAllTags();
        }
    }

}
