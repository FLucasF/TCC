package com.loja.checkout.calculo;

import com.loja.checkout.api.PedidoRequest;
import com.loja.checkout.api.PedidoRequest.ItemRequest;
import com.loja.checkout.api.ResumoResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import static com.loja.checkout.calculo.Dinheiro.centavos;

@Component
public class CalculadoraResumo {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal CINCO_KG = new BigDecimal("5");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000");
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500");
    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal JUROS_CARTAO = new BigDecimal("0.0199");
    private static final int MAX_PARCELAS_CARTAO = 12;
    private static final int MAX_PARCELAS_SEM_JUROS = 3;
    private static final MathContext PRECISAO = MathContext.DECIMAL128;

    public ResumoResponse calcular(PedidoRequest pedido) {
        List<ItemPedido> itens = validarItens(pedido.itens());
        NivelClube nivel = enumDe(NivelClube.class, pedido.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = enumDe(Regiao.class, pedido.regiao(), "REGIAO_INVALIDA");
        ModalidadeEntrega modalidade = enumDe(ModalidadeEntrega.class, pedido.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal peso = pesoTotal(itens);
        if (!modalidade.disponivel(peso)) {
            throw new ErroCheckout("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = subtotal(itens);
        Cupom cupom = cupomInformado(pedido.cupom());
        if (cupom != null && !cupom.aplicavel(itens, subtotal)) {
            throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
        }

        FormaPagamento forma = enumDe(FormaPagamento.class, pedido.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        validarParcelas(forma, parcelas);

        BigDecimal frete = nivel.freteGratis() ? ZERO : centavos(modalidade.freteBruto(peso));
        BigDecimal descontoCupom = cupom == null ? ZERO : cupom.desconto(itens, subtotal, frete);
        BigDecimal seguro = centavos(subtotal.multiply(regiao.taxaSeguro()));
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (forma == FormaPagamento.BOLETO && totalPedido.compareTo(LIMITE_BOLETO) > 0) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        Pagamento pagamento = calcularPagamento(forma, parcelas, totalPedido);
        BigDecimal ajustePagamento = pagamento.totalFinal().subtract(totalPedido);
        BigDecimal credito = centavos(subtotal.multiply(nivel.taxaCredito()));
        boolean brinde = nivel == NivelClube.OURO && subtotal.compareTo(LIMITE_BRINDE) > 0;

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento,
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                credito,
                brinde);
    }

    private static List<ItemPedido> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }
        return itens.stream().map(CalculadoraResumo::validarItem).toList();
    }

    private static ItemPedido validarItem(ItemRequest item) {
        if (item == null
                || !positivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || !positivo(item.pesoKg())) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }
        return new ItemPedido(item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static BigDecimal subtotal(List<ItemPedido> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            total = total.add(item.subtotal());
        }
        return total;
    }

    private static BigDecimal pesoTotal(List<ItemPedido> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            total = total.add(item.peso());
        }
        return total;
    }

    private static Cupom cupomInformado(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return enumDe(Cupom.class, codigo, "CUPOM_INVALIDO");
    }

    private static void validarParcelas(FormaPagamento forma, int parcelas) {
        boolean valido = switch (forma) {
            case CARTAO -> parcelas >= 1 && parcelas <= MAX_PARCELAS_CARTAO;
            case PIX, BOLETO -> parcelas == 1;
        };
        if (!valido) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }
    }

    private static Pagamento calcularPagamento(FormaPagamento forma, int parcelas, BigDecimal total) {
        return switch (forma) {
            case PIX -> {
                BigDecimal finalPix = total.subtract(centavos(total.multiply(DESCONTO_PIX)));
                yield new Pagamento(1, finalPix, finalPix);
            }
            case BOLETO -> {
                BigDecimal finalBoleto = total.add(TARIFA_BOLETO);
                yield new Pagamento(1, finalBoleto, finalBoleto);
            }
            case CARTAO -> parcelas <= MAX_PARCELAS_SEM_JUROS
                    ? cartaoSemJuros(total, parcelas)
                    : cartaoComJuros(total, parcelas);
        };
    }

    private static Pagamento cartaoSemJuros(BigDecimal total, int parcelas) {
        BigDecimal valorParcela = total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
        return new Pagamento(parcelas, valorParcela, total);
    }

    private static Pagamento cartaoComJuros(BigDecimal total, int parcelas) {
        BigDecimal fatorDesconto = BigDecimal.ONE.divide(
                BigDecimal.ONE.add(JUROS_CARTAO).pow(parcelas, PRECISAO), PRECISAO);
        BigDecimal denominador = BigDecimal.ONE.subtract(fatorDesconto);
        BigDecimal valorParcela = centavos(total.multiply(JUROS_CARTAO).divide(denominador, PRECISAO));
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        return new Pagamento(parcelas, valorParcela, centavos(totalFinal));
    }

    private static <E extends Enum<E>> E enumDe(Class<E> tipo, String codigo, String erro) {
        if (codigo != null) {
            for (E constante : tipo.getEnumConstants()) {
                if (constante.name().equals(codigo)) {
                    return constante;
                }
            }
        }
        throw new ErroCheckout(erro);
    }

    private record Pagamento(int parcelas, BigDecimal valorParcela, BigDecimal totalFinal) {
    }
}
