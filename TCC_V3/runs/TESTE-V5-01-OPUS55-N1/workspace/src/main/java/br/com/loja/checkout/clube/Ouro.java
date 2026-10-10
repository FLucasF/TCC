package br.com.loja.checkout.clube;

import br.com.loja.checkout.pedido.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Ouro implements NivelClube {

    private static final BigDecimal CREDITO = new BigDecimal("0.05");
    private static final BigDecimal MINIMO_BRINDE = Dinheiro.reais("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal frete(BigDecimal freteEntrega) {
        return Dinheiro.ZERO;
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.aplicarTaxa(subtotalProdutos, CREDITO);
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
