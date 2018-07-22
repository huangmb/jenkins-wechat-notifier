package com.huangmb.jenkins.wechat;

import com.huangmb.jenkins.wechat.bean.WechatDepartment;
import com.huangmb.jenkins.wechat.bean.WechatUser;
import hudson.model.User;
import hudson.util.ListBoxModel;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class Utils {
    public static List<WechatDepartment> getRootDepartments(List<WechatDepartment> departments) {
        Set<String> all = new HashSet<>();
        for (WechatDepartment department : departments) {
            all.add(department.getId());
        }
        List<WechatDepartment> root = new ArrayList<>();
        for (WechatDepartment department : departments) {
            if (!all.contains(department.getParentId())) {
                root.add(department);
            }
        }
        return root;
    }

    public static List<String> getUsersFromReq(JSONObject form) {
        List<String> list = new ArrayList<>();
        Object users = form.opt("users");
        if (users == null) {
            return list;
        }
        if (users instanceof JSONObject) {
            applyUserId((JSONObject) users,list);
        } else if (users instanceof JSONArray) {
            JSONArray array = (JSONArray) users;
            for (int i = 0; i < array.size(); i++) {
               applyUserId(array.optJSONObject(i),list);
            }
        }

        return list;
    }

    private static void applyUserId(JSONObject user,List<String> users) {
        if (user == null) {
            return;
        }
        String uid = user.optString("user");
        if(!StringUtils.isBlank(uid) && !StringUtils.equals(uid, "-1") && !users.contains(uid)) {
            users.add(uid);
        }
    }

    public static ListBoxModel createUserItems() {
        List<WechatUser> users = ContactsProvider.getInstance().getAllUsers();
        ListBoxModel model = new ListBoxModel();
        model.add("选择一个用户", "-1");
        for (WechatUser user : users) {
            model.add(user.getName(), user.getId());
        }
        return model;
    }

    public static String getCurrentUserName() {
        String name = "匿名";
        User user = User.current();
        if (user != null) {
            name = user.getDisplayName();
        }
        return name;
    }

}
