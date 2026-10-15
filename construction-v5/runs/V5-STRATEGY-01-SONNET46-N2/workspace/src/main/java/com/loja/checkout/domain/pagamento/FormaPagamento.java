package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {

    /**
     * Valida parcelas e restrições da forma de pagamento.
     * Lança CheckoutException com o código adequado se inválido.
     */
    void validar(int parcelas, BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
