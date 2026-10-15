package com.loja.cupom;

import com.loja.domain.Pedido;
import java.math.BigDecimal;

public class SemCupom implements Cupom {

    @Override
    public boolean aplicavel(Pedido pedido, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(Pedido pedido, BigDecimal frete) {
        return BigDecimal.ZERO.setScale(2);
    }
}
