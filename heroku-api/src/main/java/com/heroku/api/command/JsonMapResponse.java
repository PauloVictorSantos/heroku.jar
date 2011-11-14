package com.heroku.api.command;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO: Javadoc
 *
 * @author Naaman Newbold
 */
public class JsonMapResponse implements CommandResponse {

    private final Map<String, String> data;
    private final byte[] rawData;

    public JsonMapResponse(InputStream in) {
        this.rawData = CommandUtil.getBytes(in);
        Type listType = new TypeToken<HashMap<String, String>>() {
        }.getType();
        this.data = Collections.unmodifiableMap(new Gson().<Map<String, String>>fromJson(CommandUtil.bytesReader(this.rawData), listType));

    }


    @Override
    public String get(String key) {
        if (!data.containsKey(key)) {
            throw new IllegalArgumentException(key + " is not present.");
        }
        return data.get(key);
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