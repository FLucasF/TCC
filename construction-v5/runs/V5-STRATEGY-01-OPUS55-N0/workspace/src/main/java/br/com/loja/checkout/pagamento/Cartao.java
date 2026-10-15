package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** Até 3x sem juros; de 4x a 12x com juros de 1,99% ao mês (tabela Price). */
@Component
public class Cartao implements FormaPagamento {

    private static final int MAXIMO_PARCELAS = 12;
    private static final int MAXIMO_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = MathContext.DECIMAL128;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelamentoPermitido(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAXIMO_SEM_JUROS) {
            BigDecimal parcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new Cobranca(totalPedido, parcela);
        }
        // parcela = total × taxa ÷ (1 − (1 + taxa)^−n)
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(-parcelas, PRECISAO);
        BigDecimal parcela = Dinheiro.centavos(totalPedido.multiply(TAXA_MENSAL)
                .divide(BigDecimal.ONE.subtract(fator), PRECISAO));
        return new Cobranca(Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
    }
}
