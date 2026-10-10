package com.loja.checkout.service.cupom;

import com.loja.checkout.dto.ItemPedidoRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class Bemvindo10Cupom implements CalculadoraCupom {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.10");

    @Override
    public String getCodigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        return subtotalProdutos.multiply(PERCENTUAL_DESCONTO);
    }
}
