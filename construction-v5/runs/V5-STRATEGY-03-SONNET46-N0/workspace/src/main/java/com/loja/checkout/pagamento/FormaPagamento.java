package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {

    String getCodigo();

    void validarParcelas(int parcelas);

    void validarLimite(BigDecimal totalPedido);

    ResultadoPagamento calcular(int parcelas, BigDecimal totalPedido);
}
