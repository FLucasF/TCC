package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

@Component
public class Cartao implements FormaPagamento {

    private static final int MAX_PARCELAS = 12;
    private static final int MAX_PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    public String codigo() {
        return "CARTAO";
    }

    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    public boolean aceitaTotal(BigDecimal totalPedido) {
        return true;
    }

    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_PARCELAS_SEM_JUROS) {
            return new Cobranca(totalPedido, Dinheiro.arredondar(totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128)));
        }
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, MathContext.DECIMAL128);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MathContext.DECIMAL128));
        BigDecimal parcela = Dinheiro.arredondar(totalPedido.multiply(TAXA_MENSAL).divide(divisor, MathContext.DECIMAL128));
        return new Cobranca(Dinheiro.arredondar(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
    }
}
