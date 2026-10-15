package com.loja.cupom;

import com.loja.domain.Pedido;
import java.math.BigDecimal;

public interface Cupom {
    boolean aplicavel(Pedido pedido, BigDecimal frete);
    BigDecimal calcularDesconto(Pedido pedido, BigDecimal frete);
}
