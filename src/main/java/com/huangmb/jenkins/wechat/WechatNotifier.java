package com.huangmb.jenkins.wechat;

import com.huangmb.jenkins.wechat.bean.CustomGroup;
import com.huangmb.jenkins.wechat.bean.Receiver;
import com.huangmb.jenkins.wechat.bean.WechatUser;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

public class WechatNotifier extends Notifier {
    private boolean disablePublish;
    private String successMsg;
    private String failedMsg;
    private List<Receiver> receivers;

    @DataBoundConstructor
    public WechatNotifier(boolean disablePublish, List<Receiver> receivers, String successMsg, String failedMsg) {
        this.disablePublish = disablePublish;
        this.receivers = new ArrayList<>(receivers);
        this.successMsg = successMsg;
        this.failedMsg = failedMsg;
    }

    public boolean isDisablePublish() {
        return disablePublish;
    }

    @DataBoundSetter
    public void setDisablePublish(boolean disablePublish) {
        this.disablePublish = disablePublish;
    }

    @DataBoundSetter
    public void setSuccessMsg(String successMsg) {
        this.successMsg = successMsg;
    }

    @DataBoundSetter
    public void setFailedMsg(String failedMsg) {
        this.failedMsg = failedMsg;
    }

    @DataBoundSetter
    public void setReceivers(List<Receiver> receivers) {
        this.receivers = receivers;
    }

    public List<Receiver> getReceivers() {
        return receivers;
    }

    public String getSuccessMsg() {
        return successMsg;
    }

    public String getFailedMsg() {
        return failedMsg;
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
        Set<String> userSet = new HashSet<>(), partySet = new HashSet<>(), tagSet = new HashSet<>();
        if (receivers != null) {
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
                                List<WechatUser> users = group.getUsers();
                                for (WechatUser user : users) {
                                    userSet.add(user.getId());
                                }
                                break;
                            }
                        }
                        break;
                }
            }
        }
        String user = StringUtils.join(userSet, "|");
        String party = StringUtils.join(partySet, "|");
        String tag = StringUtils.join(tagSet, "|");
//        System.out.println(user);
//        System.out.println(party);
//        System.out.println(tag);

        if (StringUtils.isBlank(user) && StringUtils.isBlank(party) && StringUtils.isBlank(tag)) {
            logger.println("未填写任何微信接收人，无法发送微信通知");
            return true;
        }
        String msg = build.getResult() == Result.SUCCESS ? successMsg : failedMsg;
        if (StringUtils.isBlank(msg)) {
            logger.println("未填写消息内容，无法发送微信通知");
            return true;
        }
        EnvVars env = build.getEnvironment(listener);
        //填充环境变量
        msg = env.expand(msg);
        logger.println("微信通知内容: " + msg);
        try {
            String resp = WeChatAPI.sendMsg(user, party, tag, msg);
            checkResp(resp, logger);
        } catch (Exception e) {
            logger.println("微信通知发送失败: " + e.getMessage());
        }
        return true;
    }

    private void checkResp(String resp, PrintStream logger) {
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

}
