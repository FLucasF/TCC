package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Ouro implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal freteCobrado(BigDecimal freteModalidade) {
        return Dinheiro.ZERO;
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, PERCENTUAL_CREDITO);
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
