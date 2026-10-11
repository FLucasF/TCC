package com.loja.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    boolean aceitaParcelas(int parcelas);
    boolean aceita(BigDecimal totalPedido);
    BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas);
    int numeroParcelas(int parcelasInformadas);
    BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas);
}
