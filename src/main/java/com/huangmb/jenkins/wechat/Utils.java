package com.huangmb.jenkins.wechat;

import com.huangmb.jenkins.wechat.bean.WechatDepartment;
import com.huangmb.jenkins.wechat.bean.WechatUser;
import hudson.model.User;
import hudson.util.ListBoxModel;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;

import java.io.File;
import java.io.PrintStream;
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
            applyUserId((JSONObject) users, list);
        } else if (users instanceof JSONArray) {
            JSONArray array = (JSONArray) users;
            for (int i = 0; i < array.size(); i++) {
                applyUserId(array.optJSONObject(i), list);
            }
        }

        return list;
    }

    private static void applyUserId(JSONObject user, List<String> users) {
        if (user == null) {
            return;
        }
        String uid = user.optString("user");
        if (!StringUtils.isBlank(uid) && !StringUtils.equals(uid, "-1") && !users.contains(uid)) {
            users.add(uid);
        }
    }

    public static ListBoxModel createUserItems() {
        return ContactsProvider.getInstance().createUserItems();
    }

    public static String getCurrentUserName() {
        String name = "匿名";
        User user = User.current();
        if (user != null) {
            name = user.getDisplayName();
        }
        return name;
    }

    public static String findFile(String scandir, String wildcard, Log logger) {
        if (StringUtils.isBlank(wildcard)) {
            logger.log("文件路径不能为空");
            return null;
        }
        final File dir = new File(scandir);
        if (!dir.exists() || !dir.isDirectory()) {
            logger.log("扫描文件夹:" + dir.getAbsolutePath());
            logger.log("扫描文件夹不存在或不是一个有效的目录!");
            return null;
        }

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(scandir);
        scanner.setIncludes(new String[]{wildcard});
        scanner.setCaseSensitive(true);
        scanner.scan();
        String[] uploadFiles = scanner.getIncludedFiles();

        if (uploadFiles == null || uploadFiles.length == 0)
            return null;
        if (uploadFiles.length == 1)
            return new File(dir, uploadFiles[0]).getAbsolutePath();

        List<String> strings = Arrays.asList(uploadFiles);
        Collections.sort(strings, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                File file1 = new File(dir, o1);
                File file2 = new File(dir, o2);
                if (file1.lastModified() < file2.lastModified())
                    return 1;
                if (file1.lastModified() > file2.lastModified())
                    return -1;
                return 0;
            }
        });
        String uploadFiltPath = new File(dir, strings.get(0)).getAbsolutePath();
        logger.log("发现 " + uploadFiles.length + " 个文件,选择最后修改的文件: " + uploadFiltPath);
        return uploadFiltPath;
    }

    public interface Log {
        void log(String msg);
    }
}
