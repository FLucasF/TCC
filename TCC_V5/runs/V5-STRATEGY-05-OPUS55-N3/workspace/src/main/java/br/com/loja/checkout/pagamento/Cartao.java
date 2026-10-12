package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;
import java.math.MathContext;

import org.springframework.stereotype.Component;

import br.com.loja.checkout.Dinheiro;

@Component
public class Cartao implements FormaPagamento {

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
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Pagamento pagar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAXIMO_SEM_JUROS) {
            BigDecimal parcela = Dinheiro.centavos(totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
            return new Pagamento(totalPedido, parcela);
        }
        BigDecimal parcela = Dinheiro.centavos(parcelaPrice(totalPedido, parcelas));
        return new Pagamento(parcela.multiply(BigDecimal.valueOf(parcelas)), parcela);
    }

    /** Tabela Price: total × taxa ÷ (1 − (1 + taxa)^−n). */
    private static BigDecimal parcelaPrice(BigDecimal total, int parcelas) {
        MathContext mc = MathContext.DECIMAL128;
        BigDecimal fator = BigDecimal.ONE.add(JUROS_MENSAL).pow(-parcelas, mc);
        return total.multiply(JUROS_MENSAL).divide(BigDecimal.ONE.subtract(fator), mc);
    }
}
