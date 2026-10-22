package br.com.maralta.pousada;

import java.util.LinkedHashMap;
import java.util.Map;

public record Message(long id, Channel channel, String to, String subject, String body) {
    public Map<String, Object> toJson() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", id);
        json.put("channel", channel);
        json.put("to", to);
        json.put("subject", subject);
        json.put("body", body);
        return json;
    }
}
