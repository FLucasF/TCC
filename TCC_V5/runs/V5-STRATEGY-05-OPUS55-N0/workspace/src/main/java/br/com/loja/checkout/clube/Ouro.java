package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Ouro implements NivelClube {

    private static final BigDecimal CREDITO = new BigDecimal("0.05");
    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal percentualCredito() {
        return CREDITO;
    }

    @Override
    public boolean isentaFrete() {
        return true;
    }

    @Override
    public boolean ganhaBrinde(Pedido pedido) {
        return pedido.subtotalProdutos().compareTo(MINIMO_BRINDE) > 0;
    }
}
