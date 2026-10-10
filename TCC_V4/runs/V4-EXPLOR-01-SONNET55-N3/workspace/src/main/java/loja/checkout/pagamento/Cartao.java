package loja.checkout.pagamento;

import java.math.BigDecimal;
import java.math.MathContext;

import org.springframework.stereotype.Component;

import loja.checkout.comum.Dinheiro;

@Component
class Cartao implements FormaPagamento {
    private static final int MAX_SEM_JUROS = 3;
    private static final int MAX_PARCELAS = 12;
    private static final BigDecimal TAXA = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public Cobranca cobrar(BigDecimal total, int parcelas) {
        BigDecimal n = BigDecimal.valueOf(parcelas);
        if (parcelas <= MAX_SEM_JUROS) {
            return new Cobranca(total, Dinheiro.arredondar(total.divide(n, MathContext.DECIMAL128)));
        }
        BigDecimal fator = BigDecimal.ONE.add(TAXA).pow(parcelas, MathContext.DECIMAL128);
        BigDecimal parcela = total.multiply(TAXA).multiply(fator)
                .divide(fator.subtract(BigDecimal.ONE), MathContext.DECIMAL128);
        BigDecimal valorParcela = Dinheiro.arredondar(parcela);
        return new Cobranca(Dinheiro.arredondar(valorParcela.multiply(n)), valorParcela);
    }
}
