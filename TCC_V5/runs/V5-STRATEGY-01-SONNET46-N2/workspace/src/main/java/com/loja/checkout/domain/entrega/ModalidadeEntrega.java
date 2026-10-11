package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    BigDecimal calcularFrete(BigDecimal pesoTotal);

    int prazoEntregaDias();

    /**
     * Verifica se a modalidade está disponível para o pedido.
     * Lança CheckoutException com MODALIDADE_INDISPONIVEL se não atender.
     */
    void validarDisponibilidade(BigDecimal pesoTotal);
}
