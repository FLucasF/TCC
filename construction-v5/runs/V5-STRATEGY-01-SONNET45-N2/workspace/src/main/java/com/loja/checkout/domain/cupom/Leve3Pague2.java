package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemPedido;
import java.util.List;

public class Leve3Pague2 implements Cupom {
    @Override
    public boolean aplicavel(double subtotalProdutos, List<ItemPedido> itens) {
        return true;
    }

    @Override
    public ResultadoCupom calcular(double subtotalProdutos, double frete, List<ItemPedido> itens) {
        double desconto = 0.0;

        for (ItemPedido item : itens) {
            int quantidade = item.quantidade();
            int unidadesGratis = quantidade / 3;

            if (unidadesGratis > 0) {
                desconto += unidadesGratis * item.precoUnitario();
            }
        }

        return new ResultadoCupom(arredondar(desconto));
    }

    private static double arredondar(double valor) {
        return Math.rint(valor * 100) / 100;
    }
}
