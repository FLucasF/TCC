package br.com.marealta.notificacao;

public record Mensagem(int id, Canal canal, String para, String assunto, String texto) {
}
