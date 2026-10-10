package br.com.loja.checkout.clube;

import br.com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    @Override
    public BigDecimal freteCobrado(BigDecimal frete) {
        return frete;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
