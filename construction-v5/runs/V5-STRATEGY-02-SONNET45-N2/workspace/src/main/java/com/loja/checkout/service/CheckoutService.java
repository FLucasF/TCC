package com.loja.checkout.service;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.model.Item;
import com.loja.checkout.model.Pedido;
import com.loja.checkout.model.Resumo;
import com.loja.checkout.pagamento.FormaPagamento;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class CheckoutService {
    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveisClube;
    private final Map<String, FormaPagamento> formasPagamento;

    public CheckoutService(
        Map<String, ModalidadeEntrega> modalidades,
        Map<String, Cupom> cupons,
        Map<String, NivelClube> niveisClube,
        Map<String, FormaPagamento> formasPagamento
    ) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.niveisClube = niveisClube;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcularResumo(PedidoRequest request) {
        validarPedido(request);

        Regiao regiao = obterRegiao(request.regiao());
        NivelClube nivelClube = obterNivelClube(request.nivelClube());
        ModalidadeEntrega modalidade = obterModalidadeEntrega(request.modalidadeEntrega());
        FormaPagamento formaPagamento = obterFormaPagamento(request.formaPagamento());
        Integer parcelas = request.parcelas() != null ? request.parcelas() : 1;

        Pedido pedido = construirPedido(request, regiao);

        modalidade.validarDisponibilidade(pedido);

        Resumo resumo = new Resumo();
        resumo.setSubtotalProdutos(pedido.subtotalProdutos());

        BigDecimal frete = modalidade.calcularFrete(pedido);
        frete = nivelClube.ajustarFrete(frete);
        resumo.setFrete(frete);
        resumo.setPrazoEntregaDias(modalidade.prazoEntregaDias());

        if (request.cupom() != null && !request.cupom().isBlank()) {
            Cupom cupom = obterCupom(request.cupom());
            cupom.validarAplicabilidade(pedido);
            BigDecimal desconto = cupom.calcularDesconto(pedido, resumo);
            resumo.setDescontoCupom(desconto);
        }

        BigDecimal seguro = pedido.subtotalProdutos()
            .multiply(regiao.getPercentualSeguro())
            .setScale(2, RoundingMode.HALF_EVEN);
        resumo.setSeguro(seguro);

        formaPagamento.validarParcelas(parcelas);
        formaPagamento.validarDisponibilidade(resumo);

        BigDecimal ajuste = formaPagamento.calcularAjuste(resumo.totalPedido(), parcelas);
        BigDecimal totalFinal = resumo.totalPedido().add(ajuste);
        BigDecimal valorParcela = formaPagamento.calcularValorParcela(resumo.totalPedido(), parcelas);

        resumo.setAjustePagamento(ajuste);
        resumo.setTotalFinal(totalFinal);
        resumo.setParcelas(parcelas);
        resumo.setValorParcela(valorParcela);

        BigDecimal credito = nivelClube.calcularCredito(pedido);
        resumo.setCreditoProximaCompra(credito);

        Boolean brinde = nivelClube.temBrinde(pedido);
        resumo.setBrinde(brinde);

        return new ResumoResponse(
            resumo.getSubtotalProdutos(),
            resumo.getDescontoCupom(),
            resumo.getFrete(),
            resumo.getPrazoEntregaDias(),
            resumo.getSeguro(),
            resumo.getAjustePagamento(),
            resumo.getTotalFinal(),
            resumo.getParcelas(),
            resumo.getValorParcela(),
            resumo.getCreditoProximaCompra(),
            resumo.getBrinde()
        );
    }

    private void validarPedido(PedidoRequest request) {
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }

        for (ItemRequest item : request.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() == null || item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
        }
    }

    private Pedido construirPedido(PedidoRequest request, Regiao regiao) {
        var itens = request.itens().stream()
            .map(i -> new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()))
            .toList();
        return new Pedido(itens, regiao);
    }

    private Regiao obterRegiao(String nome) {
        if (nome == null) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }
        try {
            return Regiao.valueOf(nome);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }
    }

    private NivelClube obterNivelClube(String nome) {
        if (nome == null) {
            throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
        NivelClube nivel = niveisClube.get(nome);
        if (nivel == null) {
            throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
        return nivel;
    }

    private ModalidadeEntrega obterModalidadeEntrega(String nome) {
        if (nome == null) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }
        ModalidadeEntrega modalidade = modalidades.get(nome);
        if (modalidade == null) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }
        return modalidade;
    }

    private Cupom obterCupom(String codigo) {
        Cupom cupom = cupons.get(codigo);
        if (cupom == null) {
            throw new CheckoutException(CodigoErro.CUPOM_INVALIDO);
        }
        return cupom;
    }

    private FormaPagamento obterFormaPagamento(String nome) {
        if (nome == null) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        FormaPagamento forma = formasPagamento.get(nome);
        if (forma == null) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        return forma;
    }
}
