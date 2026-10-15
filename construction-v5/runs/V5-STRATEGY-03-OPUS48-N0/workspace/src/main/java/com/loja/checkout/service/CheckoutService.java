package com.loja.checkout.service;

import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.LinhaCarrinho;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.CheckoutResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula o resumo da compra.
 *
 * <p>As verificacoes acontecem exatamente na ordem combinada e, no primeiro problema,
 * o pedido e recusado com {@link CheckoutException}. Os calculos de dinheiro entre uma
 * etapa e outra sao arredondados para centavos no modo "meio para o par"
 * ({@link RoundingMode#HALF_EVEN}).</p>
 */
@Service
public class CheckoutService {

    /** Juros do cartao de 4x a 12x: 1,99% ao mes. */
    private static final BigDecimal TAXA_CARTAO = new BigDecimal("0.0199");
    /** Desconto do Pix: 5% do total do pedido. */
    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    /** Tarifa do boleto cobrada pelo banco. */
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    /** Ate este valor o cartao e sem juros. */
    private static final int MAX_PARCELAS_SEM_JUROS = 3;
    /** Maximo de parcelas no cartao. */
    private static final int MAX_PARCELAS_CARTAO = 12;
    /** Boleto nao e aceito para total do pedido acima deste valor. */
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");
    /** Precisao para as contas intermediarias (divisoes) antes de arredondar para centavos. */
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_EVEN);

    public CheckoutResponse calcular(CheckoutRequest req) {
        // 1) Carrinho e itens validos.
        List<LinhaCarrinho> itens = validarItens(req);
        BigDecimal subtotalProdutos = centavos(somarProdutos(itens));

        // 2) Nivel do clube.
        NivelClube nivel = validarNivel(req.nivelClube());

        // 3) Regiao.
        Regiao regiao = validarRegiao(req.regiao());

        // 4) Modalidade de entrega existe.
        ModalidadeEntrega modalidade = validarModalidade(req.modalidadeEntrega());

        // 5) Modalidade atende o pedido (ex.: motoboy ate 5 kg).
        BigDecimal pesoPedido = somarPeso(itens);
        if (!modalidade.atendePeso(pesoPedido)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        // Frete: calculado pela modalidade, mas o clube OURO nunca paga frete.
        BigDecimal frete = nivel.pagaFrete()
                ? centavos(modalidade.calcularFrete(pesoPedido))
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

        // 6) e 7) Cupom existe e e aplicavel; depois o desconto.
        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        Cupom cupom = validarCupom(req.cupom());
        if (cupom != null) {
            Cupom.Contexto ctx = new Cupom.Contexto(itens, subtotalProdutos, frete);
            if (!cupom.aplicavel(ctx)) {
                throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = centavos(cupom.descontoBruto(ctx));
        }

        // 8) Forma de pagamento existe.
        FormaPagamento formaPagamento = validarFormaPagamento(req.formaPagamento());

        // 9) Numero de parcelas permitido para a forma.
        int parcelas = validarParcelamento(formaPagamento, req.parcelas());

        // Seguro: porcentagem da regiao sobre os produtos (sem desconto, sem frete).
        BigDecimal seguro = centavos(subtotalProdutos.multiply(regiao.percentualSeguro()));

        // Total do pedido = produtos - desconto + frete + seguro.
        BigDecimal totalPedido = centavos(subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro));

        // 10) Forma de pagamento atende o pedido (ex.: boleto ate R$ 1.000,00).
        if (formaPagamento == FormaPagamento.BOLETO && totalPedido.compareTo(LIMITE_BOLETO) > 0) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        // Ajuste da forma de pagamento sobre o total do pedido.
        Pagamento pagamento = calcularPagamento(formaPagamento, parcelas, totalPedido);

        // Credito do clube: porcentagem sobre os produtos (sem desconto, sem frete).
        BigDecimal credito = centavos(subtotalProdutos.multiply(nivel.percentualCredito()));

        boolean brinde = nivel.ganhaBrinde(subtotalProdutos);

        return new CheckoutResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.ajuste(),
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                credito,
                brinde
        );
    }

    // ----------------------------------------------------------------------
    // Validacoes
    // ----------------------------------------------------------------------

    private List<LinhaCarrinho> validarItens(CheckoutRequest req) {
        if (req.itens() == null || req.itens().isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<LinhaCarrinho> linhas = new ArrayList<>();
        for (CheckoutRequest.Item item : req.itens()) {
            if (item == null
                    || item.precoUnitario() == null || !positivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || !positivo(item.pesoKg())) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
            linhas.add(new LinhaCarrinho(item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return linhas;
    }

    private NivelClube validarNivel(String valor) {
        NivelClube nivel = aEnum(NivelClube.class, valor);
        if (nivel == null) {
            throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
        return nivel;
    }

    private Regiao validarRegiao(String valor) {
        Regiao regiao = aEnum(Regiao.class, valor);
        if (regiao == null) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }
        return regiao;
    }

    private ModalidadeEntrega validarModalidade(String valor) {
        ModalidadeEntrega modalidade = aEnum(ModalidadeEntrega.class, valor);
        if (modalidade == null) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }
        return modalidade;
    }

    /** Retorna o cupom, ou {@code null} quando o cliente nao usou cupom. */
    private Cupom validarCupom(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        Cupom cupom = aEnum(Cupom.class, valor);
        if (cupom == null) {
            throw new CheckoutException(CodigoErro.CUPOM_INVALIDO);
        }
        return cupom;
    }

    private FormaPagamento validarFormaPagamento(String valor) {
        FormaPagamento forma = aEnum(FormaPagamento.class, valor);
        if (forma == null) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        return forma;
    }

    private int validarParcelamento(FormaPagamento forma, Integer parcelasInformadas) {
        int parcelas = parcelasInformadas == null ? 1 : parcelasInformadas;
        boolean ok = switch (forma) {
            case PIX, BOLETO -> parcelas == 1;                       // sempre a vista
            case CARTAO -> parcelas >= 1 && parcelas <= MAX_PARCELAS_CARTAO;
        };
        if (!ok) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        return parcelas;
    }

    // ----------------------------------------------------------------------
    // Pagamento
    // ----------------------------------------------------------------------

    /** Resultado do ajuste da forma de pagamento. */
    private record Pagamento(BigDecimal totalFinal, BigDecimal valorParcela, BigDecimal ajuste) {
    }

    private Pagamento calcularPagamento(FormaPagamento forma, int parcelas, BigDecimal totalPedido) {
        return switch (forma) {
            case PIX -> {
                BigDecimal desconto = centavos(totalPedido.multiply(DESCONTO_PIX));
                BigDecimal totalFinal = centavos(totalPedido.subtract(desconto));
                yield new Pagamento(totalFinal, totalFinal, desconto.negate());
            }
            case BOLETO -> {
                BigDecimal totalFinal = centavos(totalPedido.add(TARIFA_BOLETO));
                yield new Pagamento(totalFinal, totalFinal, TARIFA_BOLETO);
            }
            case CARTAO -> {
                if (parcelas <= MAX_PARCELAS_SEM_JUROS) {
                    // Sem juros: valor final e o proprio total; parcela e o total dividido.
                    BigDecimal valorParcela = centavos(totalPedido.divide(BigDecimal.valueOf(parcelas), MC));
                    BigDecimal ajuste = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
                    yield new Pagamento(totalPedido, valorParcela, ajuste);
                }
                // Com juros (tabela Price): parcela = total x taxa / (1 - (1 + taxa)^-n).
                BigDecimal fator = BigDecimal.ONE.add(TAXA_CARTAO).pow(parcelas);          // (1 + taxa)^n
                BigDecimal inverso = BigDecimal.ONE.divide(fator, MC);                      // (1 + taxa)^-n
                BigDecimal denominador = BigDecimal.ONE.subtract(inverso);
                BigDecimal parcela = centavos(totalPedido.multiply(TAXA_CARTAO).divide(denominador, MC));
                BigDecimal valorFinal = centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
                BigDecimal ajuste = centavos(valorFinal.subtract(totalPedido));
                yield new Pagamento(valorFinal, parcela, ajuste);
            }
        };
    }

    // ----------------------------------------------------------------------
    // Auxiliares
    // ----------------------------------------------------------------------

    private BigDecimal somarProdutos(List<LinhaCarrinho> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (LinhaCarrinho linha : itens) {
            total = total.add(linha.totalLinha());
        }
        return total;
    }

    private BigDecimal somarPeso(List<LinhaCarrinho> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (LinhaCarrinho linha : itens) {
            total = total.add(linha.pesoLinha());
        }
        return total;
    }

    private static boolean positivo(BigDecimal valor) {
        return valor.compareTo(BigDecimal.ZERO) > 0;
    }

    /** Arredonda para centavos no modo "meio para o par". */
    private static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Converte texto para o enum correspondente; devolve {@code null} se nao casar. */
    private static <E extends Enum<E>> E aEnum(Class<E> tipo, String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
