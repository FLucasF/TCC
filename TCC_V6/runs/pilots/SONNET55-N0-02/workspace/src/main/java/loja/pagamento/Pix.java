package loja.pagamento;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import loja.dominio.Dinheiro;

@Component
public class Pix implements FormaPagamento {

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public Cobranca cobrar(BigDecimal total, int parcelas, boolean comCupom) {
        BigDecimal desconto = comCupom ? Dinheiro.ZERO : Dinheiro.percentual(total, "0.05");
        BigDecimal finalPago = Dinheiro.arredondar(total.subtract(desconto));
        return new Cobranca(finalPago, finalPago);
    }
}
