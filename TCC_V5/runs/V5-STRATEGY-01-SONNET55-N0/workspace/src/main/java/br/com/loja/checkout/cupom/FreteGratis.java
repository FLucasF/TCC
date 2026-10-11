package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Carrinho;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class FreteGratis implements Cupom {

    public String codigo() {
        return "FRETEGRATIS";
    }

    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return frete;
    }
}
