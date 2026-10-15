package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    boolean isParcelasValidas(int parcelas);
    boolean estaDisponivel(BigDecimal totalPedido);
    BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas);
    BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas);
}
