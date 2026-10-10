package com.loja.strategy.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    boolean isDisponivel(BigDecimal totalPedido);
    boolean isParcelamentoValido(int parcelas);
    BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas);
    BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas);
}
