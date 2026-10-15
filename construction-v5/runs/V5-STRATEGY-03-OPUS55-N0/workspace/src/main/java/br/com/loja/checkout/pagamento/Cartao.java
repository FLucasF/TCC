package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Até 3x sem juros; de 4x a 12x com juros de 1,99% ao mês (tabela Price). */
@Component
public class Cartao implements FormaPagamento {

    private static final int MAXIMO_PARCELAS = 12;
    private static final int MAXIMO_SEM_JUROS = 3;
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public Pagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAXIMO_SEM_JUROS) {
            BigDecimal parcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new Pagamento(totalPedido, parcelas, parcela);
        }
        // parcela = total × taxa ÷ (1 − (1 + taxa)^−n)  ==  total × taxa × f ÷ (f − 1), com f = (1 + taxa)^n
        BigDecimal fator = BigDecimal.ONE.add(TAXA_JUROS).pow(parcelas);
        BigDecimal parcela = Dinheiro.arredondar(totalPedido.multiply(TAXA_JUROS).multiply(fator)
                .divide(fator.subtract(BigDecimal.ONE), MathContext.DECIMAL128));
        return new Pagamento(parcela.multiply(BigDecimal.valueOf(parcelas)), parcelas, parcela);
    }
}
