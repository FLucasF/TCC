package com.loja.checkout.coupon;

import com.loja.checkout.model.ItemPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class FreteGratis implements Coupon {

    @Override
    public String getCodigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemPedido> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
        return frete;
    }
}
