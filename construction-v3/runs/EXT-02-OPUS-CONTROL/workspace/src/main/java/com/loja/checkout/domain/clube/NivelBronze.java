package com.loja.checkout.domain.clube;

import com.loja.checkout.domain.pedido.Dinheiro;
import com.loja.checkout.domain.pedido.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** So o cadastro: nao ganha nada. */
@Component
public class NivelBronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal creditoProximaCompra(Pedido pedido) {
        return Dinheiro.ZERO;
    }
}
