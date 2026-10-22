package com.pousadamareal.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class JsonUtils {
    public static Map<String, Object> map(Object... pairs) {
        var map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
