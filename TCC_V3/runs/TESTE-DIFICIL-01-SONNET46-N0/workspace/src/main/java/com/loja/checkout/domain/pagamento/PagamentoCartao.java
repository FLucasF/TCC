package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.clube.BeneficioClube;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class PagamentoCartao implements FormaPagamento {

    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
    private static final int LIMITE_SEM_JUROS_PADRAO = 3;
    private static final int LIMITE_SEM_JUROS_OURO = 6;
    private static final int MAX_PARCELAS = 12;
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_EVEN);

    @Override
    public String getCodigo() {
        return "CARTAO";
    }

    @Override
    public boolean isParcelasValida(int parcelas, BeneficioClube nivel) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public boolean isDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas, BeneficioClube nivel) {
        int limiteSemJuros = nivel.isJurosSemAte6x() ? LIMITE_SEM_JUROS_OURO : LIMITE_SEM_JUROS_PADRAO;

        if (parcelas <= limiteSemJuros) {
            BigDecimal valorParcela = totalPedido
                    .divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), totalPedido, valorParcela, 0);
        }

        BigDecimal valorParcela = calcularParcelaComJuros(totalPedido, parcelas);
        BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas)).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, valorParcela, 0);
    }

    private BigDecimal calcularParcelaComJuros(BigDecimal total, int n) {
        // Tabela Price: parcela = total × taxa / (1 - (1 + taxa)^-n)
        BigDecimal um = BigDecimal.ONE;
        BigDecimal umMaisTaxa = um.add(TAXA_JUROS);
        BigDecimal potencia = umMaisTaxa.pow(n, MC);
        BigDecimal inversaPotencia = um.divide(potencia, MC);
        BigDecimal denominador = um.subtract(inversaPotencia);
        return total.multiply(TAXA_JUROS, MC)
                .divide(denominador, MC)
                .setScale(2, RoundingMode.HALF_EVEN);
    }
}
