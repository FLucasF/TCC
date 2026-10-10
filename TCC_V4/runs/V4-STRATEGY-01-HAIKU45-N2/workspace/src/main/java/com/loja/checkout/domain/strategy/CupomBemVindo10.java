package com.loja.checkout.domain.strategy;

import com.loja.checkout.dto.Item;
import com.loja.checkout.util.Arredondador;
import java.util.List;

public class CupomBemVindo10 implements CupomAplicador {
    @Override
    public double calcularDesconto(List<Item> itens, double subtotal, double frete) {
        double desconto = subtotal * 0.10;
        return Arredondador.arredondarParaCentavos(desconto);
    }

    @Override
    public boolean ehAplicavel(List<Item> itens, double subtotal, double frete) {
        return true;
    }
}
