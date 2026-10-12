package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class FreteGratis implements Cupom {
    public String codigo() { return "FRETEGRATIS"; }

    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return frete;
    }
}
