package com.huangmb.jenkins.wechat.group;

import com.huangmb.jenkins.wechat.Utils;
import com.huangmb.jenkins.wechat.WechatAppConfiguration;
import com.huangmb.jenkins.wechat.WechatConfig;
import com.huangmb.jenkins.wechat.bean.CustomGroup;
import com.huangmb.jenkins.wechat.chat.CreateChat;
import hudson.Extension;
import hudson.security.Permission;
import hudson.util.FormApply;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by bob.huang on 2018/7/19
 */
@Extension
public class CreateCustomGroup extends CreateChat {
    private static final Logger LOGGER = Logger.getLogger(CreateCustomGroup.class.getSimpleName());


    @RequirePOST
    @Override
    public HttpResponse doConfigSubmit(StaplerRequest req) throws ServletException {
        Jenkins.getInstance().checkPermission(Permission.WRITE);

        setErrorMsg(null);

        LOGGER.info("正在新建分组");
        JSONObject form = req.getSubmittedForm();
        bindForm(form);
        if (StringUtils.isBlank(name)) {
            return failOnCheckParameters(req, "分组名称不能为空");
        }

        if (users.isEmpty()) {
            return failOnCheckParameters(req, "分组成员为空");
        }

        LOGGER.info("分组名称 " + name);
        LOGGER.info("分组成员 " + users);

        if (FormApply.isApply(req)) {
            return FormApply.success("./");
        }

        CustomGroup group = new CustomGroup(name, users);
        List<CustomGroup> groups = WechatAppConfiguration.get().getGroups();
        groups.add(group);
        WechatAppConfiguration.get().setGroups(groups);
        return FormApply.success("../");
    }

    @Extension
    public static class DescriptorImpl extends CreateChat.DescriptorImpl {

    }
}
