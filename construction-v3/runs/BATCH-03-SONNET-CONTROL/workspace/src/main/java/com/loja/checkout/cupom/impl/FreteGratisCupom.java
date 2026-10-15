package com.loja.checkout.cupom.impl;

import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.domain.PedidoContexto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FreteGratisCupom implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(PedidoContexto pedido, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(PedidoContexto pedido, BigDecimal frete) {
        return frete;
    }
}
