package com.loja.checkout.service;

import com.loja.checkout.domain.*;
import com.loja.checkout.model.*;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CheckoutService {
    private final ModalidadeEntregaFactory modalidadeFactory;
    private final CupomFactory cupomFactory;
    private final NivelClubeFactory nivelClubeFactory;
    private final FormaPagamentoFactory formaPagamentoFactory;

    public CheckoutService(
        ModalidadeEntregaFactory modalidadeFactory,
        CupomFactory cupomFactory,
        NivelClubeFactory nivelClubeFactory,
        FormaPagamentoFactory formaPagamentoFactory
    ) {
        this.modalidadeFactory = modalidadeFactory;
        this.cupomFactory = cupomFactory;
        this.nivelClubeFactory = nivelClubeFactory;
        this.formaPagamentoFactory = formaPagamentoFactory;
    }

    public Object calcularResumo(PedidoRequest request) {
        String erro = validarPedido(request);
        if (erro != null) {
            return new ErroResponse(erro);
        }

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.itens());

        NivelClube nivelClube = nivelClubeFactory.obter(request.nivelClube());
        ModalidadeEntrega modalidade = modalidadeFactory.obter(request.modalidadeEntrega());

        BigDecimal freteBase = modalidade.calcularFrete(request.itens());
        BigDecimal frete = nivelClube.freteSempre​Gratis() ? new BigDecimal("0.00") : freteBase;

        BigDecimal descontoCupom = new BigDecimal("0.00");
        if (request.cupom() != null && !request.cupom().isBlank()) {
            Cupom cupom = cupomFactory.obter(request.cupom());
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, frete, request.itens());
        }

        Regiao regiao = Regiao.valueOf(request.regiao());
        BigDecimal seguro = Dinheiro.arredondar(
            subtotalProdutos.multiply(regiao.percentual())
        );

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        FormaPagamento formaPagamento = formaPagamentoFactory.obter(request.formaPagamento());

        BigDecimal ajustePagamento = formaPagamento.calcularAjuste(totalPedido, parcelas);
        BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(ajustePagamento));

        int parcelasFinal = formaPagamento.numeroParcelas(parcelas);
        BigDecimal valorParcela = formaPagamento.calcularValorParcela(totalFinal, parcelasFinal);

        BigDecimal creditoProximaCompra = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.ganhaBrinde(subtotalProdutos);

        return new ResumoResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            modalidade.prazoEntregaDias(),
            seguro,
            ajustePagamento,
            totalFinal,
            parcelasFinal,
            valorParcela,
            creditoProximaCompra,
            brinde
        );
    }

    private String validarPedido(PedidoRequest request) {
        if (request.itens() == null || request.itens().isEmpty()) {
            return "PEDIDO_INVALIDO";
        }

        for (ItemCarrinho item : request.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                return "PEDIDO_INVALIDO";
            }
        }

        if (request.nivelClube() == null || !nivelClubeFactory.existe(request.nivelClube())) {
            return "NIVEL_CLUBE_INVALIDO";
        }

        if (request.regiao() == null) {
            return "REGIAO_INVALIDA";
        }

        try {
            Regiao.valueOf(request.regiao());
        } catch (IllegalArgumentException e) {
            return "REGIAO_INVALIDA";
        }

        if (request.modalidadeEntrega() == null || !modalidadeFactory.existe(request.modalidadeEntrega())) {
            return "MODALIDADE_INVALIDA";
        }

        ModalidadeEntrega modalidade = modalidadeFactory.obter(request.modalidadeEntrega());
        if (!modalidade.aceitaPedido(request.itens())) {
            return "MODALIDADE_INDISPONIVEL";
        }

        if (request.cupom() != null && !request.cupom().isBlank()) {
            if (!cupomFactory.existe(request.cupom())) {
                return "CUPOM_INVALIDO";
            }

            Cupom cupom = cupomFactory.obter(request.cupom());
            BigDecimal subtotal = calcularSubtotalProdutos(request.itens());
            if (!cupom.aplicavel(subtotal, request.itens())) {
                return "CUPOM_NAO_APLICAVEL";
            }
        }

        if (request.formaPagamento() == null || !formaPagamentoFactory.existe(request.formaPagamento())) {
            return "FORMA_PAGAMENTO_INVALIDA";
        }

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        FormaPagamento formaPagamento = formaPagamentoFactory.obter(request.formaPagamento());

        if (!formaPagamento.parcelamentoValido(parcelas)) {
            return "PARCELAMENTO_INVALIDO";
        }

        BigDecimal subtotal = calcularSubtotalProdutos(request.itens());
        NivelClube nivelClube = nivelClubeFactory.obter(request.nivelClube());
        ModalidadeEntrega modalidadeEntrega = modalidadeFactory.obter(request.modalidadeEntrega());

        BigDecimal freteBase = modalidadeEntrega.calcularFrete(request.itens());
        BigDecimal frete = nivelClube.freteSempre​Gratis() ? new BigDecimal("0.00") : freteBase;

        BigDecimal descontoCupom = new BigDecimal("0.00");
        if (request.cupom() != null && !request.cupom().isBlank()) {
            Cupom cupom = cupomFactory.obter(request.cupom());
            descontoCupom = cupom.calcularDesconto(subtotal, frete, request.itens());
        }

        Regiao regiao = Regiao.valueOf(request.regiao());
        BigDecimal seguro = Dinheiro.arredondar(subtotal.multiply(regiao.percentual()));

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (!formaPagamento.aceitaPedido(totalPedido)) {
            return "FORMA_PAGAMENTO_INDISPONIVEL";
        }

        return null;
    }

    private BigDecimal calcularSubtotalProdutos(java.util.List<ItemCarrinho> itens) {
        BigDecimal subtotal = itens.stream()
            .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Dinheiro.arredondar(subtotal);
    }
}
