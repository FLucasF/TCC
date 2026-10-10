package com.loja.checkout.cupom;

import com.loja.checkout.api.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        return itens.stream()
                .map(item -> {
                    int gratis = item.quantidade() / 3;
                    return BigDecimal.valueOf(item.precoUnitario()).multiply(BigDecimal.valueOf(gratis));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
