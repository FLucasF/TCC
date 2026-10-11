package br.com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

final class Expressa implements ModalidadeEntrega {

    private static final FretePorPeso COBRANCA = new FretePorPeso("25.00", "4.50");

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return COBRANCA.calcular(pesoKg);
    }

    @Override
    public int prazoDias() {
        return 2;
    }
}
