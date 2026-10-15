package br.com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

final class Economica implements ModalidadeEntrega {

    private static final FretePorPeso COBRANCA = new FretePorPeso("12.00", "2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
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
        return 7;
    }
}
