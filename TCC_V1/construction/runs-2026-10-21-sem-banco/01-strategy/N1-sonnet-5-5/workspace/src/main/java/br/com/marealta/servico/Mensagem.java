package br.com.marealta.servico;

public record Mensagem(int id, String channel, String to, String subject, String body) {
}
