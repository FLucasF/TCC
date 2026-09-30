package com.loja.checkout.estrategia.pagamento;

import java.math.BigDecimal;

public interface EstrategiaPagamento {
    BigDecimal calcularAjuste(BigDecimal total, int parcelas);
    void validar(int parcelas, BigDecimal totalPedido) throws IllegalArgumentException;
}
