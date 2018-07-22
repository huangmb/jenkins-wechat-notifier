package com.huangmb.jenkins.wechat.group;

import com.huangmb.jenkins.wechat.WechatAppConfiguration;
import com.huangmb.jenkins.wechat.WechatConfig;
import com.huangmb.jenkins.wechat.bean.CustomGroup;
import hudson.Extension;
import jenkins.model.Jenkins;

import java.util.List;

/**
 * Created by bob.huang on 2018/7/21
 */
@Extension
public class CustomGroupConfig extends WechatConfig {
    @Override
    public String getDisplayName() {
        return "分组";
    }

    @Override
    public String getUrlName() {
        return "group";
    }

    public WechatConfig getDynamic(String name) {
        CreateCustomGroup createCustomGroup = getCreateCustomGroup();
        if (createCustomGroup.getUrlName().equals(name)) {
            return createCustomGroup;
        }
        try {
            int i =Integer.parseInt(name);
            List<CustomGroup> groups = getCustomGroups();
            if (i >= 0 && i < groups.size()) {
                EditGroup editGroup = getEditGroup();
                editGroup.setGroup(groups.get(0));
                return editGroup;
            }
        } catch (Exception e) {

        }
        return null;
    }

    private CreateCustomGroup getCreateCustomGroup() {
        return Jenkins.getInstance().getExtensionList(CreateCustomGroup.class).get(CreateCustomGroup.class);
    }

    private EditGroup getEditGroup() {
        return Jenkins.getInstance().getExtensionList(EditGroup.class).get(EditGroup.class);
    }

    public List<CustomGroup> getCustomGroups() {
        return WechatAppConfiguration.get().getGroups();
    }

    @Extension
    public static final class DescriptorImpl extends WechatConfigDescriptor {

    }
}
