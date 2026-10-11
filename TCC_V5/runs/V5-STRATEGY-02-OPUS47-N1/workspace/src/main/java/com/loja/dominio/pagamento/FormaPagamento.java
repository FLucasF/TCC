package com.loja.dominio.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    boolean parcelasValidas(int parcelas);
    boolean atende(BigDecimal totalPedido);
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
