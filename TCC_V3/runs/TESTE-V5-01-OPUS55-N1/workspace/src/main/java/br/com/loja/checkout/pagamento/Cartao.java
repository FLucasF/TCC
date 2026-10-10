package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.pedido.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

@Component
class Cartao implements FormaPagamento {

    private static final int MAXIMO_PARCELAS = 12;
    private static final int MAXIMO_SEM_JUROS = 3;
    private static final BigDecimal JUROS_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public Pagamento pagar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAXIMO_SEM_JUROS) {
            BigDecimal parcela = Dinheiro.arredondar(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
            return new Pagamento(totalPedido, parcelas, parcela);
        }
        BigDecimal parcela = parcelaPrice(totalPedido, parcelas);
        return new Pagamento(parcela.multiply(BigDecimal.valueOf(parcelas)), parcelas, parcela);
    }

    /** Tabela Price: total × taxa ÷ (1 − (1 + taxa)^−n), arredondada para centavos. */
    private static BigDecimal parcelaPrice(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(JUROS_MENSAL).pow(parcelas);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MathContext.DECIMAL128));
        return Dinheiro.arredondar(totalPedido.multiply(JUROS_MENSAL).divide(divisor, MathContext.DECIMAL128));
    }
}
