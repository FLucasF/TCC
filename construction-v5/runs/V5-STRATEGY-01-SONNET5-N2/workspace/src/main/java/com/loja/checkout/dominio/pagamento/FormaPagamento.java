package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {

    String codigo();

    boolean parcelasValidas(int parcelas);

    boolean disponivel(BigDecimal totalPedido, int parcelas);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
