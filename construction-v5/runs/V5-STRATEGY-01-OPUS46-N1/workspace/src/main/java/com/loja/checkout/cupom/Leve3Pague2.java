package com.loja.checkout.cupom;

import com.loja.checkout.Arredondamento;
import com.loja.checkout.ItemRequest;
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
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int gratuitos = item.quantidade() / 3;
            desconto = desconto.add(
                    item.precoUnitario().multiply(BigDecimal.valueOf(gratuitos))
            );
        }
        return Arredondamento.centavos(desconto);
    }
}
