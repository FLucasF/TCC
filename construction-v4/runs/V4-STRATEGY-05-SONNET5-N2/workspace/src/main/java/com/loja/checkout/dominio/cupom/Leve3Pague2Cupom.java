package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.ItemPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Leve3Pague2Cupom implements Cupom {

    private static final int TAMANHO_LOTE = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return carrinho.itens().stream()
                .map(this::descontoDoItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal descontoDoItem(ItemPedido item) {
        int unidadesGratis = item.quantidade() / TAMANHO_LOTE;
        return item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis));
    }
}
