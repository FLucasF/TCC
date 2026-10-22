package com.pousada.domain;

public enum MessageChannel {
    EMAIL("EMAIL");

    private final String displayName;

    MessageChannel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
