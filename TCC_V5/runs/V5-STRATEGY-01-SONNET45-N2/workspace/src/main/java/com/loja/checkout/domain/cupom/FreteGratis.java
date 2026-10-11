package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemPedido;
import java.util.List;

public class FreteGratis implements Cupom {
    @Override
    public boolean aplicavel(double subtotalProdutos, List<ItemPedido> itens) {
        return true;
    }

    @Override
    public ResultadoCupom calcular(double subtotalProdutos, double frete, List<ItemPedido> itens) {
        return new ResultadoCupom(frete, frete);
    }
}
