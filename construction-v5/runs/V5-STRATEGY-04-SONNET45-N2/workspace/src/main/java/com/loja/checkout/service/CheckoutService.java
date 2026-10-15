package com.loja.checkout.service;

import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.clube.NivelClubeRegistry;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.cupom.CupomRegistry;
import com.loja.checkout.domain.modalidade.ModalidadeEntrega;
import com.loja.checkout.domain.modalidade.ModalidadeRegistry;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.FormaPagamentoRegistry;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.ItemCarrinho;
import com.loja.checkout.model.PedidoRequest;
import com.loja.checkout.model.Resumo;
import com.loja.checkout.util.Arredondamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    public Resumo calcularResumo(PedidoRequest pedido) {
        validarPedido(pedido);

        NivelClube nivelClube = validarENivelClube(pedido.getNivelClube());
        Regiao regiao = validarRegiao(pedido.getRegiao());
        ModalidadeEntrega modalidade = validarModalidade(pedido.getModalidadeEntrega());

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(pedido.getItens());
        BigDecimal pesoTotal = calcularPesoTotal(pedido.getItens());

        validarModalidadeDisponivel(modalidade, pesoTotal);

        BigDecimal frete = calcularFrete(modalidade, pesoTotal, nivelClube);
        int prazoDias = modalidade.obterPrazoDias();

        Cupom cupom = null;
        BigDecimal descontoCupom = new BigDecimal("0.00");
        if (pedido.getCupom() != null && !pedido.getCupom().isEmpty()) {
            cupom = validarCupom(pedido.getCupom());
            validarCupomAplicavel(cupom, pedido.getItens(), subtotalProdutos);
            descontoCupom = cupom.calcularDesconto(pedido.getItens(), subtotalProdutos, frete);
        }

        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);
        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);

        FormaPagamento formaPagamento = validarFormaPagamento(pedido.getFormaPagamento());
        int parcelas = pedido.getParcelas() != null ? pedido.getParcelas() : 1;

        validarParcelamento(formaPagamento, parcelas);
        validarFormaPagamentoDisponivel(formaPagamento, totalPedido);

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.temBrinde(subtotalProdutos);

        return montarResumo(
            subtotalProdutos,
            descontoCupom,
            frete,
            prazoDias,
            seguro,
            resultadoPagamento,
            parcelas,
            creditoProximaCompra,
            brinde
        );
    }

    private void validarPedido(PedidoRequest pedido) {
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : pedido.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private NivelClube validarENivelClube(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        NivelClube nivel = NivelClubeRegistry.obter(codigo);
        if (nivel == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        return nivel;
    }

    private Regiao validarRegiao(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega validarModalidade(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        ModalidadeEntrega modalidade = ModalidadeRegistry.obter(codigo);
        if (modalidade == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        return modalidade;
    }

    private void validarModalidadeDisponivel(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (!modalidade.estaDisponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private Cupom validarCupom(String codigo) {
        Cupom cupom = CupomRegistry.obter(codigo);
        if (cupom == null) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
        return cupom;
    }

    private void validarCupomAplicavel(Cupom cupom, List<ItemCarrinho> itens, BigDecimal subtotal) {
        if (!cupom.ehAplicavel(itens, subtotal)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }

    private FormaPagamento validarFormaPagamento(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        FormaPagamento forma = FormaPagamentoRegistry.obter(codigo);
        if (forma == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        return forma;
    }

    private void validarParcelamento(FormaPagamento forma, int parcelas) {
        if (!forma.aceitaParcelas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    private void validarFormaPagamentoDisponivel(FormaPagamento forma, BigDecimal totalPedido) {
        if (!forma.estaDisponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (ItemCarrinho item : itens) {
            BigDecimal valorItem = item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(valorItem);
        }

        return Arredondamento.arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;

        for (ItemCarrinho item : itens) {
            BigDecimal pesoItem = item.getPesoKg().multiply(new BigDecimal(item.getQuantidade()));
            pesoTotal = pesoTotal.add(pesoItem);
        }

        return pesoTotal;
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, BigDecimal pesoTotal, NivelClube nivelClube) {
        BigDecimal valorFrete = modalidade.calcularValor(pesoTotal);

        if (nivelClube.temFreteGratis()) {
            return new BigDecimal("0.00");
        }

        return valorFrete;
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        BigDecimal seguro = subtotalProdutos.multiply(regiao.getPercentualSeguro());
        return Arredondamento.arredondar(seguro);
    }

    private Resumo montarResumo(
        BigDecimal subtotalProdutos,
        BigDecimal descontoCupom,
        BigDecimal frete,
        int prazoDias,
        BigDecimal seguro,
        ResultadoPagamento resultadoPagamento,
        int parcelas,
        BigDecimal creditoProximaCompra,
        boolean brinde
    ) {
        Resumo resumo = new Resumo();
        resumo.setSubtotalProdutos(subtotalProdutos);
        resumo.setDescontoCupom(descontoCupom);
        resumo.setFrete(frete);
        resumo.setPrazoEntregaDias(prazoDias);
        resumo.setSeguro(seguro);
        resumo.setAjustePagamento(resultadoPagamento.getAjuste());
        resumo.setTotalFinal(resultadoPagamento.getTotalFinal());
        resumo.setParcelas(parcelas);
        resumo.setValorParcela(resultadoPagamento.getValorParcela());
        resumo.setCreditoProximaCompra(creditoProximaCompra);
        resumo.setBrinde(brinde);
        return resumo;
    }
}
