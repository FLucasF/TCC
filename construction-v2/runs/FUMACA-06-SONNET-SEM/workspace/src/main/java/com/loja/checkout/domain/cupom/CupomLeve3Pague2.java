package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.ItemPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CupomLeve3Pague2 implements Cupom {

    private static final int TAMANHO_LEVA = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            int unidadesGratis = item.quantidade() / TAMANHO_LEVA;
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
        }
        return Dinheiro.arredondar(desconto);
    }
}
