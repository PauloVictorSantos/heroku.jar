package com.heroku.api.request.app;

import com.heroku.api.Heroku;
import com.heroku.api.exception.RequestFailedException;
import com.heroku.api.http.Http;
import com.heroku.api.http.HttpUtil;
import com.heroku.api.request.Request;
import com.heroku.api.request.RequestConfig;

import java.util.Collections;
import java.util.Map;

/**
 * @author Ryan Brainard
 */
public class AppExists implements Request<Boolean> {

    private final RequestConfig config;

    public AppExists(String appName) {
        this.config = new RequestConfig().app(appName);
    }

    @Override
    public Http.Method getHttpMethod() {
        return Http.Method.GET;
    }

    @Override
    public String getEndpoint() {
        return Heroku.Resource.App.format(config.get(Heroku.RequestKey.AppName));
    }

    @Override
    public boolean hasBody() {
        return false;
    }

    @Override
    public String getBody() {
        throw HttpUtil.noBody();
    }

    @Override
    public Http.Accept getResponseType() {
        return Http.Accept.JSON;
    }

    @Override
    public Map<String, String> getHeaders() {
        return Collections.emptyMap();
    }

    @Override
    public Boolean getResponse(byte[] data, int code) {
        if (Http.Status.OK.equals(code) || Http.Status.FORBIDDEN.equals(code)) {
            return true;
        } else if (Http.Status.NOT_FOUND.equals(code)) {
            return false;
        } else {
            throw new RequestFailedException("Unexpected app exist status", code, data);
        }
    }
}