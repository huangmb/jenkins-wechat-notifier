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
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
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
    private static final String GET_USER_BY_DEPARTMENT_SIMPLE_URL = "https://qyapi.weixin.qq.com/cgi-bin/user/simplelist?access_token=%s&department_id=%s&fetch_child=%d";
    //获取部门成员详情
    private static final String GET_USER_BY_DEPARTMENT__URL = "https://qyapi.weixin.qq.com/cgi-bin/user/list?access_token=%s&department_id=%s&fetch_child=%d";
    //获取标签
    private static final String GET_TAGS_URL = "https://qyapi.weixin.qq.com/cgi-bin/tag/list?access_token=%s";
    //创建群聊
    private static final String CREATE_CHAT = "https://qyapi.weixin.qq.com/cgi-bin/appchat/create?access_token=%s";
    //推送群聊消息
    private static final String SEND_CHAT = "https://qyapi.weixin.qq.com/cgi-bin/appchat/send?access_token=%s";
    //获取群聊信息
    private static final String GET_CHAT = "https://qyapi.weixin.qq.com/cgi-bin/appchat/get?access_token=%s&chatid=%s";
    //更新群聊信息
    private static final String UPDATE_CHAT = "https://qyapi.weixin.qq.com/cgi-bin/appchat/update?access_token=%s";

    private static final String UPLOAD_MEDIA = "https://qyapi.weixin.qq.com/cgi-bin/media/upload?access_token=%s&type=%s";

    private static final String CONFIG_FILE = AccessToken.class.getName() + ".xml";
    private static AccessToken accessToken;

    static {
        Items.XSTREAM.registerConverter(new AccessTokenConverter());
    }

    public static Map<String, String> createReceiver(String user, String party, String tag) {
        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(user)) {
            params.put("touser", user);
        }
        if (StringUtils.isNotBlank(party)) {
            params.put("toparty", party);
        }
        if (StringUtils.isNotBlank(tag)) {
            params.put("totag", tag);
        }
        return params;
    }

    private static JSONObject createCommonParams(Map<String, String> receiver, String msgType) {
        JSONObject params = new JSONObject();
        params.put("msgtype", msgType);
        params.put("agentid", WechatAppConfiguration.get().getAgentId());
        for (Map.Entry<String, String> entry : receiver.entrySet()) {
            params.put(entry.getKey(), entry.getValue());
        }
        return params;
    }

    private static JSONObject jsonOf(String key, Object value) {
        JSONObject json = new JSONObject();
        json.put(key, value);
        return json;
    }

    private static JSONArray createArticles(String title, String imgId, String content, String digest) {
        JSONObject article = new JSONObject();
        article.put("title", title);
        article.put("thumb_media_id", imgId);
        article.put("content", content);
//            article.put("content_source_url","");
        if (StringUtils.isNotBlank(digest)) {
            article.put("digest", digest);
        }
        JSONArray articles = new JSONArray();
        articles.add(article);
        return articles;
    }

    private static String sendBaseMsg(Map<String, String> receiver, String msgType, JSONObject content) {
        JSONObject params = createCommonParams(receiver, msgType);
        params.put(msgType, content);
        AccessToken accessToken = getAccessToken();
        String query = String.format(SEND_URL, accessToken.getToken());
        return post(query, params.toString());
    }

    /**
     * 发送文本消息
     *
     * @param msg 消息内容
     */
    public static String sendMsg(Map<String, String> receiver, String msg) {
        return sendBaseMsg(receiver, "text", jsonOf("content", msg));
    }

    /**
     * 发送markdown消息
     *
     * @param receiver 接收人
     * @param content  Markdown文本
     */
    public static String sendMarkdown(Map<String, String> receiver, String content) {
        return sendBaseMsg(receiver, "markdown", jsonOf("content", content));
    }


    /**
     * 发送图片消息
     *
     * @param receiver 接收人
     * @param imgId    图片素材id
     */
    public static String sendImage(Map<String, String> receiver, String imgId) {
        return sendBaseMsg(receiver, "image", jsonOf("media_id", imgId));
    }

    /**
     * 发送文件
     *
     * @param receiver 接收人
     * @param fileId   文件素材id
     */
    public static String sendFile(Map<String, String> receiver, String fileId) {
        return sendBaseMsg(receiver, "file", jsonOf("media_id", fileId));
    }

    public static String sendCard(Map<String, String> receiver, String title, String description, String url, String btnText) {
        JSONObject content = createCard(title, description, url, btnText);
        return sendBaseMsg(receiver, "textcard", content);
    }

    private static JSONObject createCard(String title, String description, String url, String btnText) {
        JSONObject content = new JSONObject();
        content.put("title", title);
        content.put("description", description);
        content.put("url", url);
        content.put("btntxt", btnText);
        return content;
    }

    /**
     * 发送图文消息
     *
     * @param title   标题
     * @param content 内容
     * @param imgId   封面素材id
     */
    public static String sendNews(Map<String, String> receiver, String title, String content, String digest, String imgId) {
        JSONArray articles = createArticles(title, imgId, content, digest);
        return sendBaseMsg(receiver, "mpnews", jsonOf("articles", articles));
    }

    private static String uploadMedia(String type, File file) {
        try {
            AccessToken accessToken = getAccessToken();
            String query = String.format(UPLOAD_MEDIA, accessToken.getToken(), type);
            PostMethod postMethod = new PostMethod(query);
            FilePart part = new FilePart("media", file);
            MultipartRequestEntity entity = new MultipartRequestEntity(new Part[]{part}, postMethod.getParams());
            postMethod.setRequestEntity(entity);

            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
            int status = client.executeMethod(postMethod);
            if (status == HttpStatus.SC_OK) {
                String res = postMethod.getResponseBodyAsString();
                JSONObject jsonObject = JSONObject.fromObject(res);
                String mediaId = jsonObject.optString("media_id");
                if (StringUtils.isEmpty(mediaId)) {
                    System.out.println(res);
                }
                return mediaId;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 上传图片
     *
     * @param img 图片
     * @return 图片id, 上传失败为null
     */
    public static String uploadImg(File img) {
        return uploadMedia("image", img);
    }

    public static String uploadFile(File file) {
        return uploadMedia("file", file);
    }

    /**
     * 获取部门
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
     */
    public static Map<String, List<WechatUser>> getAllUsers(List<WechatDepartment> departments) {
        List<WechatDepartment> rootDepartments = Utils.getRootDepartments(departments);
        Map<String, List<WechatUser>> map = new HashMap<>();

        try {
            AccessToken token = getAccessToken();
            for (WechatDepartment department : rootDepartments) {
                String query = String.format(GET_USER_BY_DEPARTMENT__URL, token.getToken(), department.getId(), 1);
                String resp = get(query);
                JSONObject obj = JSONObject.fromObject(resp);
                if (obj.getInt("errcode") == 0) {
                    JSONArray userList = obj.getJSONArray("userlist");
                    int size = userList.size();
                    for (int i = 0; i < size; i++) {
                        JSONObject user = userList.getJSONObject(i);
                        String userId = user.getString("userid");
                        String name = user.getString("name");
                        String email = user.getString("email");
                        JSONArray dep = user.optJSONArray("department");

                        WechatUser u = new WechatUser();
                        u.setId(userId);
                        u.setName(name);
                        u.setEmail(email);

                        for (int j = 0, n = dep.size(); j < n; j++) {
                            addToDepartment(map, dep.optString(j), u);
                        }
                    }
                }
            }
            return map;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "error in get user req", e);
        }
        return null;
    }

    private static void addToDepartment(Map<String, List<WechatUser>> map, String depId, WechatUser user) {
        List<WechatUser> userList = map.get(depId);
        if (userList == null) {
            userList = new ArrayList<>();
            map.put(depId, userList);
        }
        userList.add(user);
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
     *
     * @param name  群聊名称,不可为空
     * @param user  成员id列表,不少于2人
     * @param owner 群主,如果为空则从列表随机选取一人
     * @return 群聊id
     */
    public static String createChat(String name, List<String> user, String owner) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("必须指定群聊名");
        }
        if (user == null || user.size() < 2) {
            throw new IllegalArgumentException("成员不少于2人");
        }

        try {
            AccessToken accessToken = getAccessToken();
            String request = String.format(CREATE_CHAT, accessToken.getToken());
            JSONObject params = new JSONObject();
            params.put("name", name);
            if (!StringUtils.isBlank(owner)) {
                params.put("owner", owner);
            }
            params.put("userlist", JSONArray.fromObject(user));
            String res = post(request, params.toString());
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

    private static String sendBaseChatMsg(String chatId, String type, JSONObject msg) {
        try {
            JSONObject params = new JSONObject();
            params.put("chatid", chatId);
            params.put("msgtype", type);
            params.put(type, msg);
            AccessToken accessToken = getAccessToken();
            String query = String.format(SEND_CHAT, accessToken.getToken());
            return post(query, params.toString());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "error in send chat " + type, e);
        }
        return null;
    }

    /**
     * 发送群消息
     *
     * @param chatId 群id
     * @param msg    消息
     */
    public static String sendChatMsg(String chatId, String msg) {
        return sendBaseChatMsg(chatId, "text", jsonOf("content", msg));
    }

    /**
     * 发送群聊图片消息
     *
     * @param chatId  群id
     * @param mediaId 图片素材id
     */
    public static String sendChatImage(String chatId, String mediaId) {
        return sendBaseChatMsg(chatId, "image", jsonOf("media_id", mediaId));
    }

    /**
     * 发送群聊文件消息
     *
     * @param chatId  群id
     * @param mediaId 素材id
     */
    public static String sendChatFile(String chatId, String mediaId) {
        return sendBaseChatMsg(chatId, "file", jsonOf("media_id", mediaId));
    }

    /**
     * 发送群聊卡片消息
     *
     * @param chatId      群id
     * @param title       标题
     * @param description 描述
     * @param url         调整url
     * @param btnText     按钮文字
     */
    public static String sendChatCard(String chatId, String title, String description, String url, String btnText) {
        JSONObject content = createCard(title, description, url, btnText);
        return sendBaseChatMsg(chatId, "textcard", content);
    }

    /**
     * 发送群聊图文消息
     *
     * @param chatId  群id
     * @param title   标题
     * @param content 内容
     * @param imgId   图片素材id
     */
    public static String sendChatNews(String chatId, String title, String content, String digest, String imgId) {
        JSONArray articles = createArticles(title, imgId, content, digest);
        return sendBaseChatMsg(chatId, "mpnews", jsonOf("articles", articles));
    }

    /**
     * 发送Markdown 格式的群消息
     *
     * @param chatId 群id
     * @param msg    Markdown内容
     */
    public static String sendChatMarkdown(String chatId, String msg) {
        return sendBaseChatMsg(chatId, "markdown", jsonOf("content", msg));
    }

    /**
     * 获取群聊信息
     *
     * @param id 群id
     */
    public static Chat getChat(String id) {
        try {
            String request = String.format(GET_CHAT, getAccessToken().getToken(), id);
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

    private static String readAsString(HttpURLConnection connection) {
        try {
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(10 * 1000);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                return IOUtils.toString(inputStream);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "error in req " + connection.getURL(), e);
        }
        return "";
    }

    private static String get(String path) {
        try {
            URL url = new URL(path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            return readAsString(connection);
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
            return readAsString(connection);
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
