# jenkins-wechat-notifier
Jenkins微信通知插件

微信通知插件用于在构建完成后通过企业微信应用向指定用户或群聊发送消息通知


## Pipeline support

    weworkchatknotify chatId:'${chatGroupId}', successMarkdownMsg:'', failMarkdownMsg:''
    
    weworkchatknotify userId:'${userId}', successMarkdownMsg:'', failMarkdownMsg:''
