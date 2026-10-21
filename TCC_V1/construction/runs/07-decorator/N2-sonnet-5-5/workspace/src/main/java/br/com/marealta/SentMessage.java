package br.com.marealta;

public record SentMessage(int id, Channel channel, String to, String subject, String body, MessageStatus status) {
}
