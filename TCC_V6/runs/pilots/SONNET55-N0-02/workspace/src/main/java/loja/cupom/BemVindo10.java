package loja.cupom;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Component;

import loja.dominio.Dinheiro;

@Component
public class BemVindo10 implements Cupom {

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public Optional<BigDecimal> desconto(ContextoCupom ctx) {
        if (!ctx.primeiraCompra().getAsBoolean()) {
            return Optional.empty();
        }
        return Optional.of(Dinheiro.percentual(ctx.subtotalProdutos(), "0.10"));
    }
}
