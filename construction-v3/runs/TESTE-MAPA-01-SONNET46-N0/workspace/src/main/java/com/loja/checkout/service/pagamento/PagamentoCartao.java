package com.loja.checkout.service.pagamento;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PagamentoCartao implements EstrategiaPagamento {

    private static final double TAXA_JUROS = 0.0199;

    @Override
    public String getCodigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceitaParcelamento(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean atendePedido(BigDecimal total) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
        if (parcelas <= 3) {
            // sem juros
            BigDecimal valorParcela = total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(total.setScale(2, RoundingMode.HALF_EVEN), parcelas, valorParcela);
        } else {
            // com juros - tabela Price
            // parcela = total × taxa / (1 − (1 + taxa)^(−n))
            double taxa = TAXA_JUROS;
            double fatorComposto = Math.pow(1.0 + taxa, parcelas);
            double denominador = (fatorComposto - 1.0) / fatorComposto;
            BigDecimal valorParcela = BigDecimal.valueOf(total.doubleValue() * taxa / denominador)
                    .setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas)).setScale(2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(totalFinal, parcelas, valorParcela);
        }
    }
}
