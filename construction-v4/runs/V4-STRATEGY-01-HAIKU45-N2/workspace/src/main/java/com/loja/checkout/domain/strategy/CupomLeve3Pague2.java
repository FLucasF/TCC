package com.loja.checkout.domain.strategy;

import com.loja.checkout.dto.Item;
import com.loja.checkout.util.Arredondador;
import java.util.List;

public class CupomLeve3Pague2 implements CupomAplicador {
    @Override
    public double calcularDesconto(List<Item> itens, double subtotal, double frete) {
        double desconto = 0;
        for (Item item : itens) {
            int itensGratis = item.getQuantidade() / 3;
            desconto += itensGratis * item.getPrecoUnitario();
        }
        return Arredondador.arredondarParaCentavos(desconto);
    }

    @Override
    public boolean ehAplicavel(List<Item> itens, double subtotal, double frete) {
        return true;
    }
}
