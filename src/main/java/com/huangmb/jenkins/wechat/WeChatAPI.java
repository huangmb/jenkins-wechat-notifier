package com.huangmb.jenkins.wechat;


import com.huangmb.jenkins.wechat.bean.WechatDepartment;
import com.huangmb.jenkins.wechat.bean.WechatTag;
import com.huangmb.jenkins.wechat.bean.WechatUser;
import hudson.XmlFile;
import hudson.model.Items;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeChatAPI {
    private static final String GET = "GET";
    private static final String POST = "POST";

    private static final String ACCESS_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s";
    private static final String SEND_URL = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=%s";
    private static final String GET_DEPARTMENT_URL = "https://qyapi.weixin.qq.com/cgi-bin/department/list?access_token=%s";// + "&id=%s";
    private static final String GET_USER_BY_DEPARTMENT = "https://qyapi.weixin.qq.com/cgi-bin/user/simplelist?access_token=%s&department_id=%s&fetch_child=%d";
    private static final String GET_TAGS_URL = "https://qyapi.weixin.qq.com/cgi-bin/tag/list?access_token=%s";

    private static final String CONFIG_FILE = AccessToken.class.getName() + ".xml";
    private static AccessToken accessToken;

    static {
        Items.XSTREAM.registerConverter(new AccessTokenConverter());
    }
    /**
     * 发送文本消息
     *
     * @param user  用户ID，|分隔
     * @param party 部门id，|分隔
     * @param tag   标签id，|分隔
     * @param msg   消息内容
     */
    public static String sendMsg(String user, String party, String tag, String msg) {
        JSONObject params = new JSONObject();
        params.put("msgtype", "text");
        params.put("agentid", WechatAppConfiguration.get().getAgentId());
        JSONObject content = new JSONObject();
        content.put("content", msg);
        params.put("text", content);
        if (StringUtils.isNotBlank(user)) {
            params.put("touser", user);
        }
        if (StringUtils.isNotBlank(party)) {
            params.put("toparty", party);
        }
        if (StringUtils.isNotBlank(tag)) {
            params.put("totag", tag);
        }

        AccessToken accessToken = getAccessToken();
        String query = String.format(SEND_URL, accessToken.getToken());
        return post(query, params.toString());
    }

    /**
     * 获取部门
     *
     * @return
     */
    public static List<WechatDepartment> getDepartments() {
        List<WechatDepartment> list = new ArrayList<>();
        try {
            AccessToken token = getAccessToken();
            String query = String.format(GET_DEPARTMENT_URL, token.getToken());
            String resp = get(query);
            JSONObject obj = JSONObject.fromObject(resp);
            if (obj.getInt("errcode") == 0) {
                JSONArray departments = obj.getJSONArray("department");
                int size = departments.size();
                for (int i = 0; i < size; i++) {
                    JSONObject department = departments.getJSONObject(i);
                    String id = department.getString("id");
                    String name = department.getString("name");
                    String pid = department.getString("parentid");
                    WechatDepartment model = new WechatDepartment();
                    model.setId(id);
                    model.setName(name);
                    model.setParentId(pid);
                    list.add(model);
                }
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取所有成员
     *
     * @return
     */
    public static Map<String,List<WechatUser>> getAllUsers(List<WechatDepartment> departments) {
        List<WechatDepartment> rootDepartments = Utils.getRootDepartments(departments);
        Map<String,List<WechatUser>> map = new HashMap<>();

        try {
            AccessToken token = getAccessToken();
            for (WechatDepartment department : rootDepartments) {
                String query = String.format(GET_USER_BY_DEPARTMENT, token.getToken(), department.getId(), 1);
                String resp = get(query);
                JSONObject obj = JSONObject.fromObject(resp);
                if (obj.getInt("errcode") == 0) {
                    JSONArray userList = obj.getJSONArray("userlist");
                    int size = userList.size();
                    List<WechatUser> list = new ArrayList<>();
                    map.put(department.getId(),list);
                    for (int i = 0; i < size; i++) {
                        JSONObject user = userList.getJSONObject(i);
                        String userId = user.getString("userid");
                        String name = user.getString("name");
                        WechatUser u = new WechatUser();
                        u.setId(userId);
                        u.setName(name);
                        list.add(u);
                    }
                }
            }
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<WechatTag> getAllTags() {

        try {
            AccessToken accessToken = getAccessToken();
            String query = String.format(GET_TAGS_URL, accessToken.getToken());
            String resp = get(query);
            JSONObject obj = JSONObject.fromObject(resp);
            if (obj.getInt("errcode") == 0) {
                JSONArray tagList = obj.getJSONArray("taglist");
                int size = tagList.size();
                List<WechatTag> list = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    JSONObject tag = tagList.getJSONObject(i);
                    String tagId = tag.getString("tagid");
                    String name = tag.getString("tagname");
                    WechatTag model = new WechatTag();
                    model.setId(tagId);
                    model.setName(name);
                    list.add(model);
                }
                return list;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static AccessToken fetchAccessToken(String corpId, String secret) {
        if (StringUtils.isBlank(corpId) || StringUtils.isBlank(secret)) {
            throw new NullPointerException("请先设置企业ID和应用密钥");
        }
        try {
            String request = String.format(ACCESS_TOKEN_URL, corpId.trim(), secret.trim());
            String response = get(request);
            JSONObject obj = JSONObject.fromObject(response);
            AccessToken token = new AccessToken();
            token.setToken(obj.getString("access_token"));
            int expires_in = obj.getInt("expires_in");
            token.setExpiresIn((System.currentTimeMillis() / 1000 + expires_in) + "");
            return token;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("获取微信Token失败");
        }
    }

    private static String get(String path) {
        try {
//            System.out.println(path);
            URL url = new URL(path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(10 * 1000);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                return IOUtils.toString(inputStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String post(String path, String params) {
        try {
//            System.out.println(path);
            URL url = new URL(path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            OutputStream outputStream = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(outputStream);
            writer.print(params);
            writer.flush();
            writer.close();
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(10 * 1000);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                return IOUtils.toString(inputStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void invalidateToken() {
        accessToken = null;
        XmlFile xmlFile = new XmlFile(new File(Jenkins.getInstance().getRootDir(), CONFIG_FILE));
        if (xmlFile.exists()) {
            xmlFile.delete();
        }
    }
    private static AccessToken getAccessToken() {
        if (accessToken == null) {
            loadAccessToken();
        }
        if (accessToken == null || !accessToken.isValid()) {
            WechatAppConfiguration configuration = WechatAppConfiguration.get();
            accessToken = fetchAccessToken(configuration.getCorpId(), configuration.getSecret());
            saveAccessToken();
        }
        return accessToken;
    }
    private static void loadAccessToken() {
        try {
            accessToken = new AccessToken();
            XmlFile xmlFile = new XmlFile(new File(Jenkins.getInstance().getRootDir(), CONFIG_FILE));
            if (xmlFile.exists()) {
                xmlFile.unmarshal(accessToken);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void saveAccessToken() {
        try {
            XmlFile xmlFile = new XmlFile(new File(Jenkins.getInstance().getRootDir(), CONFIG_FILE));
            xmlFile.write(accessToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
