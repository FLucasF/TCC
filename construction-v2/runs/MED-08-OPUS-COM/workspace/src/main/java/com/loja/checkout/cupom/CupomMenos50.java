package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CupomMenos50 implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal SUBTOTAL_MINIMO = new BigDecimal("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(BaseDoCupom base) {
        return base.subtotalProdutos().compareTo(SUBTOTAL_MINIMO) >= 0;
    }

    @Override
    public BigDecimal desconto(BaseDoCupom base) {
        return Dinheiro.arredondar(DESCONTO);
    }
}
