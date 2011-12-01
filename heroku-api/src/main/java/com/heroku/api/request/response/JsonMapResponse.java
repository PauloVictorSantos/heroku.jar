package com.heroku.api.request.response;

import com.heroku.api.Heroku;
import com.heroku.api.parser.Json;
import com.heroku.api.parser.TypeReference;
import com.heroku.api.request.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: Javadoc
 *
 * @author Naaman Newbold
 */
public class JsonMapResponse implements Response {

    protected final Map<String, String> data;
    private final byte[] rawData;

    public JsonMapResponse(byte[] bytes) {
        this.rawData = bytes;
        this.data = Json.getJsonParser().parse(this.rawData, new TypeReference<Map<String, String>>() {
        }.getType());
    }


    @Override
    public String get(String key) {
        if (!data.containsKey(key)) {
            throw new IllegalArgumentException(key + " is not present.");
        }
        return data.get(key);
    }

    @Override
    public String get(Heroku.ResponseKey key) {
        return get(key.toString());
    }

    @Override
    public byte[] getRawData() {
        return rawData;
    }

    @Override
    public Map<String, String> getData() {
        return new HashMap<String, String>(data);
    }

    @Override
    public String toString() {
        String stringValue = "{ ";
        String separator = "";
        for (Map.Entry<String, String> e : data.entrySet()) {
            stringValue += separator + e.getKey() + " : " + e.getValue();
            separator = ", ";
        }
        return stringValue + " }";
    }
}
