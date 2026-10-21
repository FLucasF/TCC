package br.com.marealta.dominio;

import java.math.BigDecimal;

public enum TipoQuarto {
    STANDARD(2, "249.90"),
    DELUXE(3, "389.90"),
    SUITE(4, "579.90");

    private final int capacidade;
    private final BigDecimal diaria;

    TipoQuarto(int capacidade, String diaria) {
        this.capacidade = capacidade;
        this.diaria = Dinheiro.valor(diaria);
    }

    public int capacidade() {
        return capacidade;
    }

    public BigDecimal diaria() {
        return diaria;
    }
}
