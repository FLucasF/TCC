package com.loja.cupom;

import com.loja.model.ItemPedido;
import java.math.BigDecimal;
import java.util.List;

public class CupomFreteGratis implements Cupom {
    @Override
    public BigDecimal calcularDesconto(List<ItemPedido> itens, BigDecimal subtotal, BigDecimal frete) {
        return frete;
    }

    @Override
    public boolean podeAplicar(List<ItemPedido> itens, BigDecimal subtotal) {
        return true;
    }

    @Override
    public String obterCodigo() {
        return "FRETEGRATIS";
    }
}
