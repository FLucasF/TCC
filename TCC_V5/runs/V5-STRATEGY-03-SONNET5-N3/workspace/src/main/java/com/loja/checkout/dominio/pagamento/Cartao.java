package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.erro.PedidoInvalidoException;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class Cartao implements FormaPagamento {

    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = new MathContext(20);

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas < 1 || parcelas > PARCELAS_MAXIMAS) {
            throw new PedidoInvalidoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    @Override
    public AjustePagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Dinheiro.arredondar(totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
            return new AjustePagamento(BigDecimal.ZERO, totalPedido, valorParcela);
        }
        BigDecimal baseElevada = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
        BigDecimal fator = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(baseElevada, PRECISAO));
        BigDecimal valorParcela = Dinheiro.arredondar(totalPedido.multiply(TAXA_MENSAL).divide(fator, PRECISAO));
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new AjustePagamento(ajuste, totalFinal, valorParcela);
    }
}
