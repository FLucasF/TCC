package br.com.loja.pedidos.checkout.pagamento;

import br.com.loja.pedidos.comum.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

/** Cartão de crédito: até 3x sem juros; de 4x a 12x com juros de 1,99% ao mês (tabela Price). */
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
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public CondicaoPagamento condicao(BigDecimal totalPedido, int parcelas, boolean usouCupom) {
        if (parcelas <= MAXIMO_SEM_JUROS) {
            BigDecimal parcela = Dinheiro.arredondar(totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
            return new CondicaoPagamento(totalPedido, parcelas, parcela);
        }
        // parcela = total × taxa ÷ (1 − (1 + taxa)^−n)
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(-parcelas, PRECISAO);
        BigDecimal parcela = Dinheiro.arredondar(
                totalPedido.multiply(TAXA_MENSAL).divide(BigDecimal.ONE.subtract(fator), PRECISAO));
        return new CondicaoPagamento(Dinheiro.arredondar(parcela.multiply(BigDecimal.valueOf(parcelas))), parcelas, parcela);
    }
}
