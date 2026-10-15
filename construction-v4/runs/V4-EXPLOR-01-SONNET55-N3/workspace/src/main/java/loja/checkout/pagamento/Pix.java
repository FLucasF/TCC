package loja.checkout.pagamento;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import loja.checkout.comum.Dinheiro;

@Component
class Pix implements FormaPagamento {
    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public Cobranca cobrar(BigDecimal total, int parcelas) {
        BigDecimal valor = total.subtract(Dinheiro.percentual(total, new BigDecimal("0.05")));
        return new Cobranca(valor, valor);
    }
}
