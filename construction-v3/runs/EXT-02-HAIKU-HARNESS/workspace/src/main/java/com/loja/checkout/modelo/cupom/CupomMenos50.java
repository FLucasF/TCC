package com.loja.checkout.modelo.cupom;

import com.loja.checkout.dto.ItemPedidoDTO;
import java.math.BigDecimal;
import java.util.List;

public class CupomMenos50 implements Cupom {
    @Override
    public BigDecimal calcularDesconto(List<ItemPedidoDTO> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        return new BigDecimal("50.00");
    }

    @Override
    public boolean ehAplicavel(List<ItemPedidoDTO> itens, BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
    }
}
