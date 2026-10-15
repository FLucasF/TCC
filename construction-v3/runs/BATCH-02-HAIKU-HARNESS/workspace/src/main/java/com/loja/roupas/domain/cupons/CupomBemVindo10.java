package com.loja.roupas.domain.cupons;

import com.loja.roupas.domain.Cupom;
import com.loja.roupas.domain.ItemPedido;
import java.math.BigDecimal;
import java.util.List;

public class CupomBemVindo10 implements Cupom {
    @Override
    public boolean podeAplicar(BigDecimal subtotal, Double pesoTotal, List<ItemPedido> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, Double pesoTotal, BigDecimal frete, List<ItemPedido> itens) {
        return subtotal.multiply(new BigDecimal("0.10"));
    }
}
