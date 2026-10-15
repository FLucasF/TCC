package loja.checkout.pagamento;

import java.math.BigDecimal;
import java.math.MathContext;

import org.springframework.stereotype.Component;

import loja.checkout.Dinheiro;

@Component
public class Cartao implements FormaPagamento {
    private static final int MAXIMO_PARCELAS = 12;
    private static final int MAXIMO_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        return parcelas <= MAXIMO_SEM_JUROS ? semJuros(totalPedido, parcelas) : comJuros(totalPedido, parcelas);
    }

    private Cobranca semJuros(BigDecimal total, int parcelas) {
        BigDecimal parcela = Dinheiro.arredondar(total.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
        return new Cobranca(total, parcela);
    }

    /** Tabela Price: total × taxa ÷ (1 − (1 + taxa)^−n). */
    private Cobranca comJuros(BigDecimal total, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
        BigDecimal descontado = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MathContext.DECIMAL128));
        BigDecimal parcela = Dinheiro.arredondar(
                total.multiply(TAXA_MENSAL).divide(descontado, MathContext.DECIMAL128));
        return new Cobranca(parcela.multiply(BigDecimal.valueOf(parcelas)), parcela);
    }
}
