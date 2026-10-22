package com.pousada.marealta.domain;

public enum MessageChannel {
    EMAIL;

    public static MessageChannel fromString(String name) {
        for (MessageChannel channel : MessageChannel.values()) {
            if (channel.name().equals(name)) {
                return channel;
            }
        }
        return null;
    }
}
