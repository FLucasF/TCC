package com.loja.domain.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas);
    BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas);
    boolean isParcelamentoValido(int parcelas);
    boolean isDisponivel(BigDecimal totalPedido);
}
