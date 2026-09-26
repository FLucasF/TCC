package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;
import java.util.List;

public class CupomFrteGratis implements Cupom {
    private BigDecimal frete = BigDecimal.ZERO;

    public void definirFrete(BigDecimal frete) {
        this.frete = frete;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemCupom> itens) {
        return frete;
    }

    @Override
    public boolean podeAplicar(BigDecimal subtotal, List<ItemCupom> itens) {
        return true;
    }

    @Override
    public String obterCodigo() {
        return "FRETEGRATIS";
    }
}
