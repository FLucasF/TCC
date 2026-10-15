package com.loja.checkout.modelo.cupom;

import com.loja.checkout.dto.ItemPedidoDTO;
import com.loja.checkout.util.ArredondamentoBancario;
import java.math.BigDecimal;
import java.util.List;

public class CupomBemVindo10 implements Cupom {
    @Override
    public BigDecimal calcularDesconto(List<ItemPedidoDTO> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        return ArredondamentoBancario.arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
    }

    @Override
    public boolean ehAplicavel(List<ItemPedidoDTO> itens, BigDecimal subtotalProdutos) {
        return true;
    }
}
