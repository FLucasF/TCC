package com.loja.checkout.coupon;

import com.loja.checkout.PedidoContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FreteGratisCupom implements CupomStrategy {

    @Override
    public String getCodigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(PedidoContext pedido, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(PedidoContext pedido, BigDecimal subtotalProdutos, BigDecimal frete) {
        return frete;
    }
}
