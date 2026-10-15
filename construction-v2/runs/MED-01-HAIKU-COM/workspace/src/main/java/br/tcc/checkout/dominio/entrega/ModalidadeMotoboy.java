package br.tcc.checkout.dominio.entrega;

import java.math.BigDecimal;

public class ModalidadeMotoboy implements ModalidadeEntrega {
    @Override
    public String getCodigo() {
        return "MOTOBOY";
    }

    @Override
    public int getPrazo() {
        return 0;
    }

    @Override
    public boolean ehDisponivel(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(new BigDecimal("5.00")) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return new BigDecimal("18.00");
    }
}
