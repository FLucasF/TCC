package loja.checkout.cupom;

import java.math.BigDecimal;
import loja.checkout.Carrinho;
import org.springframework.stereotype.Component;

@Component
public class FreteGratis implements Cupom {
    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return frete;
    }
}
