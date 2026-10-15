package br.com.loja.pedidos.pagamento;

import java.math.BigDecimal;
import java.math.MathContext;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.comum.Dinheiro;

/** Até 3x sem juros; de 4x a 12x com juros mensais pela tabela Price. */
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
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas, boolean usouCupom) {
        if (parcelas <= MAXIMO_SEM_JUROS) {
            BigDecimal parcela = Dinheiro.centavos(totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
            return new ResultadoPagamento(totalPedido, parcelas, parcela);
        }
        // parcela = total × taxa ÷ (1 − (1 + taxa)^−n)
        BigDecimal descontoFator = BigDecimal.ONE.divide(BigDecimal.ONE.add(JUROS_MENSAL).pow(parcelas), MathContext.DECIMAL128);
        BigDecimal parcela = Dinheiro.centavos(totalPedido.multiply(JUROS_MENSAL)
                .divide(BigDecimal.ONE.subtract(descontoFator), MathContext.DECIMAL128));
        return new ResultadoPagamento(parcela.multiply(BigDecimal.valueOf(parcelas)), parcelas, parcela);
    }
}
