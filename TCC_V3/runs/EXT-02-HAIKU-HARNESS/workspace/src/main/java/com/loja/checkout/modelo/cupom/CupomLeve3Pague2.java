package com.loja.checkout.modelo.cupom;

import com.loja.checkout.dto.ItemPedidoDTO;
import com.loja.checkout.util.ArredondamentoBancario;
import java.math.BigDecimal;
import java.util.List;

public class CupomLeve3Pague2 implements Cupom {
    @Override
    public BigDecimal calcularDesconto(List<ItemPedidoDTO> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (ItemPedidoDTO item : itens) {
            int unidadesGratis = item.getQuantidade() / 3;
            BigDecimal descontoItem = item.getPrecoUnitario().multiply(new BigDecimal(unidadesGratis));
            desconto = desconto.add(descontoItem);
        }

        return ArredondamentoBancario.arredondar(desconto);
    }

    @Override
    public boolean ehAplicavel(List<ItemPedidoDTO> itens, BigDecimal subtotalProdutos) {
        return true;
    }
}
