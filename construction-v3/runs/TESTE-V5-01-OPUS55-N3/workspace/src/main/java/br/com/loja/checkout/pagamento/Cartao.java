package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.Dinheiro;
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
        if (parcelas <= MAXIMO_SEM_JUROS) {
            BigDecimal parcela = totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128);
            return new Cobranca(totalPedido, Dinheiro.centavos(parcela));
        }
        BigDecimal parcela = parcelaPrice(totalPedido, parcelas);
        return new Cobranca(parcela.multiply(BigDecimal.valueOf(parcelas)), parcela);
    }

    /** Tabela Price: total × taxa ÷ (1 − (1 + taxa)^−n). */
    private static BigDecimal parcelaPrice(BigDecimal total, int parcelas) {
        MathContext mc = MathContext.DECIMAL128;
        BigDecimal descontoComposto = BigDecimal.ONE.divide(BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, mc), mc);
        return Dinheiro.centavos(total.multiply(TAXA_MENSAL).divide(BigDecimal.ONE.subtract(descontoComposto), mc));
    }
}
