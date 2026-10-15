package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.ResultadoPagamento;
import java.math.BigDecimal;

public interface FormaPagamento {
    boolean estaDisponivel(BigDecimal totalPedido);
    void validarParcelas(int parcelas);
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
