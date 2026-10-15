package br.tcc.checkout.dominio.entrega;

import java.math.BigDecimal;

public class ModalidadeRetiradaLoja implements ModalidadeEntrega {
    @Override
    public String getCodigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public int getPrazo() {
        return 1;
    }

    @Override
    public boolean ehDisponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return new BigDecimal("0.00");
    }
}
