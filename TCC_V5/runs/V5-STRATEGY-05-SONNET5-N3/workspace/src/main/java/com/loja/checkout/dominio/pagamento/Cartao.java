package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class Cartao implements CalculoPagamento {

    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int MAXIMO_PARCELAS = 12;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int ESCALA_INTERMEDIARIA = 20;

    @Override
    public FormaPagamento forma() {
        return FormaPagamento.CARTAO;
    }

    @Override
    public boolean parcelaPermitida(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Arredondamento.paraCentavos(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), ESCALA_INTERMEDIARIA, RoundingMode.HALF_EVEN));
            return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), totalPedido, valorParcela);
        }

        BigDecimal potencia = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
        BigDecimal numerador = totalPedido.multiply(TAXA_MENSAL).multiply(potencia);
        BigDecimal denominador = potencia.subtract(BigDecimal.ONE);
        BigDecimal valorParcela = Arredondamento.paraCentavos(
                numerador.divide(denominador, ESCALA_INTERMEDIARIA, RoundingMode.HALF_EVEN));
        BigDecimal totalFinal = Arredondamento.paraCentavos(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }
}
