package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

@Component
class Cartao implements FormaPagamento {

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

    private static Cobranca semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal parcela = Dinheiro.centavos(totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
        return new Cobranca(totalPedido, parcela);
    }

    /** Tabela Price: parcela = total × taxa ÷ (1 − (1 + taxa)^−n). */
    private static Cobranca comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, MathContext.DECIMAL128);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MathContext.DECIMAL128));
        BigDecimal parcela = Dinheiro.centavos(totalPedido.multiply(TAXA_MENSAL).divide(divisor, MathContext.DECIMAL128));
        return new Cobranca(parcela.multiply(BigDecimal.valueOf(parcelas)), parcela);
    }
}
