package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Carrinho;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class FreteGratis implements Cupom {

    public String codigo() {
        return "FRETEGRATIS";
    }

    public boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return frete;
    }
}
