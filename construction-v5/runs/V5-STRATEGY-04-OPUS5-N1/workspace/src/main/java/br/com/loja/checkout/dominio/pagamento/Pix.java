package br.com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

import br.com.loja.checkout.dominio.Dinheiro;

final class Pix implements FormaPagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = Dinheiro.de("5");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.percentual(PERCENTUAL_DESCONTO, totalPedido);
        BigDecimal totalFinal = Dinheiro.centavos(totalPedido.subtract(desconto));
        return new Cobranca(totalFinal, 1, totalFinal);
    }
}
