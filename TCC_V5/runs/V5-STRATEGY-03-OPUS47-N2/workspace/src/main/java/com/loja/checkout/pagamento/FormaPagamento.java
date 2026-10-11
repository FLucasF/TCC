package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    void validarParcelas(int parcelas);
    void validar(BigDecimal totalPedido);
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
