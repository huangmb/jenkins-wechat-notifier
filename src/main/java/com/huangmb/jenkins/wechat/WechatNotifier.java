package com.huangmb.jenkins.wechat;

import com.huangmb.jenkins.wechat.bean.Chat;
import com.huangmb.jenkins.wechat.bean.CustomGroup;
import com.huangmb.jenkins.wechat.bean.Receiver;
import hudson.DescriptorExtensionList;
import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WechatNotifier extends Notifier {
    private Set<String> userSet = new HashSet<>();
    private Set<String> partySet = new HashSet<>();
    private Set<String> tagSet = new HashSet<>();
    private Set<String> chatSet = new HashSet<>();

    private boolean disablePublish;
    private List<Receiver> receivers;

    private MessageType successMsgType;
    private MessageType failMsgType;

    private String successMsg;
    private String failedMsg;


    @DataBoundConstructor
    public WechatNotifier(boolean disablePublish, List<Receiver> receivers,String successMsg,String failedMsg, MessageType successMsgType, MessageType failMsgType) {
        this.disablePublish = disablePublish;
        this.receivers = new ArrayList<>(receivers);
        this.successMsg = successMsg;
        this.failedMsg = failedMsg;
        this.successMsgType = successMsgType;
        this.failMsgType = failMsgType;
    }

    public boolean isDisablePublish() {
        return disablePublish;
    }

    @DataBoundSetter
    public void setDisablePublish(boolean disablePublish) {
        this.disablePublish = disablePublish;
    }

    @DataBoundSetter
    public void setReceivers(List<Receiver> receivers) {
        this.receivers = receivers;
    }

    public List<Receiver> getReceivers() {
        return receivers;
    }

    public MessageType getSuccessMsgType() {
        return successMsgType;
    }

    public MessageType getFailMsgType() {
        return failMsgType;
    }

    public List<Descriptor<MessageType>> getMsgTypeDescriptors() {
        DescriptorExtensionList<MessageType, Descriptor<MessageType>> list = Jenkins.getInstance().getDescriptorList(MessageType.class);

        List<Descriptor<MessageType>> result = new ArrayList<>(list);
        Collections.sort(result, new Comparator<Descriptor<MessageType>>() {
            @Override
            public int compare(Descriptor<MessageType> o1, Descriptor<MessageType> o2) {
                return ((MessageTypeDescriptor) o1).getOrder() - ((MessageTypeDescriptor) o2).getOrder();
            }
        });
        return result;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public WechatNotifierDescriptor getDescriptor() {
        return (WechatNotifierDescriptor) super.getDescriptor();
    }


    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();
        if (disablePublish) {
            logger.println("本次构建不发布微信消息，启用微信通知请在设置中取消勾选禁用发送");
            return true;
        }
        updateReceivers();
        String user = StringUtils.join(userSet, "|");
        String party = StringUtils.join(partySet, "|");
        String tag = StringUtils.join(tagSet, "|");

        Map<String, String> receiver = WeChatAPI.createReceiver(user, party, tag);
        boolean hasReceiver = !receiver.isEmpty();
        boolean hasChat = !chatSet.isEmpty();
        if (!hasReceiver && !hasChat) {
            logger.println("未填写任何微信接收人");
            return true;
        }

        doCompatible();
        MessageType messageType = build.getResult() == Result.SUCCESS ? successMsgType : failMsgType;
        if (messageType == null) {
            logger.println("未填写消息内容");
            return true;
        }

        EnvVars env = build.getEnvironment(listener);
        messageType.setLogger(logger);
        messageType.setEnvVars(env);
        if (!messageType.checkValue()) {
            logger.println("输入不合法");
            return true;
        }

        //填充环境变量
        try {
            if (hasReceiver) {
                messageType.sendMsg(receiver);
            }
            for (String chatId : chatSet) {
                messageType.sendChat(chatId);
            }

        } catch (Exception e) {
            logger.println("微信通知发送失败: " + e.getMessage());
        }
        return true;
    }

    /**
     * 兼容旧版本
     */
    private void doCompatible() {
        if (successMsgType == null && StringUtils.isNotEmpty(successMsg)) {
            successMsgType = new Text(successMsg);
        }
        if (failMsgType == null && StringUtils.isNotEmpty(failedMsg)) {
            failMsgType = new Text(failedMsg);
        }
    }

    private void updateReceivers() {
        userSet.clear();
        partySet.clear();
        tagSet.clear();
        chatSet.clear();
        if (receivers == null) {
            return;
        }
        for (Receiver receiver : receivers) {
            if ("-1".equals(receiver.getId())) {
                continue;
            }
            switch (receiver.getType()) {
                case "user":
                    userSet.add(receiver.getId());
                    break;
                case "party":
                    partySet.add(receiver.getId());
                    break;
                case "tag":
                    tagSet.add(receiver.getId());
                    break;
                case "group":
                    List<CustomGroup> customGroups = ContactsProvider.getInstance().getCustomGroups();
                    for (CustomGroup group : customGroups) {
                        if (StringUtils.equals(group.getName(), receiver.getId())) {
                            List<String> users = group.getUsers();
                            for (String user : users) {
                                if ("-1".equals(user)) {
                                    continue;
                                }
                                userSet.add(user);
                            }
                            break;
                        }
                    }
                    break;
                case "chat":
                    List<Chat> chats = ContactsProvider.getInstance().getChats();
                    for (Chat chat : chats) {
                        if (StringUtils.equals(chat.getChatId(), receiver.getId())) {
                            chatSet.add(receiver.getId());
                            break;
                        }
                    }
                    break;
            }
        }

    }


    public static abstract class MessageType implements ExtensionPoint, Describable<MessageType> {
        protected String name;
        private PrintStream logger;
        protected EnvVars envVars;

        protected MessageType(String name) {
            this.name = name;
        }

        protected boolean checkValue() {
            return true;
        }

        public void setLogger(PrintStream logger) {
            this.logger = logger;
        }

        public void setEnvVars(EnvVars envVars) {
            this.envVars = envVars;
        }

        public void log(String msg) {
            if (logger != null) {
                logger.println("[" + name + "]: " + msg);
            }
        }

        /**
         * 填充环境变量占位符
         *
         * @param text 原文
         * @return 填充占位符后的文本
         */
        public String expand(String text) {
            if (envVars != null) {
                return envVars.expand(text);
            }
            return text;
        }

        /**
         * 发送应用消息
         *
         * @param receiver 接收人
         */
        public final void sendMsg(Map<String, String> receiver) {
            String resp = sendMsgInternal(receiver);
            checkResp(resp);
        }

        /**
         * 发送应用消息
         *
         * @param receiver 接收人列表
         */
        protected abstract String sendMsgInternal(Map<String, String> receiver);

        /**
         * 发送群聊消息
         *
         * @param chatId 群id
         */
        public final void sendChat(String chatId) {
            String resp = sendChatInternal(chatId);
            checkResp(resp);
        }

        /**
         * 发送群聊消息
         *
         * @param chatId 群id
         */
        protected abstract String sendChatInternal(String chatId);

        private void checkResp(String resp) {
            try {
                JSONObject obj = JSONObject.fromObject(resp);
                int errcode = obj.getInt("errcode");
                if (errcode != 0) {
                    logger.println("微信通知发送失败:" + resp);
                    return;
                }
                logger.println("微信通知发送成功");

                String invaliduser = obj.optString("invaliduser");
                String invalidparty = obj.optString("invalidparty");
                String invalidtag = obj.optString("invalidtag");
                String result = "";
                if (StringUtils.isNotBlank(invaliduser)) {
                    result += "用户" + invaliduser.replace("|", " ") + ",";
                }
                if (StringUtils.isNotBlank(invalidparty)) {
                    result += "部门" + invalidparty.replace("|", " ") + ",";
                }
                if (StringUtils.isNotBlank(invalidtag)) {
                    result += "标签" + invalidtag.replace("|", " ");
                }
                if (StringUtils.isNotBlank(result)) {
                    result += "未接收到本消息，接收人不存在或者不在企业应用的可见范围内";
                    logger.println(result);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public Descriptor<MessageType> getDescriptor() {
            return Jenkins.getInstance().getDescriptor(getClass());
        }
    }

    public static class MessageTypeDescriptor extends Descriptor<MessageType> {
        private int order;
        private String name;

        public MessageTypeDescriptor(Class<? extends MessageType> clazz, String name, int order) {
            super(clazz);
            this.order = order;
            this.name = name;
        }

        protected int getOrder() {
            return order;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return name;
        }
    }

    public static class Text extends MessageType {
        private String content;

        @DataBoundConstructor
        public Text(String content) {
            super("文本消息");
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        @Override
        protected boolean checkValue() {
            if (StringUtils.isBlank(content)) {
                log("内容不能为空");
                return false;
            }
            return true;
        }

        @Override
        protected String sendMsgInternal(Map<String, String> receiver) {
            return WeChatAPI.sendMsg(receiver, expand(content));
        }

        @Override
        protected String sendChatInternal(String chatId) {
            return WeChatAPI.sendChatMsg(chatId, expand(content));
        }

        @Extension
        public static final MessageTypeDescriptor D = new MessageTypeDescriptor(Text.class, "文本消息", 1);
    }

    public static class Markdown extends MessageType {
        private String content;

        @DataBoundConstructor
        public Markdown(String content) {
            super("Markdown");
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        @Override
        protected boolean checkValue() {
            if (StringUtils.isBlank(content)) {
                log("内容不能为空");
                return false;
            }
            return true;
        }

        @Override
        protected String sendMsgInternal(Map<String, String> receiver) {
            return WeChatAPI.sendMarkdown(receiver, expand(content));
        }

        @Override
        protected String sendChatInternal(String chatId) {
            return WeChatAPI.sendChatMarkdown(chatId, envVars.expand(content));
        }

        @Extension
        public static final MessageTypeDescriptor D = new MessageTypeDescriptor(Markdown.class, "Markdown", 2);

    }


    /**
     * 带附件的消息,如文件消息\图片消息\图文消息等
     */
    protected static abstract class AbstractFileMsg extends MessageType implements Utils.Log {


        public static final String TYPE_FILE = "file";
        public static final String TYPE_IMAGE = "image";
        /**
         * 扫描文件夹
         */
        private String scanDir;
        /**
         * 文件,支持通配符
         */
        private String wildcard;

        // 缓存文件上传结果,素材id
        protected String mediaId;

        public AbstractFileMsg(String name, String scanDir, String wildcard) {
            super(name);
            this.scanDir = scanDir;
            this.wildcard = wildcard;
        }

        public String getScanDir() {
            return scanDir;
        }

        public String getWildcard() {
            return wildcard;
        }

        protected void uploadFile() {
            uploadMedia(TYPE_FILE);
        }

        protected void uploadImage() {
            uploadMedia(TYPE_IMAGE);
        }

        private void uploadMedia(String type) {
            mediaId = null;

            String path = Utils.findFile(expand(scanDir), expand(wildcard), this);
            if (StringUtils.isEmpty(path)) {
                log("没找到文件");
                return;
            }
            File file = new File(path);
            if ("file".equals(type)) {
                mediaId = WeChatAPI.uploadFile(file);
            } else if ("image".equals(type)) {
                mediaId = WeChatAPI.uploadImg(file);
            }

            if (StringUtils.isEmpty(mediaId)) {
                log("文件上传失败: " + path);
            }
        }
    }

    public static class Image extends AbstractFileMsg {

        @DataBoundConstructor
        public Image(String scanDir, String wildcard) {
            super("图片消息", scanDir, wildcard);
        }

        @Override
        protected boolean checkValue() {
            uploadImage();
            return StringUtils.isNotEmpty(mediaId);
        }

        @Override
        protected String sendMsgInternal(Map<String, String> receiver) {
            return WeChatAPI.sendImage(receiver, mediaId);
        }

        @Override
        protected String sendChatInternal(String chatId) {
            return WeChatAPI.sendChatImage(chatId, mediaId);
        }

        @Extension
        public static final MessageTypeDescriptor D = new MessageTypeDescriptor(Image.class, "图片消息", 3);
    }

    public static class FileMsg extends AbstractFileMsg {

        @DataBoundConstructor
        public FileMsg(String scanDir, String wildcard) {
            super("文件消息", scanDir, wildcard);
        }

        @Override
        protected boolean checkValue() {
            uploadFile();
            return StringUtils.isNotEmpty(mediaId);
        }

        @Override
        protected String sendMsgInternal(Map<String, String> receiver) {
            return WeChatAPI.sendFile(receiver, mediaId);
        }

        @Override
        protected String sendChatInternal(String chatId) {
            return WeChatAPI.sendChatFile(chatId, mediaId);
        }

        @Extension
        public static final MessageTypeDescriptor D = new MessageTypeDescriptor(FileMsg.class, "文件消息", 4);
    }

    /**
     * 图文消息
     */
    public static class News extends AbstractFileMsg {
        private String title;
        //正文
        private String content;
        //描述
        private String digest;

        @DataBoundConstructor
        public News(String title, String content, String digest, String scanDir, String wildcard) {
            super("图文消息", scanDir, wildcard);
            this.title = title;
            this.content = content;
            this.digest = digest;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

        public String getDigest() {
            return digest;
        }

        @Override
        protected boolean checkValue() {
            if (StringUtils.isBlank(title) || StringUtils.isBlank(content)) {
                log("标题或内容不能为空");
                return false;
            }
            uploadImage();
            return !StringUtils.isEmpty(mediaId);
        }

        @Override
        protected String sendMsgInternal(Map<String, String> receiver) {
            return WeChatAPI.sendNews(receiver, expand(title), expand(content), expand(digest), mediaId);
        }

        @Override
        protected String sendChatInternal(String chatId) {
            return WeChatAPI.sendChatNews(chatId, expand(title), expand(content), expand(digest), mediaId);
        }

        @Extension
        public static final MessageTypeDescriptor D = new MessageTypeDescriptor(News.class, "图文消息", 4);
    }

    public static class Card extends MessageType {
        private String title;
        private String description;
        private String url;
        private String btnText;

        @DataBoundConstructor
        public Card(String title, String description, String url, String btnText) {
            super("卡片消息");
            this.title = title;
            this.description = description;
            this.url = url;
            this.btnText = btnText;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getBtnText() {
            return btnText;
        }

        public void setBtnText(String btnText) {
            this.btnText = btnText;
        }

        @Override
        protected boolean checkValue() {
            return StringUtils.isNotBlank(title) && StringUtils.isNotBlank(description) && StringUtils.isNotBlank(url);
        }

        @Override
        protected String sendMsgInternal(Map<String, String> receiver) {
            return WeChatAPI.sendCard(receiver, expand(title), expand(description), expand(url), expand(btnText));
        }

        @Override
        protected String sendChatInternal(String chatId) {
            return WeChatAPI.sendChatCard(chatId, expand(title), expand(description), expand(url), expand(btnText));
        }

        @Extension
        public static final MessageTypeDescriptor D = new MessageTypeDescriptor(Card.class, "卡片消息", 5);
    }


}
