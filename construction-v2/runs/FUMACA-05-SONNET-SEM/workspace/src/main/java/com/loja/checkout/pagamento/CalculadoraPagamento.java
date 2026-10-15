package com.loja.checkout.pagamento;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.ErroCodigo;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

@Component
public class CalculadoraPagamento {

    private static final MathContext MC = new MathContext(50);

    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");

    private static final int CARTAO_PARCELAS_SEM_JUROS = 3;
    private static final int CARTAO_PARCELAS_MAXIMO = 12;
    private static final BigDecimal TAXA_JUROS_CARTAO_MES = new BigDecimal("0.0199");

    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");

    public void validarParcelas(FormaPagamento forma, int parcelas) {
        boolean valido = switch (forma) {
            case PIX, BOLETO -> parcelas == 1;
            case CARTAO -> parcelas >= 1 && parcelas <= CARTAO_PARCELAS_MAXIMO;
        };
        if (!valido) {
            throw new CheckoutException(ErroCodigo.PARCELAMENTO_INVALIDO);
        }
    }

    public void validarDisponibilidade(FormaPagamento forma, BigDecimal totalPedido) {
        if (forma == FormaPagamento.BOLETO && totalPedido.compareTo(LIMITE_BOLETO) > 0) {
            throw new CheckoutException(ErroCodigo.FORMA_PAGAMENTO_INDISPONIVEL);
        }
    }

    public ResultadoPagamento calcular(FormaPagamento forma, int parcelas, BigDecimal totalPedido) {
        return switch (forma) {
            case PIX -> calcularPix(totalPedido);
            case BOLETO -> calcularBoleto(totalPedido);
            case CARTAO -> calcularCartao(parcelas, totalPedido);
        };
    }

    private ResultadoPagamento calcularPix(BigDecimal totalPedido) {
        BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(DESCONTO_PIX));
        BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.subtract(desconto));
        BigDecimal ajuste = Dinheiro.arredondar(totalFinal.subtract(totalPedido));
        return new ResultadoPagamento(ajuste, totalFinal, 1, totalFinal);
    }

    private ResultadoPagamento calcularBoleto(BigDecimal totalPedido) {
        BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(TARIFA_BOLETO));
        BigDecimal ajuste = Dinheiro.arredondar(totalFinal.subtract(totalPedido));
        return new ResultadoPagamento(ajuste, totalFinal, 1, totalFinal);
    }

    private ResultadoPagamento calcularCartao(int parcelas, BigDecimal totalPedido) {
        if (parcelas <= CARTAO_PARCELAS_SEM_JUROS) {
            BigDecimal totalFinal = totalPedido;
            BigDecimal valorParcela = Dinheiro.arredondar(
                    totalFinal.divide(BigDecimal.valueOf(parcelas), MC));
            return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), totalFinal, parcelas, valorParcela);
        }

        BigDecimal valorParcela = calcularParcelaPrice(totalPedido, parcelas);
        BigDecimal totalFinal = Dinheiro.arredondar(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        BigDecimal ajuste = Dinheiro.arredondar(totalFinal.subtract(totalPedido));
        return new ResultadoPagamento(ajuste, totalFinal, parcelas, valorParcela);
    }

    /**
     * Tabela Price: parcela = total x taxa / (1 - (1 + taxa)^-n).
     */
    private BigDecimal calcularParcelaPrice(BigDecimal totalPedido, int parcelas) {
        BigDecimal base = BigDecimal.ONE.add(TAXA_JUROS_CARTAO_MES);
        BigDecimal baseElevada = base.pow(parcelas, MC);
        BigDecimal inversoBaseElevada = BigDecimal.ONE.divide(baseElevada, MC);
        BigDecimal denominador = BigDecimal.ONE.subtract(inversoBaseElevada);
        BigDecimal parcela = totalPedido.multiply(TAXA_JUROS_CARTAO_MES).divide(denominador, MC);
        return Dinheiro.arredondar(parcela);
    }
}
