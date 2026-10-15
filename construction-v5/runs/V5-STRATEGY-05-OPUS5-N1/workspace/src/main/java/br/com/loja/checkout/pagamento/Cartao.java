package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

@Component
public class Cartao implements FormaPagamento {

    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return semJuros(totalPedido, parcelas);
        }
        return comJuros(totalPedido, parcelas);
    }

    private Cobranca semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorParcela = Dinheiro.centavos(
                totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL64));
        return new Cobranca(totalPedido, valorParcela);
    }

    /** Tabela Price: parcela = total x taxa / (1 - (1 + taxa)^-parcelas). */
    private Cobranca comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
        BigDecimal valorParcela = Dinheiro.centavos(totalPedido.multiply(TAXA_MENSAL).multiply(fator)
                .divide(fator.subtract(BigDecimal.ONE), MathContext.DECIMAL64));
        BigDecimal totalFinal = Dinheiro.centavos(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        return new Cobranca(totalFinal, valorParcela);
    }
}
