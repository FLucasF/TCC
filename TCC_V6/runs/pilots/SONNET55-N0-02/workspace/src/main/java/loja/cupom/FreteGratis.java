package loja.cupom;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public Optional<BigDecimal> desconto(ContextoCupom ctx) {
        if (!ctx.entregaTemFrete()) {
            return Optional.empty();
        }
        return Optional.of(ctx.frete());
    }
}
