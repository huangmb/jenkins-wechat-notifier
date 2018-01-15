package com.huangmb.jenkins.wechat;

import com.huangmb.jenkins.wechat.bean.WechatDepartment;

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
}
