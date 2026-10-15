package com.loja.checkout.servico;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.clube.BeneficioClube;
import com.loja.checkout.clube.BeneficioClubeRegistro;
import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomRegistro;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.ItemCarrinho;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.OpcaoEntrega;
import com.loja.checkout.entrega.OpcoesEntregaRegistro;
import com.loja.checkout.pagamento.Pagamento;
import com.loja.checkout.pagamento.PagamentoRegistro;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.servico.erro.CodigoErro;
import com.loja.checkout.servico.erro.PedidoInvalidoException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Orquestra o calculo do resumo da compra: esta e a parte que e igual em
 * todo pedido (a ordem das etapas e das validacoes). O comportamento que
 * muda por modalidade de entrega, cupom, nivel de clube e forma de
 * pagamento mora nos registros/estrategias de cada um.
 */
@Service
public class ResumoCompraServico {

    private final OpcoesEntregaRegistro opcoesEntregaRegistro;
    private final CupomRegistro cupomRegistro;
    private final BeneficioClubeRegistro beneficioClubeRegistro;
    private final PagamentoRegistro pagamentoRegistro;

    public ResumoCompraServico(
            OpcoesEntregaRegistro opcoesEntregaRegistro,
            CupomRegistro cupomRegistro,
            BeneficioClubeRegistro beneficioClubeRegistro,
            PagamentoRegistro pagamentoRegistro) {
        this.opcoesEntregaRegistro = opcoesEntregaRegistro;
        this.cupomRegistro = cupomRegistro;
        this.beneficioClubeRegistro = beneficioClubeRegistro;
        this.pagamentoRegistro = pagamentoRegistro;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        List<ItemCarrinho> itens = validarItens(request);
        BeneficioClube beneficio = resolverBeneficioClube(request);
        Regiao regiao = resolverRegiao(request);
        OpcaoEntrega opcaoEntrega = resolverOpcaoEntrega(request);

        BigDecimal pesoTotal = itens.stream()
                .map(ItemCarrinho::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!opcaoEntrega.disponivel(pesoTotal)) {
            throw new PedidoInvalidoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = itens.stream()
                .map(ItemCarrinho::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal frete = beneficio.freteGratis()
                ? BigDecimal.ZERO.setScale(2)
                : opcaoEntrega.calcularFrete(pesoTotal);

        BigDecimal descontoCupom = resolverDescontoCupom(request, subtotalProdutos, frete, itens);

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        Pagamento pagamento = resolverPagamento(request, parcelas);

        BigDecimal seguro = Dinheiro.arredondar(subtotalProdutos.multiply(regiao.percentualSeguro()));
        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);

        if (!pagamento.disponivel(totalPedido)) {
            throw new PedidoInvalidoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultadoPagamento = pagamento.calcular(totalPedido, parcelas);
        BigDecimal credito = beneficio.calcularCredito(subtotalProdutos);
        boolean brinde = beneficio.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                opcaoEntrega.prazoDias(),
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                credito,
                brinde);
    }

    private List<ItemCarrinho> validarItens(ResumoRequest request) {
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new PedidoInvalidoException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<ItemCarrinho> itens = request.itens().stream()
                .map(item -> new ItemCarrinho(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()))
                .toList();
        boolean algumInvalido = itens.stream().anyMatch(item -> !item.valido());
        if (algumInvalido) {
            throw new PedidoInvalidoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return itens;
    }

    private BeneficioClube resolverBeneficioClube(ResumoRequest request) {
        NivelClube nivelClube = parseEnum(request.nivelClube(), NivelClube.class)
                .orElseThrow(() -> new PedidoInvalidoException(CodigoErro.NIVEL_CLUBE_INVALIDO));
        return beneficioClubeRegistro.buscar(nivelClube)
                .orElseThrow(() -> new PedidoInvalidoException(CodigoErro.NIVEL_CLUBE_INVALIDO));
    }

    private Regiao resolverRegiao(ResumoRequest request) {
        return parseEnum(request.regiao(), Regiao.class)
                .orElseThrow(() -> new PedidoInvalidoException(CodigoErro.REGIAO_INVALIDA));
    }

    private OpcaoEntrega resolverOpcaoEntrega(ResumoRequest request) {
        ModalidadeEntrega modalidade = parseEnum(request.modalidadeEntrega(), ModalidadeEntrega.class)
                .orElseThrow(() -> new PedidoInvalidoException(CodigoErro.MODALIDADE_INVALIDA));
        return opcoesEntregaRegistro.buscar(modalidade)
                .orElseThrow(() -> new PedidoInvalidoException(CodigoErro.MODALIDADE_INVALIDA));
    }

    private BigDecimal resolverDescontoCupom(
            ResumoRequest request, BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        if (request.cupom() == null || request.cupom().isBlank()) {
            return BigDecimal.ZERO.setScale(2);
        }
        Cupom cupom = cupomRegistro.buscar(request.cupom())
                .orElseThrow(() -> new PedidoInvalidoException(CodigoErro.CUPOM_INVALIDO));
        ContextoCupom contexto = new ContextoCupom(subtotalProdutos, frete, itens);
        if (!cupom.aplicavel(contexto)) {
            throw new PedidoInvalidoException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return cupom.calcularDesconto(contexto);
    }

    private Pagamento resolverPagamento(ResumoRequest request, int parcelas) {
        FormaPagamento forma = parseEnum(request.formaPagamento(), FormaPagamento.class)
                .orElseThrow(() -> new PedidoInvalidoException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        Pagamento pagamento = pagamentoRegistro.buscar(forma)
                .orElseThrow(() -> new PedidoInvalidoException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        if (!pagamento.parcelasValidas(parcelas)) {
            throw new PedidoInvalidoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        return pagamento;
    }

    private <E extends Enum<E>> Optional<E> parseEnum(String valor, Class<E> tipo) {
        if (valor == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Enum.valueOf(tipo, valor));
        } catch (IllegalArgumentException excecao) {
            return Optional.empty();
        }
    }
}
