package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

public interface CalculoPagamento {

    FormaPagamento forma();

    boolean parcelaPermitida(int parcelas);

    boolean disponivel(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
