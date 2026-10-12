package com.loja.checkout.pagamento;

import com.loja.checkout.comum.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class Cartao implements FormaPagamento {

    private static final int MAX_PARCELAS = 12;
    private static final int MAX_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_EVEN);

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_SEM_JUROS) {
            return semJuros(totalPedido, parcelas);
        }
        return comJuros(totalPedido, parcelas);
    }

    /** Até 3x: o valor final é o próprio total; a parcela é o total dividido. */
    private ResultadoPagamento semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorFinal = Dinheiro.centavos(totalPedido);
        BigDecimal parcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
        return new ResultadoPagamento(valorFinal, parcela);
    }

    /** De 4x a 12x: tabela Price; valor final é parcela x número de parcelas. */
    private ResultadoPagamento comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal base = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal fatorDesconto = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(base.pow(parcelas), MC));
        BigDecimal parcela = Dinheiro.centavos(
                totalPedido.multiply(TAXA_MENSAL).divide(fatorDesconto, MC));
        BigDecimal valorFinal = Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(valorFinal, parcela);
    }
}
