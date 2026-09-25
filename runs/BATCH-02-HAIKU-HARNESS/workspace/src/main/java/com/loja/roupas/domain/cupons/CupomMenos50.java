package com.loja.roupas.domain.cupons;

import com.loja.roupas.domain.Cupom;
import com.loja.roupas.domain.ItemPedido;
import java.math.BigDecimal;
import java.util.List;

public class CupomMenos50 implements Cupom {
    @Override
    public boolean podeAplicar(BigDecimal subtotal, Double pesoTotal, List<ItemPedido> itens) {
        return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, Double pesoTotal, BigDecimal frete, List<ItemPedido> itens) {
        return new BigDecimal("50.00");
    }
}
