package com.huangmb.jenkins.wechat;

import com.huangmb.jenkins.wechat.bean.Chat;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class WeChatAPI {
    private static final Logger LOGGER = Logger.getLogger(WeChatAPI.class.getName());
    private static final String GET = "GET";
    private static final String POST = "POST";

    //获取token
    private static final String ACCESS_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s";
    //推送消息
    private static final String SEND_URL = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=%s";
    //获取部门
    private static final String GET_DEPARTMENT_URL = "https://qyapi.weixin.qq.com/cgi-bin/department/list?access_token=%s";// + "&id=%s";
    //获取部门成员
    private static final String GET_USER_BY_DEPARTMENT = "https://qyapi.weixin.qq.com/cgi-bin/user/simplelist?access_token=%s&department_id=%s&fetch_child=%d";
    //获取标签
    private static final String GET_TAGS_URL = "https://qyapi.weixin.qq.com/cgi-bin/tag/list?access_token=%s";
    //创建群聊
    private static final String CREATE_CHAT = "https://qyapi.weixin.qq.com/cgi-bin/appchat/create?access_token=%s";
    //推送群聊消息
    private static final String SEND_CHAT = "https://qyapi.weixin.qq.com/cgi-bin/appchat/send?access_token=%s";
    //获取群聊信息
    private static final String GET_CHAT = "https://qyapi.weixin.qq.com/cgi-bin/appchat/get?access_token=%s&chatid=%s";
    //更新群聊信息
    private static final String UPDATE_CHAT= "https://qyapi.weixin.qq.com/cgi-bin/appchat/update?access_token=%s";

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
            LOGGER.log(Level.WARNING, "error in get dep req", e);
        }
        return null;
    }

    /**
     * 获取所有成员
     *
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
            LOGGER.log(Level.WARNING, "error in get user req", e);
        }
        return null;
    }

    /**
     * @return 所有标签
     */
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
            LOGGER.log(Level.WARNING, "error in get tag req", e);
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
            LOGGER.log(Level.WARNING, "error in get token req", e);
            throw new IllegalStateException("获取微信Token失败");
        }
    }

    /**
     * 创建群聊
     * @param name 群聊名称,不可为空
     * @param user 成员id列表,不少于2人
     * @param owner 群主,如果为空则从列表随机选取一人
     * @return 群聊id
     */
    public static String createChat(String name, List<String> user,String owner){
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("必须指定群聊名");
        }
        if (user == null || user.size() < 2) {
            throw new IllegalArgumentException("成员不少于2人");
        }

        try {
            AccessToken accessToken = getAccessToken();
            String request = String.format(CREATE_CHAT,accessToken.getToken());
            JSONObject params = new JSONObject();
            params.put("name",name);
            if (!StringUtils.isBlank(owner)) {
                params.put("owner",owner);
            }
            params.put("userlist",JSONArray.fromObject(user));
            String res = post(request,params.toString());
            JSONObject obj = JSONObject.fromObject(res);
            if (obj.getInt("errcode") == 0) {
                return obj.optString("chatid");
            } else {
                LOGGER.log(Level.WARNING, obj.toString());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "error in create chat", e);
        }
        return null;
    }

    /**
     * 发送群消息
     * @param chatId 群id
     * @param msg 消息
     */
    public static String sendChatMsg(String chatId,String msg) {
        try {
            JSONObject params = new JSONObject();
            params.put("chatid",chatId);
            params.put("msgtype","text");
            JSONObject content = new JSONObject();
            content.put("content", msg);
            params.put("text", content);
            AccessToken accessToken = getAccessToken();
            String query = String.format(SEND_CHAT, accessToken.getToken());
            return post(query, params.toString());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "error in send chat msg", e);
        }
        return null;
    }

    /**
     * 获取群聊信息
     * @param id 群id
     */
    public static Chat getChat(String id) {
        try {
            String request =String.format(GET_CHAT,getAccessToken().getToken(),id);
            String res = get(request);
            JSONObject obj = JSONObject.fromObject(res);
            if (obj.getInt("errcode") == 0) {
                JSONObject chatInfo = obj.getJSONObject("chat_info");
                Chat chat = new Chat();
                chat.setChatId(chatInfo.optString("chatid"));
                chat.setName(chatInfo.optString("name"));
                chat.setOwner(chatInfo.optString("owner"));
                List<String> users = (List<String>) JSONArray.toCollection(chatInfo.optJSONArray("userlist"));
                chat.setUsers(users);
                return chat;
            } else {
                LOGGER.log(Level.WARNING, obj.toString());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "error in get chat req", e);
        }
        return null;
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
            LOGGER.log(Level.WARNING, "error in req " + path, e);
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
            LOGGER.log(Level.WARNING, "error in post " + path, e);
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
            LOGGER.log(Level.WARNING, "error in load token", e);
        }
    }
    private static void saveAccessToken() {
        try {
            XmlFile xmlFile = new XmlFile(new File(Jenkins.getInstance().getRootDir(), CONFIG_FILE));
            xmlFile.write(accessToken);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "error in save token", e);
        }
    }
}
