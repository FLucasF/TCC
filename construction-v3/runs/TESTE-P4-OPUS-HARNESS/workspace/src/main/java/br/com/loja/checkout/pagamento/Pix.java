package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Pix extends AVista {

    private static final BigDecimal TAXA_DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    BigDecimal valorFinal(BigDecimal totalPedido) {
        return Dinheiro.centavos(totalPedido.subtract(Dinheiro.percentual(totalPedido, TAXA_DESCONTO)));
    }
}
