package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class CartaoCredito implements FormaPagamento {
    private static final int MAX_PARCELAS = 12;
    private static final int MAX_PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    public String codigo() { return "CARTAO"; }

    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal n = BigDecimal.valueOf(parcelas);
        if (parcelas <= MAX_PARCELAS_SEM_JUROS) {
            return new Cobranca(totalPedido, totalPedido.divide(n, 2, RoundingMode.HALF_EVEN));
        }
        MathContext mc = MathContext.DECIMAL128;
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, mc);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, mc), mc);
        BigDecimal parcela = Dinheiro.arredondar(totalPedido.multiply(TAXA_MENSAL).divide(divisor, mc));
        return new Cobranca(parcela.multiply(n), parcela);
    }
}
