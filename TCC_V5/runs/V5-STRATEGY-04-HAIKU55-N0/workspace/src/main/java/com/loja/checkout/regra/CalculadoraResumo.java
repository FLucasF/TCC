package com.loja.checkout.regra;

import com.loja.checkout.regra.PedidoCompra.Item;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import static com.loja.checkout.regra.CodigoErro.CUPOM_NAO_APLICAVEL;
import static com.loja.checkout.regra.CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL;
import static com.loja.checkout.regra.CodigoErro.MODALIDADE_INDISPONIVEL;
import static com.loja.checkout.regra.CodigoErro.PARCELAMENTO_INVALIDO;
import static com.loja.checkout.regra.CodigoErro.PEDIDO_INVALIDO;

@Service
public class CalculadoraResumo {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final BigDecimal DEZ_POR_CENTO = new BigDecimal("0.10");
    private static final BigDecimal MINIMO_PARA_MENOS50 = new BigDecimal("300");
    private static final BigDecimal DESCONTO_MENOS50 = new BigDecimal("50.00");
    private static final BigDecimal MINIMO_PARA_BRINDE = new BigDecimal("500");
    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000");
    private static final BigDecimal TAXA_JUROS_CARTAO = new BigDecimal("0.0199");
    private static final int MAX_PARCELAS_SEM_JUROS = 3;
    private static final int MAX_PARCELAS = 12;

    public ResumoCompra calcular(PedidoCompra pedido) {
        List<Item> itens = validarItens(pedido.itens());
        BigDecimal subtotal = arredonda(itens.stream()
                .map(item -> item.precoUnitario().multiply(quantidade(item)))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal pesoTotal = itens.stream()
                .map(item -> item.pesoKg().multiply(quantidade(item)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        NivelClube nivel = NivelClube.de(pedido.nivelClube());
        Regiao regiao = Regiao.de(pedido.regiao());
        Modalidade modalidade = Modalidade.de(pedido.modalidadeEntrega());
        if (!modalidade.atende(pesoTotal)) {
            throw new ErroCheckout(MODALIDADE_INDISPONIVEL);
        }

        BigDecimal frete = nivel == NivelClube.OURO ? ZERO : arredonda(modalidade.frete(pesoTotal));
        BigDecimal seguro = arredonda(subtotal.multiply(regiao.taxaSeguro()));
        BigDecimal desconto = descontoCupom(pedido.cupom(), subtotal, frete, itens);

        FormaPagamento forma = FormaPagamento.de(pedido.formaPagamento());
        int parcelas = parcelasValidas(forma, pedido.parcelas());
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);
        if (forma == FormaPagamento.BOLETO && totalPedido.compareTo(LIMITE_BOLETO) > 0) {
            throw new ErroCheckout(FORMA_PAGAMENTO_INDISPONIVEL);
        }

        Pagamento pagamento = calcularPagamento(forma, totalPedido, parcelas);
        BigDecimal credito = arredonda(subtotal.multiply(nivel.percentualCredito()));
        boolean brinde = nivel == NivelClube.OURO && subtotal.compareTo(MINIMO_PARA_BRINDE) > 0;

        return new ResumoCompra(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.ajuste(),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                credito,
                brinde);
    }

    private static List<Item> validarItens(List<Item> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroCheckout(PEDIDO_INVALIDO);
        }
        for (Item item : itens) {
            if (item == null
                    || !positivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || !positivo(item.pesoKg())) {
                throw new ErroCheckout(PEDIDO_INVALIDO);
            }
        }
        return itens;
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static BigDecimal quantidade(Item item) {
        return BigDecimal.valueOf(item.quantidade());
    }

    private static BigDecimal descontoCupom(String codigo, BigDecimal subtotal, BigDecimal frete, List<Item> itens) {
        if (codigo == null || codigo.isBlank()) {
            return ZERO;
        }
        Cupom cupom = Cupom.de(codigo);
        return switch (cupom) {
            case BEMVINDO10 -> arredonda(subtotal.multiply(DEZ_POR_CENTO));
            case MENOS50 -> {
                if (subtotal.compareTo(MINIMO_PARA_MENOS50) < 0) {
                    throw new ErroCheckout(CUPOM_NAO_APLICAVEL);
                }
                yield DESCONTO_MENOS50;
            }
            case FRETEGRATIS -> frete;
            case LEVE3PAGUE2 -> {
                BigDecimal desconto = itens.stream()
                        .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (desconto.signum() == 0) {
                    throw new ErroCheckout(CUPOM_NAO_APLICAVEL);
                }
                yield arredonda(desconto);
            }
        };
    }

    private static int parcelasValidas(FormaPagamento forma, Integer parcelas) {
        int numero = parcelas == null ? 1 : parcelas;
        boolean valido = switch (forma) {
            case CARTAO -> numero >= 1 && numero <= MAX_PARCELAS;
            case PIX, BOLETO -> numero == 1;
        };
        if (!valido) {
            throw new ErroCheckout(PARCELAMENTO_INVALIDO);
        }
        return numero;
    }

    private static Pagamento calcularPagamento(FormaPagamento forma, BigDecimal total, int parcelas) {
        return switch (forma) {
            case PIX -> {
                BigDecimal ajuste = arredonda(total.multiply(DESCONTO_PIX)).negate();
                BigDecimal totalFinal = total.add(ajuste);
                yield new Pagamento(ajuste, totalFinal, 1, totalFinal);
            }
            case BOLETO -> {
                BigDecimal totalFinal = total.add(TARIFA_BOLETO);
                yield new Pagamento(TARIFA_BOLETO, totalFinal, 1, totalFinal);
            }
            case CARTAO -> parcelas <= MAX_PARCELAS_SEM_JUROS
                    ? cartaoSemJuros(total, parcelas)
                    : cartaoComJuros(total, parcelas);
        };
    }

    private static Pagamento cartaoSemJuros(BigDecimal total, int parcelas) {
        BigDecimal valorParcela = total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
        return new Pagamento(ZERO, total, parcelas, valorParcela);
    }

    private static Pagamento cartaoComJuros(BigDecimal total, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_JUROS_CARTAO).pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MathContext.DECIMAL128));
        BigDecimal valorParcela = arredonda(total.multiply(TAXA_JUROS_CARTAO).divide(denominador, MathContext.DECIMAL128));
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        return new Pagamento(totalFinal.subtract(total), totalFinal, parcelas, valorParcela);
    }

    private static BigDecimal arredonda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    private record Pagamento(BigDecimal ajuste, BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
    }
}
