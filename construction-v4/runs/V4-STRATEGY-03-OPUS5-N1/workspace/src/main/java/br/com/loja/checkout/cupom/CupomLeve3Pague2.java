package br.com.loja.checkout.cupom;

import br.com.loja.checkout.calculo.Dinheiro;
import br.com.loja.checkout.calculo.Item;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CupomLeve3Pague2 implements Cupom {

    private static final int UNIDADES_PARA_GANHAR_UMA = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        BigDecimal desconto = contexto.itens().stream()
                .map(this::unidadesGratis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.centavos(desconto);
    }

    private BigDecimal unidadesGratis(Item item) {
        int gratis = item.quantidade() / UNIDADES_PARA_GANHAR_UMA;
        return item.precoUnitario().multiply(BigDecimal.valueOf(gratis));
    }
}
