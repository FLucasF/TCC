package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal credito(Pedido pedido) {
        return Dinheiro.ZERO;
    }
}
