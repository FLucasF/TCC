package loja.cupom;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Component;

import loja.dominio.Dinheiro;
import loja.dominio.LinhaItem;

@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public Optional<BigDecimal> desconto(ContextoCupom ctx) {
        BigDecimal total = Dinheiro.ZERO;
        boolean algumGratis = false;
        for (LinhaItem item : ctx.itens()) {
            int gratis = item.quantidade() / 3;
            if (gratis > 0) {
                algumGratis = true;
                total = total.add(item.preco().multiply(BigDecimal.valueOf(gratis)));
            }
        }
        return algumGratis ? Optional.of(Dinheiro.arredondar(total)) : Optional.empty();
    }
}
