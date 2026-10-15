package com.loja.service;

import com.loja.domain.Regiao;
import com.loja.domain.clube.NivelClube;
import com.loja.domain.clube.NivelClubeFactory;
import com.loja.domain.cupom.Cupom;
import com.loja.domain.cupom.CupomFactory;
import com.loja.domain.modalidade.ModalidadeEntrega;
import com.loja.domain.modalidade.ModalidadeEntregaFactory;
import com.loja.domain.pagamento.FormaPagamento;
import com.loja.domain.pagamento.FormaPagamentoFactory;
import com.loja.model.CheckoutRequest;
import com.loja.model.CheckoutResponse;
import com.loja.model.ItemCarrinho;
import com.loja.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarPedido(request);

        List<ItemCarrinho> itens = request.itens();
        NivelClube nivelClube = NivelClubeFactory.criar(request.nivelClube());
        ModalidadeEntrega modalidade = ModalidadeEntregaFactory.criar(request.modalidadeEntrega());
        Regiao regiao = Regiao.fromString(request.regiao());
        FormaPagamento formaPagamento = FormaPagamentoFactory.criar(request.formaPagamento());
        int numeroParcelas = request.parcelas() != null ? request.parcelas() : 1;

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);

        BigDecimal freteCalculado = modalidade.calcularFrete(itens);
        BigDecimal frete = nivelClube.isFreteCortesia() ? new BigDecimal("0.00") : freteCalculado;

        Cupom cupom = CupomFactory.criar(request.cupom());
        BigDecimal descontoCupom = new BigDecimal("0.00");
        if (cupom != null) {
            if (!cupom.isAplicavel(subtotalProdutos, itens)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, frete, itens);
        }

        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);

        if (!formaPagamento.isDisponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        BigDecimal ajustePagamento = formaPagamento.calcularAjuste(totalPedido, numeroParcelas);
        BigDecimal totalFinal = totalPedido.add(ajustePagamento);
        BigDecimal valorParcela = formaPagamento.calcularValorParcela(totalPedido, numeroParcelas);

        BigDecimal creditoProximaCompra = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.temBrinde(subtotalProdutos);

        return new CheckoutResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            modalidade.getPrazoEntregaDias(),
            seguro,
            ajustePagamento,
            totalFinal,
            numeroParcelas,
            valorParcela,
            creditoProximaCompra,
            brinde
        );
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        BigDecimal subtotal = itens.stream()
            .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Dinheiro.arredondar(subtotal);
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(regiao.getPercentualSeguro()));
    }

    private void validarPedido(CheckoutRequest request) {
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : request.itens()) {
            if (!item.isValido()) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        NivelClube nivelClube = NivelClubeFactory.criar(request.nivelClube());
        if (nivelClube == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        Regiao regiao = Regiao.fromString(request.regiao());
        if (regiao == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        ModalidadeEntrega modalidade = ModalidadeEntregaFactory.criar(request.modalidadeEntrega());
        if (modalidade == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        if (!modalidade.isDisponivel(request.itens())) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        if (request.cupom() != null && !request.cupom().isBlank()) {
            Cupom cupom = CupomFactory.criar(request.cupom());
            if (cupom == null) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
        }

        FormaPagamento formaPagamento = FormaPagamentoFactory.criar(request.formaPagamento());
        if (formaPagamento == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        int numeroParcelas = request.parcelas() != null ? request.parcelas() : 1;
        if (!formaPagamento.isParcelamentoValido(numeroParcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    public static class CheckoutException extends RuntimeException {
        public CheckoutException(String codigo) {
            super(codigo);
        }
    }
}
