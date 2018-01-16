package com.huangmb.jenkins.wechat;

import com.huangmb.jenkins.wechat.bean.CustomGroup;
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

    public List<CustomGroup> getCustomGroups(){
        List<CustomGroup> groups = WechatAppConfiguration.get().getCustomGroups();
        return groups == null ? new ArrayList<CustomGroup>() : groups;
    }

    public void invalidateCache() {
        users = null;
        departmentAndUserMap = null;
        departments = null;
        tags = null;

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
            for (List<WechatUser> userList : departmentAndUserMap.values()) {
                users.addAll(userList);
            }
            Collections.sort(users,new Comparator<WechatUser>() {
                @Override
                public int compare(WechatUser u1, WechatUser u2) {
                    return u1.getName().compareTo(u2.getName());
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
