package com.loja.checkout.domain.cupom;

import com.loja.checkout.utils.Arredondamento;
import java.math.BigDecimal;
import java.util.List;

public class CupomBemVindo10 implements Cupom {
    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemCupom> itens) {
        BigDecimal desconto = subtotal.multiply(PERCENTUAL);
        return Arredondamento.arredondarParaCentavos(desconto);
    }

    @Override
    public boolean podeAplicar(BigDecimal subtotal, List<ItemCupom> itens) {
        return true;
    }

    @Override
    public String obterCodigo() {
        return "BEMVINDO10";
    }
}
