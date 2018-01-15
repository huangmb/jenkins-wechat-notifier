package com.huangmb.jenkins.wechat;

public class AccessToken {
    private String token;
    private String expiresIn;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(String expiresIn) {
        this.expiresIn = expiresIn;
    }

    public boolean isValid() {
        try {
            long expireTime = Long.valueOf(expiresIn);
            if (System.currentTimeMillis() / 1000 + 10 < expireTime) {

                return true;
            }
        } catch (Exception ignore) {

        }
        return false;
    }
}
