package com.ztbsuper.dingding;

import com.alibaba.fastjson.JSONObject;
import hudson.ProxyConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by Marvin on 16/10/8.
 */
public class DingdingServiceImpl implements DingdingService {

    private Logger logger = LoggerFactory.getLogger(DingdingService.class);

    private String jenkinsURL;

    private boolean onStart;

    private boolean onSuccess;

    private boolean onFailed;

    private boolean onAbort;

    private TaskListener listener;

    private AbstractBuild build;

    private static final String apiUrl = "https://oapi.dingtalk.com/robot/send?access_token=";

    private String api;

    public DingdingServiceImpl(String jenkinsURL, String token, boolean onStart, boolean onSuccess, boolean onFailed, boolean onAbort, TaskListener listener, AbstractBuild build) {
        this.jenkinsURL = jenkinsURL;
        this.onStart = onStart;
        this.onSuccess = onSuccess;
        this.onFailed = onFailed;
        this.onAbort =  onAbort;
        this.listener = listener;
        this.build = build;
        this.api = apiUrl + token;
    }

    @Override
    public void start() {
        String pic = "http://www.qqzhi.com/uploadpic/2014-09-18/125457131.jpg";
        String title = String.format("%s%s开始构建", build.getProject().getDisplayName(), build.getDisplayName() + "分支：请关注成功/失败通知\n");
        String content = String.format("项目[%s%s]开始构建", build.getProject().getDisplayName(), build.getDisplayName() + "分支：请关注成功/失败通知");
        String link = getBuildUrl();
        if (onStart) {
            logger.info("send link msg from " + listener.toString());
            sendLinkMessage(link, content, title, pic);
        }
    }

    private String getBuildUrl() {
        if (jenkinsURL.endsWith("/")) {
            return jenkinsURL + build.getUrl();
        } else {
            return jenkinsURL + "/" + build.getUrl();
        }
    }

    @Override
    public void success() {
        String branch = null;
        try {
            branch = build.getEnvironment().get("GIT_BRANCH", "未知分支");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String pic = "http://www.qqzhi.com/uploadpic/2015-01-22/035257809.jpg";
        String title = String.format("%s%s构建成功", build.getProject().getDisplayName(), build.getDisplayName() + "分支：" + branch);
        String content = String.format("项目[%s%s]构建成功, 状态:%s, 耗时:%s", build.getProject().getDisplayName(), build.getDisplayName() + "分支：" + branch, build.getBuildStatusSummary().message, build.getDurationString());
        String link = getBuildUrl();
        logger.info(link);
        if (onSuccess) {
            logger.info("send link msg from " + listener.toString());
            sendLinkMessage(link, content, title, pic);
        }
    }

    @Override
    public void failed() {
        String branch = null;
        try {
            branch = build.getEnvironment().get("GIT_BRANCH", "未知分支");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String pic = "http://www.qqzhi.com/uploadpic/2015-01-22/035258591.jpg";
        String title = String.format("%s%s构建失败", build.getProject().getDisplayName(), build.getDisplayName() + "分支：" + branch);
        String content = String.format("项目[%s%s]构建失败, 状态:%s, 耗时:%s", build.getProject().getDisplayName(), build.getDisplayName() + "分支：" + branch, build.getBuildStatusSummary().message, build.getDurationString());

        String link = getBuildUrl();
        logger.info(link);
        if (onFailed) {
            logger.info("send link msg from " + listener.toString());
            sendLinkMessage(link, content, title, pic);
        }
    }

    @Override
    public void abort() {
        String branch = null;
        try {
            branch = build.getEnvironment().get("GIT_BRANCH", "未知分支");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String pic = "http://www.iconsdb.com/icons/preview/soylent-red/x-mark-3-xxl.png";
        String title = String.format("%s%s构建中断", build.getProject().getDisplayName(), build.getDisplayName() + "分支：" + branch);
        String content = String.format("项目[%s%s]构建中断, 状态:%s, 耗时:%s", build.getProject().getDisplayName(), build.getDisplayName() + "分支：" + branch, build.getBuildStatusSummary().message, build.getDurationString());

        String link = getBuildUrl();
        logger.info(link);
        if (onAbort) {
            logger.info("send link msg from " + listener.toString());
            sendLinkMessage(link, content, title, pic);
        }
    }

    private void sendTextMessage(String msg) {

    }

    private void sendLinkMessage(String link, String msg, String title, String pic) {
        HttpClient client = getHttpClient();
        PostMethod post = new PostMethod(api);

        JSONObject body = new JSONObject();
        body.put("msgtype", "link");


        JSONObject linkObject = new JSONObject();
        linkObject.put("text", msg);
        linkObject.put("title", title);
        linkObject.put("picUrl", pic);
        linkObject.put("messageUrl", link);

        body.put("link", linkObject);
        try {
            post.setRequestEntity(new StringRequestEntity(body.toJSONString(), "application/json", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error("build request error", e);
        }
        try {
            client.executeMethod(post);
            logger.info(post.getResponseBodyAsString());
        } catch (IOException e) {
            logger.error("send msg error", e);
        }
        post.releaseConnection();
    }


    private HttpClient getHttpClient() {
        HttpClient client = new HttpClient();
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null && jenkins.proxy != null) {
            ProxyConfiguration proxy = jenkins.proxy;
            if (proxy != null && client.getHostConfiguration() != null) {
                client.getHostConfiguration().setProxy(proxy.name, proxy.port);
                String username = proxy.getUserName();
                String password = proxy.getPassword();
                // Consider it to be passed if username specified. Sufficient?
                if (username != null && !"".equals(username.trim())) {
                    logger.info("Using proxy authentication (user=" + username + ")");
                    client.getState().setProxyCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials(username, password));
                }
            }
        }
        return client;
    }
}
