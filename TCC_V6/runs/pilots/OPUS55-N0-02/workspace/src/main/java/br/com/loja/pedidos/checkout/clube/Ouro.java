package br.com.loja.pedidos.checkout.clube;

import br.com.loja.pedidos.checkout.entrega.ModalidadeEntrega;
import br.com.loja.pedidos.comum.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** 5% dos produtos em crédito, frete grátis (menos no motoboy) e brinde acima de R$ 500,00 em produtos. */
@Component
public class Ouro implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("5");
    private static final BigDecimal MINIMO_BRINDE = Dinheiro.reais("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, PERCENTUAL_CREDITO);
    }

    @Override
    public boolean freteGratis(ModalidadeEntrega modalidade) {
        return modalidade.aceitaFreteGratisDoClube();
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
