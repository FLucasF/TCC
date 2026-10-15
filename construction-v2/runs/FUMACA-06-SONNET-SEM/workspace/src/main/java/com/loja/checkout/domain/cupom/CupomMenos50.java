package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.ItemPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CupomMenos50 implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal SUBTOTAL_MINIMO = new BigDecimal("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public void validarAplicavel(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        if (subtotalProdutos.compareTo(SUBTOTAL_MINIMO) < 0) {
            naoAplicavel();
        }
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        return DESCONTO;
    }
}
