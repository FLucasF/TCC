package br.com.marealta.dominio;

import java.math.BigDecimal;

public record Quarto(int numero, TipoQuarto tipo) {

    public int capacidade() {
        return tipo.capacidade();
    }

    public BigDecimal diaria() {
        return tipo.diaria();
    }
}
