package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.calculo.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class PagamentoCartao implements FormaPagamento {

    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = new MathContext(20, RoundingMode.HALF_EVEN);

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return semJuros(totalPedido, parcelas);
        }
        return comJuros(totalPedido, parcelas);
    }

    @Override
    public boolean parcelamentoPermitido(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    private Cobranca semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorParcela = Dinheiro.centavos(
                totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
        return new Cobranca(Dinheiro.centavos(totalPedido), valorParcela);
    }

    /** Tabela Price: parcela = total x taxa / (1 - (1 + taxa)^-parcelas). */
    private Cobranca comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, PRECISAO));
        BigDecimal valorParcela = Dinheiro.centavos(
                totalPedido.multiply(TAXA_MENSAL).divide(divisor, PRECISAO));
        BigDecimal totalFinal = Dinheiro.centavos(
                valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        return new Cobranca(totalFinal, valorParcela);
    }
}
