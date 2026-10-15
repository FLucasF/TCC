package com.loja.checkout.pagamento.impl;

import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.util.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

@Component
public class CartaoPagamento implements FormaPagamento {

    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_AO_MES = new BigDecimal("0.0199");
    private static final BigDecimal UM = BigDecimal.ONE;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean disponivelPara(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal parcela = Arredondamento.paraCentavos(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
            return new ResultadoPagamento(BigDecimal.ZERO, totalPedido, parcelas, parcela);
        }

        BigDecimal fator = UM.add(TAXA_AO_MES).pow(parcelas);
        BigDecimal numerador = totalPedido.multiply(TAXA_AO_MES).multiply(fator);
        BigDecimal denominador = fator.subtract(UM);
        BigDecimal parcela = Arredondamento.paraCentavos(numerador.divide(denominador, MathContext.DECIMAL128));
        BigDecimal totalFinal = parcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, parcelas, parcela);
    }
}
