package com.loja.model.cupom;

import com.loja.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class DescontoFixo implements CalculadoraDesconto {
    private final BigDecimal valor;
    private final BigDecimal valorMinimo;

    public DescontoFixo(BigDecimal valor, BigDecimal valorMinimo) {
        this.valor = valor;
        this.valorMinimo = valorMinimo;
    }

    public boolean podeAplicar(BigDecimal subtotal) {
        return subtotal.compareTo(valorMinimo) >= 0;
    }

    @Override
    public BigDecimal calcular(BigDecimal subtotal, List<ItemCarrinho> itens) {
        return valor;
    }

    @Override
    public boolean ehFretegratis() {
        return false;
    }
}
