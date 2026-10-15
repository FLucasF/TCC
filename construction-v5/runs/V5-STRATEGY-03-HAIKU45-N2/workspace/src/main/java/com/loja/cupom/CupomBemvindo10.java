package com.loja.cupom;

import com.loja.model.ItemPedido;
import com.loja.util.Arredondamento;
import java.math.BigDecimal;
import java.util.List;

public class CupomBemvindo10 implements Cupom {
    @Override
    public BigDecimal calcularDesconto(List<ItemPedido> itens, BigDecimal subtotal, BigDecimal frete) {
        BigDecimal desconto = subtotal.multiply(BigDecimal.valueOf(0.10));
        return Arredondamento.arredondarParaPar(desconto);
    }

    @Override
    public boolean podeAplicar(List<ItemPedido> itens, BigDecimal subtotal) {
        return true;
    }

    @Override
    public String obterCodigo() {
        return "BEMVINDO10";
    }
}
