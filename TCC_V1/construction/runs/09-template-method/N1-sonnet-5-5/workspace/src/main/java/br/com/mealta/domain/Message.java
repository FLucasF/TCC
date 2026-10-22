package br.com.mealta.domain;

public record Message(Long id, Channel channel, String to, String subject, String body) {
}
