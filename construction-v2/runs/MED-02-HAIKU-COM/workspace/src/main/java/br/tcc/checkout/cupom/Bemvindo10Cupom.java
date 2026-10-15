package br.tcc.checkout.cupom;

import java.math.BigDecimal;
import br.tcc.checkout.util.Arredondador;

public class Bemvindo10Cupom implements Cupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete) {
        return Arredondador.arredondar(subtotal.multiply(new BigDecimal("0.10")));
    }

    @Override
    public boolean aplicavel(BigDecimal subtotal) {
        return true;
    }
}
