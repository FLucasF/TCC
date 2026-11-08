package com.marealta.domain;

/** Por onde saem as mensagens. Por enquanto elas só ficam guardadas numa lista. */
public interface MessageOutbox {
    void send(Channel channel, String to, String subject, String body);
}
