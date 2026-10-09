package com.loja.checkout.calculo;

import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CalculadoraResumo {

    private static final MathContext PRECISAO = new MathContext(20, RoundingMode.HALF_EVEN);
    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000");
    private static final BigDecimal JUROS_CARTAO_MENSAL = new BigDecimal("0.0199");
    private static final int MAX_PARCELAS_CARTAO = 12;
    private static final int MAX_PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal DESCONTO_BEMVINDO10 = new BigDecimal("0.10");
    private static final BigDecimal MINIMO_MENOS50 = new BigDecimal("300");
    private static final BigDecimal VALOR_MENOS50 = new BigDecimal("50");
    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500");

    public ResumoResponse calcular(ResumoRequest pedido) {
        validarItens(pedido.itens());
        NivelClube nivel = enumOuErro(NivelClube.class, pedido.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = enumOuErro(Regiao.class, pedido.regiao(), CodigoErro.REGIAO_INVALIDA);
        ModalidadeEntrega modalidade =
                enumOuErro(ModalidadeEntrega.class, pedido.modalidadeEntrega(), CodigoErro.MODALIDADE_INVALIDA);

        BigDecimal peso = pesoTotal(pedido.itens());
        if (!modalidade.disponivelPara(peso)) {
            throw new ErroPedido(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotal = subtotal(pedido.itens());
        BigDecimal frete = dinheiro(nivel == NivelClube.OURO ? BigDecimal.ZERO : modalidade.frete(peso));

        Cupom cupom = cupomInformado(pedido.cupom());
        BigDecimal desconto = cupom == null ? BigDecimal.ZERO : descontoDo(cupom, pedido.itens(), subtotal, frete);

        BigDecimal seguro = dinheiro(subtotal.multiply(regiao.percentualSeguro()));
        BigDecimal totalPedido = dinheiro(subtotal.subtract(desconto).add(frete).add(seguro));

        FormaPagamento forma =
                enumOuErro(FormaPagamento.class, pedido.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        validarParcelas(forma, parcelas);
        if (forma == FormaPagamento.BOLETO && totalPedido.compareTo(LIMITE_BOLETO) > 0) {
            throw new ErroPedido(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        Cobranca cobranca = cobrar(forma, parcelas, totalPedido);
        BigDecimal credito = dinheiro(subtotal.multiply(nivel.percentualCredito()));
        boolean brinde = nivel == NivelClube.OURO && subtotal.compareTo(MINIMO_BRINDE) > 0;

        return new ResumoResponse(
                subtotal,
                dinheiro(desconto),
                frete,
                modalidade.prazoDias(),
                seguro,
                dinheiro(cobranca.totalFinal().subtract(totalPedido)),
                cobranca.totalFinal(),
                cobranca.parcelas(),
                cobranca.valorParcela(),
                credito,
                brinde);
    }

    private static void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroPedido(CodigoErro.PEDIDO_INVALIDO);
        }
        for (ItemRequest item : itens) {
            if (item == null
                    || !positivo(item.precoUnitario())
                    || item.quantidade() == null
                    || item.quantidade() <= 0
                    || !positivo(item.pesoKg())) {
                throw new ErroPedido(CodigoErro.PEDIDO_INVALIDO);
            }
        }
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static BigDecimal subtotal(List<ItemRequest> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            total = total.add(dinheiro(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade()))));
        }
        return total;
    }

    private static BigDecimal pesoTotal(List<ItemRequest> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            total = total.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return total;
    }

    private static Cupom cupomInformado(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return enumOuErro(Cupom.class, valor, CodigoErro.CUPOM_INVALIDO);
    }

    private static BigDecimal descontoDo(Cupom cupom, List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
        return switch (cupom) {
            case BEMVINDO10 -> dinheiro(subtotal.multiply(DESCONTO_BEMVINDO10));
            case MENOS50 -> {
                if (subtotal.compareTo(MINIMO_MENOS50) < 0) {
                    throw new ErroPedido(CodigoErro.CUPOM_NAO_APLICAVEL);
                }
                yield VALOR_MENOS50;
            }
            case FRETEGRATIS -> frete;
            case LEVE3PAGUE2 -> {
                BigDecimal desconto = descontoLeve3Pague2(itens);
                if (desconto.signum() == 0) {
                    throw new ErroPedido(CodigoErro.CUPOM_NAO_APLICAVEL);
                }
                yield desconto;
            }
        };
    }

    private static BigDecimal descontoLeve3Pague2(List<ItemRequest> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int unidadesGratis = item.quantidade() / 3;
            desconto = desconto.add(dinheiro(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis))));
        }
        return desconto;
    }

    private static void validarParcelas(FormaPagamento forma, int parcelas) {
        boolean valido = switch (forma) {
            case CARTAO -> parcelas >= 1 && parcelas <= MAX_PARCELAS_CARTAO;
            case PIX, BOLETO -> parcelas == 1;
        };
        if (!valido) {
            throw new ErroPedido(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    private static Cobranca cobrar(FormaPagamento forma, int parcelas, BigDecimal total) {
        return switch (forma) {
            case PIX -> {
                BigDecimal totalFinal = total.subtract(dinheiro(total.multiply(DESCONTO_PIX)));
                yield new Cobranca(totalFinal, 1, totalFinal);
            }
            case BOLETO -> {
                BigDecimal totalFinal = total.add(TARIFA_BOLETO);
                yield new Cobranca(totalFinal, 1, totalFinal);
            }
            case CARTAO -> parcelas <= MAX_PARCELAS_SEM_JUROS
                    ? new Cobranca(total, parcelas, total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN))
                    : cartaoComJuros(total, parcelas);
        };
    }

    private static Cobranca cartaoComJuros(BigDecimal total, int parcelas) {
        BigDecimal fatorDeDesconto = BigDecimal.ONE
                .divide(BigDecimal.ONE.add(JUROS_CARTAO_MENSAL).pow(parcelas, PRECISAO), PRECISAO);
        BigDecimal parcela = dinheiro(
                total.multiply(JUROS_CARTAO_MENSAL).divide(BigDecimal.ONE.subtract(fatorDeDesconto), PRECISAO));
        return new Cobranca(parcela.multiply(BigDecimal.valueOf(parcelas)), parcelas, parcela);
    }

    private static <E extends Enum<E>> E enumOuErro(Class<E> tipo, String valor, CodigoErro erro) {
        if (valor == null) {
            throw new ErroPedido(erro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new ErroPedido(erro);
        }
    }

    private static BigDecimal dinheiro(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    private record Cobranca(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
    }
}
