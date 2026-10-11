package com.loja.checkout.cupom;

import com.loja.checkout.Arredondamento;
import com.loja.checkout.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class BemVindo10 implements Cupom {

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        return Arredondamento.centavos(subtotalProdutos.multiply(new BigDecimal("0.10")));
    }
}
