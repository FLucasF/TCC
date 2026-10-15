package com.loja.checkout.estrategia.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas);
    BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas);
    boolean estaDisponivel(BigDecimal totalPedido);
    boolean parcelamentoValido(int parcelas);
}
