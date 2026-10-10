package com.loja.checkout.service.cupom;

import com.loja.checkout.dto.ItemPedidoRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class Menos50Cupom implements CalculadoraCupom {

    private static final BigDecimal VALOR_DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal SUBTOTAL_MINIMO = new BigDecimal("300.00");

    @Override
    public String getCodigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(SUBTOTAL_MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        return VALOR_DESCONTO;
    }
}
