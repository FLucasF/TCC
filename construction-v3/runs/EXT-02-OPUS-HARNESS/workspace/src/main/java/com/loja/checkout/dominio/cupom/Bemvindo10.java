package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Bemvindo10 implements Cupom {

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(BaseCupom base) {
        return Dinheiro.percentual(base.subtotalProdutos(), "10");
    }
}
