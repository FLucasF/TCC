package com.loja.checkout.service;

import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CheckoutService {

    public ResumoResponse calcularResumo(ResumoRequest request) {
        Optional<String> erro = validar(request);
        if (erro.isPresent()) {
            throw new ErroCheckout(erro.get());
        }

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.itens());

        Cupom cupom = request.cupom() != null ? Cupom.valueOf(request.cupom()) : null;
        BigDecimal descontoCupomCalculado = cupom != null && !cupom.ehFretegratis()
            ? cupom.calcular(subtotalProdutos, request.itens())
            : BigDecimal.ZERO;

        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(request.modalidadeEntrega());
        BigDecimal pesoTotal = calcularPesoTotal(request.itens());
        BigDecimal freteOriginal = calcularFreteOriginal(modalidade, pesoTotal);

        NivelClube nivelClube = NivelClube.valueOf(request.nivelClube());
        BigDecimal frete = nivelClube.temFreteGratis() ? BigDecimal.ZERO : freteOriginal;

        BigDecimal totalPedidoParaPagamento = subtotalProdutos.subtract(descontoCupomCalculado).add(frete);

        Regiao regiao = Regiao.valueOf(request.regiao());
        BigDecimal baseImposto = subtotalProdutos.subtract(descontoCupomCalculado);
        BigDecimal imposto = regiao.calcularImposto(baseImposto);

        FormaPagamento formaPagamento = FormaPagamento.valueOf(request.formaPagamento());
        int parcelas = request.getParcelas();

        FormaPagamento.ResultadoPagamento pagamento = formaPagamento.calcular(totalPedidoParaPagamento, parcelas);
        BigDecimal totalFinal = pagamento.totalFinal();
        BigDecimal ajustePagamento = Arredonda.round(pagamento.ajuste());
        BigDecimal valorParcela = pagamento.valorParcela();

        BigDecimal descontoCupomFinal = descontoCupomCalculado;
        if (cupom == Cupom.FRETEGRATIS) {
            descontoCupomFinal = freteOriginal;
        }

        BigDecimal creditoProximaCompra = nivelClube.calcularCredito(subtotalProdutos);
        boolean temBrinde = nivelClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
            Arredonda.round(subtotalProdutos),
            Arredonda.round(descontoCupomFinal),
            Arredonda.round(frete),
            modalidade.prazoDias,
            Arredonda.round(imposto),
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela,
            creditoProximaCompra,
            temBrinde
        );
    }

    private Optional<String> validar(ResumoRequest request) {
        if (request.itens() == null || request.itens().isEmpty()) {
            return Optional.of("PEDIDO_INVALIDO");
        }

        for (Item item : request.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) < 0) {
                return Optional.of("PEDIDO_INVALIDO");
            }
        }

        if (request.nivelClube() == null) {
            return Optional.of("NIVEL_CLUBE_INVALIDO");
        }
        try {
            NivelClube.valueOf(request.nivelClube());
        } catch (IllegalArgumentException e) {
            return Optional.of("NIVEL_CLUBE_INVALIDO");
        }

        if (request.regiao() == null) {
            return Optional.of("REGIAO_INVALIDA");
        }
        try {
            Regiao.valueOf(request.regiao());
        } catch (IllegalArgumentException e) {
            return Optional.of("REGIAO_INVALIDA");
        }

        if (request.modalidadeEntrega() == null) {
            return Optional.of("MODALIDADE_INVALIDA");
        }
        ModalidadeEntrega modalidade;
        try {
            modalidade = ModalidadeEntrega.valueOf(request.modalidadeEntrega());
        } catch (IllegalArgumentException e) {
            return Optional.of("MODALIDADE_INVALIDA");
        }

        BigDecimal pesoTotal = calcularPesoTotal(request.itens());
        if (modalidade == ModalidadeEntrega.MOTOBOY && pesoTotal.compareTo(new BigDecimal("5")) > 0) {
            return Optional.of("MODALIDADE_INDISPONIVEL");
        }

        if (request.cupom() != null) {
            Optional<Cupom> cupomOpt = Cupom.buscar(request.cupom());
            if (cupomOpt.isEmpty()) {
                return Optional.of("CUPOM_INVALIDO");
            }
            BigDecimal subtotal = calcularSubtotalProdutos(request.itens());
            Optional<String> erroAplicacao = cupomOpt.get().validar(subtotal);
            if (erroAplicacao.isPresent()) {
                return erroAplicacao;
            }
        }

        if (request.formaPagamento() == null) {
            return Optional.of("FORMA_PAGAMENTO_INVALIDA");
        }
        FormaPagamento formaPagamento;
        try {
            formaPagamento = FormaPagamento.valueOf(request.formaPagamento());
        } catch (IllegalArgumentException e) {
            return Optional.of("FORMA_PAGAMENTO_INVALIDA");
        }

        int parcelas = request.getParcelas();
        if (!formaPagamento.permite(parcelas)) {
            return Optional.of("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.itens());
        Cupom cupom = request.cupom() != null ? Cupom.valueOf(request.cupom()) : null;
        BigDecimal descontoCupom = cupom != null && !cupom.ehFretegratis()
            ? cupom.calcular(subtotalProdutos, request.itens())
            : BigDecimal.ZERO;
        BigDecimal frete = calcularFreteOriginal(modalidade, pesoTotal);
        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete);

        if (!formaPagamento.disponivel(totalPedido)) {
            return Optional.of("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        return Optional.empty();
    }

    private BigDecimal calcularSubtotalProdutos(List<Item> itens) {
        return Arredonda.round(
            itens.stream()
                .map(item -> item.precoUnitario().multiply(new BigDecimal(item.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    private BigDecimal calcularPesoTotal(List<Item> itens) {
        return itens.stream()
            .map(item -> item.pesoKg().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularFreteOriginal(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        return Arredonda.round(modalidade.calcularFrete(pesoTotal));
    }
}
