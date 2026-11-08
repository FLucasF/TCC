package com.pousadamarealta.domain;

public enum MessageChannel {
  EMAIL("EMAIL");

  public final String value;

  MessageChannel(String value) {
    this.value = value;
  }

  public static MessageChannel from(String value) {
    for (MessageChannel channel : MessageChannel.values()) {
      if (channel.value.equals(value)) {
        return channel;
      }
    }
    throw new IllegalArgumentException("Unknown channel: " + value);
  }
}
