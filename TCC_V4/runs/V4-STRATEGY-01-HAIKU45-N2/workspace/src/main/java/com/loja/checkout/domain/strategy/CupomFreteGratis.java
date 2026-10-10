package com.loja.checkout.domain.strategy;

import com.loja.checkout.dto.Item;
import com.loja.checkout.util.Arredondador;
import java.util.List;

public class CupomFreteGratis implements CupomAplicador {
    @Override
    public double calcularDesconto(List<Item> itens, double subtotal, double frete) {
        return Arredondador.arredondarParaCentavos(frete);
    }

    @Override
    public boolean ehAplicavel(List<Item> itens, double subtotal, double frete) {
        return true;
    }
}
