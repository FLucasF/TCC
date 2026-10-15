package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    void validarParcelas(int parcelas);
    void validarDisponibilidade(BigDecimal total);
    ResultadoPagamento calcular(BigDecimal total, int parcelas);
}
