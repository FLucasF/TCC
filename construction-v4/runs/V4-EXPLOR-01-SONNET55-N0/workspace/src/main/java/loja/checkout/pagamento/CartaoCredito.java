package loja.checkout.pagamento;

import java.math.BigDecimal;
import java.math.MathContext;
import loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

@Component
class CartaoCredito implements FormaPagamento {
    private static final int MAX_PARCELAS = 12;
    private static final int MAX_PARCELAS_SEM_JUROS = 3;
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
    public Liquidacao liquidar(BigDecimal totalPedido, int parcelas) {
        BigDecimal n = BigDecimal.valueOf(parcelas);
        if (parcelas <= MAX_PARCELAS_SEM_JUROS) {
            return new Liquidacao(totalPedido, Dinheiro.arredondar(totalPedido.divide(n, MathContext.DECIMAL128)));
        }
        BigDecimal fator = BigDecimal.ONE.add(TAXA).pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MathContext.DECIMAL128));
        BigDecimal parcela = Dinheiro.arredondar(totalPedido.multiply(TAXA).divide(denominador, MathContext.DECIMAL128));
        return new Liquidacao(Dinheiro.arredondar(parcela.multiply(n)), parcela);
    }
}
