package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** 10% de desconto no valor dos produtos. */
@Component
public class BemVindo10 implements Cupom {

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.percentual(contexto.subtotalProdutos(), BigDecimal.TEN);
    }
}
