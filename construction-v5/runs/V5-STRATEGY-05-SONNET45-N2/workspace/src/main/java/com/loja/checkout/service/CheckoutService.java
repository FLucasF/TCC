package com.loja.checkout.service;

import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.cupom.CupomFactory;
import com.loja.checkout.domain.modalidade.ModalidadeEntrega;
import com.loja.checkout.domain.modalidade.ModalidadeEntregaFactory;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.FormaPagamentoFactory;
import com.loja.checkout.domain.regiao.Regiao;
import com.loja.checkout.model.request.CheckoutRequest;
import com.loja.checkout.model.request.ItemRequest;
import com.loja.checkout.model.response.CheckoutResponse;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarPedido(request);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.getItens());
        BigDecimal pesoTotal = calcularPesoTotal(request.getItens());

        ModalidadeEntrega modalidade = obterModalidade(request.getModalidadeEntrega());
        validarDisponibilidadeModalidade(modalidade, pesoTotal);

        Cupom cupom = obterCupom(request.getCupom());

        BigDecimal freteCalculado = Dinheiro.arredondar(modalidade.calcularFrete(pesoTotal));
        BigDecimal frete = request.getNivelClube().isentaFrete() ? Dinheiro.zero() : freteCalculado;

        BigDecimal descontoCupom = calcularDescontoCupom(cupom, subtotalProdutos, freteCalculado, request.getItens());
        validarAplicabilidadeCupom(cupom, subtotalProdutos, freteCalculado, request.getItens());

        BigDecimal seguro = calcularSeguro(subtotalProdutos, request.getRegiao());
        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);

        FormaPagamento formaPagamento = obterFormaPagamento(request.getFormaPagamento());
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        validarParcelamento(formaPagamento, parcelas);
        validarDisponibilidadeFormaPagamento(formaPagamento, totalPedido);

        BigDecimal ajustePagamento = Dinheiro.arredondar(formaPagamento.calcularAjuste(totalPedido, parcelas));
        BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(ajustePagamento));
        BigDecimal valorParcela = Dinheiro.arredondar(formaPagamento.calcularValorParcela(totalPedido, parcelas));

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(
            request.getNivelClube().calcularCredito(subtotalProdutos)
        );

        boolean brinde = request.getNivelClube().ganhaBrinde(subtotalProdutos);

        return construirResposta(
            subtotalProdutos,
            descontoCupom,
            frete,
            modalidade.getPrazoEntregaDias(),
            seguro,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela,
            creditoProximaCompra,
            brinde
        );
    }

    private void validarPedido(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new IllegalArgumentException("PEDIDO_INVALIDO");
        }

        for (ItemRequest item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("PEDIDO_INVALIDO");
            }
        }

        if (request.getNivelClube() == null) {
            throw new IllegalArgumentException("NIVEL_CLUBE_INVALIDO");
        }

        if (request.getRegiao() == null) {
            throw new IllegalArgumentException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega obterModalidade(String codigo) {
        if (codigo == null) {
            throw new IllegalArgumentException("MODALIDADE_INVALIDA");
        }

        ModalidadeEntrega modalidade = ModalidadeEntregaFactory.obter(codigo);
        if (modalidade == null) {
            throw new IllegalArgumentException("MODALIDADE_INVALIDA");
        }

        return modalidade;
    }

    private void validarDisponibilidadeModalidade(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (!modalidade.estaDisponivel(pesoTotal)) {
            throw new IllegalArgumentException("MODALIDADE_INDISPONIVEL");
        }
    }

    private Cupom obterCupom(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            return null;
        }

        Cupom cupom = CupomFactory.obter(codigo);
        if (cupom == null) {
            throw new IllegalArgumentException("CUPOM_INVALIDO");
        }

        return cupom;
    }

    private void validarAplicabilidadeCupom(Cupom cupom, BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        if (cupom != null && !cupom.eAplicavel(subtotalProdutos, frete, itens)) {
            throw new IllegalArgumentException("CUPOM_NAO_APLICAVEL");
        }
    }

    private FormaPagamento obterFormaPagamento(String codigo) {
        if (codigo == null) {
            throw new IllegalArgumentException("FORMA_PAGAMENTO_INVALIDA");
        }

        FormaPagamento formaPagamento = FormaPagamentoFactory.obter(codigo);
        if (formaPagamento == null) {
            throw new IllegalArgumentException("FORMA_PAGAMENTO_INVALIDA");
        }

        return formaPagamento;
    }

    private void validarParcelamento(FormaPagamento formaPagamento, int parcelas) {
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new IllegalArgumentException("PARCELAMENTO_INVALIDO");
        }
    }

    private void validarDisponibilidadeFormaPagamento(FormaPagamento formaPagamento, BigDecimal totalPedido) {
        if (!formaPagamento.estaDisponivel(totalPedido)) {
            throw new IllegalArgumentException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (ItemRequest item : itens) {
            BigDecimal valorItem = item.getPrecoUnitario()
                .multiply(BigDecimal.valueOf(item.getQuantidade()));
            subtotal = subtotal.add(valorItem);
        }

        return Dinheiro.arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;

        for (ItemRequest item : itens) {
            BigDecimal pesoItem = item.getPesoKg()
                .multiply(BigDecimal.valueOf(item.getQuantidade()));
            pesoTotal = pesoTotal.add(pesoItem);
        }

        return pesoTotal;
    }

    private BigDecimal calcularDescontoCupom(Cupom cupom, BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        if (cupom == null) {
            return Dinheiro.zero();
        }

        return Dinheiro.arredondar(cupom.calcularDesconto(subtotalProdutos, frete, itens));
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        return Dinheiro.arredondar(
            subtotalProdutos.multiply(regiao.getPercentualSeguro())
        );
    }

    private CheckoutResponse construirResposta(
        BigDecimal subtotalProdutos,
        BigDecimal descontoCupom,
        BigDecimal frete,
        int prazoEntregaDias,
        BigDecimal seguro,
        BigDecimal ajustePagamento,
        BigDecimal totalFinal,
        int parcelas,
        BigDecimal valorParcela,
        BigDecimal creditoProximaCompra,
        boolean brinde
    ) {
        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(subtotalProdutos);
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(prazoEntregaDias);
        response.setSeguro(seguro);
        response.setAjustePagamento(ajustePagamento);
        response.setTotalFinal(totalFinal);
        response.setParcelas(parcelas);
        response.setValorParcela(valorParcela);
        response.setCreditoProximaCompra(creditoProximaCompra);
        response.setBrinde(brinde);
        return response;
    }
}
