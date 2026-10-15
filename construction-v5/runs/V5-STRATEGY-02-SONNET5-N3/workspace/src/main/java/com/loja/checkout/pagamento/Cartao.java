package com.loja.checkout.pagamento;

import com.loja.checkout.comum.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

@Component
public class Cartao implements FormaPagamento {

    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = new MathContext(20);

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= PARCELAS_MINIMAS && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Arredondamento.paraCentavos(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
            return new ResultadoPagamento(totalPedido, valorParcela);
        }

        BigDecimal fatorComposto = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, PRECISAO);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fatorComposto, PRECISAO));
        BigDecimal parcelaBruta = totalPedido.multiply(TAXA_MENSAL).divide(denominador, PRECISAO);
        BigDecimal valorParcela = Arredondamento.paraCentavos(parcelaBruta);
        BigDecimal valorFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        return new ResultadoPagamento(valorFinal, valorParcela);
    }
}
