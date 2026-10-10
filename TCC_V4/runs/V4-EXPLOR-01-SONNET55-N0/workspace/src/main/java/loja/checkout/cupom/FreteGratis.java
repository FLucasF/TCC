package loja.checkout.cupom;

import java.math.BigDecimal;
import loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

@Component
class FreteGratis implements Cupom {
    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return frete;
    }
}
