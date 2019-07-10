# jenkins-wechat-notifier
Jenkins微信通知插件

微信通知插件用于在构建完成后通过企业微信应用向指定用户或群聊发送消息通知


## Pipeline support

    # ${chatGroupIds}，群聊ID，逗号分隔，ID需确保在Jenkins里配置过，在此查看 ${JENKINS_URL}/wechat/chat/
    weworkchatknotify chatIds:'${chatGroupIds}', successMarkdownMsg:'', failMarkdownMsg:''
    
    # ${userEmails}, 用户邮箱, 逗号分隔
    weworkchatknotify userEmails:'${userEmails}', successMarkdownMsg:'', failMarkdownMsg:''
    
    # 完整参数如下
    weworkchatknotify(chatIds?: String, disablePublish?: boolean, failMarkdownMsg?: String, failMsgType?: MessageType{Card(title: String, description: String, url: String, btnText: String) | FileMsg(scanDir: String, wildcard: String) | Image(scanDir: String, wildcard: String) | Markdown(content: String) | News(title: String, content: String, digest: String, scanDir: String, wildcard: String) | Text(content: String)}, receivers?: Receiver(type: String, id: String)[], successMarkdownMsg?: String, successMsgType?: MessageType{Card(title: String, description: String, url: String, btnText: String) | FileMsg(scanDir: String, wildcard: String) | Image(scanDir: String, wildcard: String) | Markdown(content: String) | News(title: String, content: String, digest: String, scanDir: String, wildcard: String) | Text(content: String)}, userEmails?: String)
