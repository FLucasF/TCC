package com.loja.checkout.modelo.cupom;

import com.loja.checkout.dto.ItemPedidoDTO;
import java.math.BigDecimal;
import java.util.List;

public class CupomFreteGratis implements Cupom {
    @Override
    public BigDecimal calcularDesconto(List<ItemPedidoDTO> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        return frete;
    }

    @Override
    public boolean ehAplicavel(List<ItemPedidoDTO> itens, BigDecimal subtotalProdutos) {
        return true;
    }
}
