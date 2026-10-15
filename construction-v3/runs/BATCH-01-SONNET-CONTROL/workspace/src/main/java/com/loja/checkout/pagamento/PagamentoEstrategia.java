package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface PagamentoEstrategia {

    String getCodigo();

    boolean parcelasValidas(int parcelas);

    boolean disponivel(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
