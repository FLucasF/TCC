package br.com.loja.checkout.clube;

import br.com.loja.checkout.pedido.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal frete(BigDecimal freteEntrega) {
        return freteEntrega;
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
