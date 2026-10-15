package br.com.loja.pedidos.checkout.cupom;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** O desconto é o valor do frete. Não vale para retirada na loja. */
@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(PedidoParaCupom pedido) {
        if ("RETIRADA_LOJA".equals(pedido.modalidadeEntrega().codigo())) {
            throw NaoAplicavel.erro();
        }
        return pedido.frete();
    }
}
