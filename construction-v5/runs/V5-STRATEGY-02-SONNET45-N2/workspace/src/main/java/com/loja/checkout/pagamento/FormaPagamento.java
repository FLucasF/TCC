package com.loja.checkout.pagamento;

import com.loja.checkout.model.Resumo;
import java.math.BigDecimal;

public interface FormaPagamento {
    void validarDisponibilidade(Resumo resumo);
    void validarParcelas(Integer parcelas);
    BigDecimal calcularAjuste(BigDecimal totalPedido, Integer parcelas);
    BigDecimal calcularValorParcela(BigDecimal totalPedido, Integer parcelas);
}
