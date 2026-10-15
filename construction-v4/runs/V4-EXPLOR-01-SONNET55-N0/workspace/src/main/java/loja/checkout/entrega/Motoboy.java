package loja.checkout.entrega;

import java.math.BigDecimal;
import loja.checkout.dominio.Dinheiro;
import loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

@Component
class Motoboy implements OpcaoEntrega {
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public boolean atende(Pedido pedido) {
        return pedido.pesoKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.arredondar(new BigDecimal("18.00"));
    }
}
