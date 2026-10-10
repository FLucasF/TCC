package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ContextoPedido;
import java.math.BigDecimal;

public interface Cupom {
    boolean podeAplicar(ContextoPedido contexto, BigDecimal frete);
    BigDecimal calcularDesconto(ContextoPedido contexto, BigDecimal frete);
}
