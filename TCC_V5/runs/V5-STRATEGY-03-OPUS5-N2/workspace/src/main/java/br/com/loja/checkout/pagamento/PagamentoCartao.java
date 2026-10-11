package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

@Component
public class PagamentoCartao implements FormaPagamento {

    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = MathContext.DECIMAL128;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelamentoPermitido(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas) {
        return parcelas <= PARCELAS_SEM_JUROS
                ? semJuros(totalPedido, parcelas)
                : comJuros(totalPedido, parcelas);
    }

    private ResultadoPagamento semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Dinheiro.centavos(totalPedido);
        BigDecimal parcela = Dinheiro.centavos(
                totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
        return new ResultadoPagamento(totalFinal, parcela);
    }

    /** Tabela Price: parcela = total x taxa / (1 - (1 + taxa)^-parcelas). */
    private ResultadoPagamento comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal divisor = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(umMaisTaxa.pow(parcelas, PRECISAO), PRECISAO));
        BigDecimal parcela = Dinheiro.centavos(
                totalPedido.multiply(TAXA_MENSAL).divide(divisor, PRECISAO));
        BigDecimal totalFinal = Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, parcela);
    }
}
