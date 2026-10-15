package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemPedido;
import java.util.List;

public class Menos50 implements Cupom {
    @Override
    public boolean aplicavel(double subtotalProdutos, List<ItemPedido> itens) {
        return subtotalProdutos >= 300.00;
    }

    @Override
    public ResultadoCupom calcular(double subtotalProdutos, double frete, List<ItemPedido> itens) {
        return new ResultadoCupom(50.00);
    }
}
