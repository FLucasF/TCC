package com.loja.checkout.pagamento;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Pix implements FormaPagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("5");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Pagamento pagar(BigDecimal totalPedido, int parcelas) {
        return Pagamento.aVista(totalPedido.subtract(Dinheiro.percentual(totalPedido, PERCENTUAL_DESCONTO)));
    }
}
