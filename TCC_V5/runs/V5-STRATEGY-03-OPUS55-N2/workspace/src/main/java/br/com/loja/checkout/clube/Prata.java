package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Prata implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal freteCobrado(BigDecimal freteModalidade) {
        return freteModalidade;
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, PERCENTUAL_CREDITO);
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
