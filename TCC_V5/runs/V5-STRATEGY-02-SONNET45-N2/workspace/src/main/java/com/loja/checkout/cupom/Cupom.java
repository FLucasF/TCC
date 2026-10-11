package com.loja.checkout.cupom;

import com.loja.checkout.model.Pedido;
import com.loja.checkout.model.Resumo;
import java.math.BigDecimal;

public interface Cupom {
    BigDecimal calcularDesconto(Pedido pedido, Resumo resumo);
    void validarAplicabilidade(Pedido pedido);
}
