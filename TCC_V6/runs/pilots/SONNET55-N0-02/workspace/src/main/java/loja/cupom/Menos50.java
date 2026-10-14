package loja.cupom;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Component;

import loja.dominio.Dinheiro;

@Component
public class Menos50 implements Cupom {

    private static final BigDecimal MINIMO = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = Dinheiro.arredondar(new BigDecimal("50.00"));

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public Optional<BigDecimal> desconto(ContextoCupom ctx) {
        if (ctx.subtotalProdutos().compareTo(MINIMO) < 0) {
            return Optional.empty();
        }
        return Optional.of(DESCONTO);
    }
}
