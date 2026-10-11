package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemPedido;
import java.util.List;

public class BemVindo10 implements Cupom {
    @Override
    public boolean aplicavel(double subtotalProdutos, List<ItemPedido> itens) {
        return true;
    }

    @Override
    public ResultadoCupom calcular(double subtotalProdutos, double frete, List<ItemPedido> itens) {
        double desconto = arredondar(subtotalProdutos * 0.10);
        return new ResultadoCupom(desconto);
    }

    private static double arredondar(double valor) {
        return Math.rint(valor * 100) / 100;
    }
}
