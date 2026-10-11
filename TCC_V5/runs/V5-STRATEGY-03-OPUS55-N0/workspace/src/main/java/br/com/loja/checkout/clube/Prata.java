package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Prata implements NivelClube {

    private static final BigDecimal TAXA_CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal credito(Pedido pedido) {
        return Dinheiro.percentual(pedido.subtotalProdutos(), TAXA_CREDITO);
    }
}
