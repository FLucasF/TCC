package com.loja.checkout.service.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    String getCodigo();
    boolean aceita(BigDecimal totalPedido, int parcelas);
    boolean aceitaParcelas(int parcelas);
    BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas);
    BigDecimal calcularValorFinal(BigDecimal totalPedido, int parcelas);
    BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas);
}
