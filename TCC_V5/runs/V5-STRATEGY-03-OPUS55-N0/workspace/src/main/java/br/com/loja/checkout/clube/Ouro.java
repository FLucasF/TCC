package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Ouro implements NivelClube {

    private static final BigDecimal TAXA_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal MINIMO_BRINDE = Dinheiro.reais("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal credito(Pedido pedido) {
        return Dinheiro.percentual(pedido.subtotalProdutos(), TAXA_CREDITO);
    }

    @Override
    public boolean isentoDeFrete() {
        return true;
    }

    @Override
    public boolean ganhaBrinde(Pedido pedido) {
        return pedido.subtotalProdutos().compareTo(MINIMO_BRINDE) > 0;
    }
}
