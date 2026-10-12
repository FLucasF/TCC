package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal VALOR = new BigDecimal("18.00");
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
    public BigDecimal calcularFrete(Pedido pedido) {
        return VALOR;
    }

    @Override
    public boolean atende(Pedido pedido) {
        return pedido.pesoKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }
}
