package br.com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Entrega com valor fixo + valor por kg, prazo fixo e limite opcional de peso. */
public record EntregaPorPeso(String codigo, BigDecimal base, BigDecimal porKg, int prazoDias, BigDecimal pesoMaximoKg)
        implements Entrega {

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return pesoMaximoKg == null || pesoKg.compareTo(pesoMaximoKg) <= 0;
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.arredondar(base.add(porKg.multiply(pesoKg)));
    }
}
