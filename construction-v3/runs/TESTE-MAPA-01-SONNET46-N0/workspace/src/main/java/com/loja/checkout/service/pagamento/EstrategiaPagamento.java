package com.loja.checkout.service.pagamento;

import java.math.BigDecimal;

public interface EstrategiaPagamento {
    String getCodigo();
    boolean aceitaParcelamento(int parcelas);
    boolean atendePedido(BigDecimal total);
    ResultadoPagamento calcular(BigDecimal total, int parcelas);
}
