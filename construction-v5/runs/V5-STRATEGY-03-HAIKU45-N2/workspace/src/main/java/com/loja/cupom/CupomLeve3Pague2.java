package com.loja.cupom;

import com.loja.model.ItemPedido;
import com.loja.util.Arredondamento;
import java.math.BigDecimal;
import java.util.List;

public class CupomLeve3Pague2 implements Cupom {
    @Override
    public BigDecimal calcularDesconto(List<ItemPedido> itens, BigDecimal subtotal, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (ItemPedido item : itens) {
            int unidadesGratis = item.getQuantidade() / 3;
            BigDecimal descontoItem = BigDecimal.valueOf(item.getPrecoUnitario())
                    .multiply(BigDecimal.valueOf(unidadesGratis));
            desconto = desconto.add(descontoItem);
        }

        return Arredondamento.arredondarParaPar(desconto);
    }

    @Override
    public boolean podeAplicar(List<ItemPedido> itens, BigDecimal subtotal) {
        return true;
    }

    @Override
    public String obterCodigo() {
        return "LEVE3PAGUE2";
    }
}
