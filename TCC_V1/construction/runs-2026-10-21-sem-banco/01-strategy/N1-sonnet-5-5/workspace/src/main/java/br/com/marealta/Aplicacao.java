package br.com.marealta;

import br.com.marealta.web.Rotas;

public final class Aplicacao {

    private static final int PORTA_PADRAO = 7070;

    private Aplicacao() {
    }

    public static void main(String[] args) {
        String porta = System.getenv("PORT");
        new Rotas().criar().start(porta == null || porta.isBlank() ? PORTA_PADRAO : Integer.parseInt(porta.trim()));
    }
}
