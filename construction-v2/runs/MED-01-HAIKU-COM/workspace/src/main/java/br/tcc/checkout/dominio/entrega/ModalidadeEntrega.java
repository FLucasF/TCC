package br.tcc.checkout.dominio.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    String getCodigo();

    int getPrazo();

    boolean ehDisponivel(BigDecimal pesoTotalKg);

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);
}
