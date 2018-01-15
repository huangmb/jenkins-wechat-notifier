package com.huangmb.jenkins.wechat;


import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class AccessTokenConverter implements Converter {

    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext marshallingContext) {
        AccessToken token = (AccessToken) o;
        writer.addAttribute("token",token.getToken());
        writer.addAttribute("expireIn",token.getExpiresIn());

    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext unmarshallingContext) {
        String token = reader.getAttribute("token");
        String expireIn = reader.getAttribute("expireIn");
        AccessToken accessToken = new AccessToken();
        accessToken.setToken(token);
        accessToken.setExpiresIn(expireIn);
        return accessToken;
    }

    @Override
    public boolean canConvert(Class aClass) {
        return AccessToken.class == aClass;
    }
}
