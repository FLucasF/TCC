package loja.checkout.cupom;

import java.math.BigDecimal;
import loja.checkout.dominio.Carrinho;
import org.springframework.stereotype.Component;

@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return frete;
    }
}
