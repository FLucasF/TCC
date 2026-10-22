package br.com.marealta.messaging;

public record StoredMessage(long id, Channel channel, String to, String subject, String body, MessageStatus status) {}
