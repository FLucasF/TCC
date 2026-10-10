package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface Pagamento {
    BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas);
    BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas);
    boolean aceitaParcelas(int parcelas);
    boolean estaDisponivel(BigDecimal totalPedido);
}
