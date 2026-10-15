package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

public interface Pagamento {
    String codigo();
    boolean parcelasValidas(int parcelas);
    boolean disponivelPara(BigDecimal totalPedido);
    Ajuste calcular(BigDecimal totalPedido, int parcelas);

    record Ajuste(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {}
}
