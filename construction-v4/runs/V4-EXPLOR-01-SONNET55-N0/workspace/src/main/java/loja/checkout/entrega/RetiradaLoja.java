package loja.checkout.entrega;

import java.math.BigDecimal;
import loja.checkout.dominio.Dinheiro;
import loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

@Component
class RetiradaLoja implements OpcaoEntrega {
    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public int prazoDias() {
        return 1;
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.ZERO;
    }
}
