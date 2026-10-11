package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.ItemPedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CupomLeve3Pague2 implements Cupom {

    private static final int UNIDADES_POR_BRINDE = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.centavos(contexto.pedido().itens().stream()
                .map(this::unidadesGratis)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal unidadesGratis(ItemPedido item) {
        int gratis = item.quantidade() / UNIDADES_POR_BRINDE;
        return item.precoUnitario().multiply(BigDecimal.valueOf(gratis));
    }
}
