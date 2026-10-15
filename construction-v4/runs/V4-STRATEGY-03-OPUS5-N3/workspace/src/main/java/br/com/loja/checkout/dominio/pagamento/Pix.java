package br.com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.checkout.dominio.Dinheiro;

/** Sempre a vista, com 5% de desconto no total do pedido. */
@Component
public class Pix implements FormaPagamento {

    private static final BigDecimal TAXA_DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(Dinheiro totalPedido) {
        return true;
    }

    @Override
    public Cobranca cobrar(Dinheiro totalPedido, int parcelas) {
        Dinheiro totalFinal = totalPedido.menos(totalPedido.vezes(TAXA_DESCONTO));
        return new Cobranca(totalFinal, totalFinal);
    }
}
