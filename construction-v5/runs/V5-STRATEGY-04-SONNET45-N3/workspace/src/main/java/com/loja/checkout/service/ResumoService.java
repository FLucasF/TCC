package com.loja.checkout.service;

import com.loja.checkout.domain.*;
import com.loja.checkout.domain.clube.*;
import com.loja.checkout.domain.cupom.*;
import com.loja.checkout.domain.entrega.*;
import com.loja.checkout.domain.pagamento.*;
import com.loja.checkout.model.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class ResumoService {
    private final RegistroCupons registroCupons;
    private final RegistroModalidades registroModalidades;
    private final RegistroNiveis registroNiveis;
    private final RegistroFormasPagamento registroFormasPagamento;

    public ResumoService(
        RegistroCupons registroCupons,
        RegistroModalidades registroModalidades,
        RegistroNiveis registroNiveis,
        RegistroFormasPagamento registroFormasPagamento
    ) {
        this.registroCupons = registroCupons;
        this.registroModalidades = registroModalidades;
        this.registroNiveis = registroNiveis;
        this.registroFormasPagamento = registroFormasPagamento;
    }

    public Object calcular(PedidoRequest request) {
        if (!pedidoValido(request)) {
            return new ErroResponse("PEDIDO_INVALIDO");
        }

        if (request.nivelClube() == null) {
            return new ErroResponse("NIVEL_CLUBE_INVALIDO");
        }
        NivelClube nivelClube = registroNiveis.buscar(request.nivelClube())
            .orElse(null);
        if (nivelClube == null) {
            return new ErroResponse("NIVEL_CLUBE_INVALIDO");
        }

        if (request.regiao() == null) {
            return new ErroResponse("REGIAO_INVALIDA");
        }
        Regiao regiao;
        try {
            regiao = Regiao.valueOf(request.regiao());
        } catch (IllegalArgumentException e) {
            return new ErroResponse("REGIAO_INVALIDA");
        }

        if (request.modalidadeEntrega() == null) {
            return new ErroResponse("MODALIDADE_INVALIDA");
        }
        ModalidadeEntrega modalidade = registroModalidades.buscar(request.modalidadeEntrega())
            .orElse(null);
        if (modalidade == null) {
            return new ErroResponse("MODALIDADE_INVALIDA");
        }

        ContextoCalculo contexto = new ContextoCalculo(request.itens());
        BigDecimal pesoTotal = contexto.getPesoTotal();

        if (!modalidade.estaDisponivel(pesoTotal)) {
            return new ErroResponse("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.itens());
        contexto.setSubtotalProdutos(subtotalProdutos);

        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        if (nivelClube.freteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }
        contexto.setFrete(frete);

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (request.cupom() != null && !request.cupom().isBlank()) {
            Cupom cupom = registroCupons.buscar(request.cupom()).orElse(null);
            if (cupom == null) {
                return new ErroResponse("CUPOM_INVALIDO");
            }
            if (!cupom.aplicavel(contexto)) {
                return new ErroResponse("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(contexto);
        }

        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);
        totalPedido = Arredondamento.arredondar(totalPedido);

        if (request.formaPagamento() == null) {
            return new ErroResponse("FORMA_PAGAMENTO_INVALIDA");
        }
        FormaPagamento formaPagamento = registroFormasPagamento.buscar(request.formaPagamento())
            .orElse(null);
        if (formaPagamento == null) {
            return new ErroResponse("FORMA_PAGAMENTO_INVALIDA");
        }

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            return new ErroResponse("PARCELAMENTO_INVALIDO");
        }

        if (!formaPagamento.estaDisponivel(totalPedido)) {
            return new ErroResponse("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        BigDecimal ajustePagamento = formaPagamento.calcularAjuste(totalPedido, parcelas);
        BigDecimal totalFinal = formaPagamento.calcularValorFinal(totalPedido, parcelas);
        BigDecimal valorParcela = formaPagamento.calcularValorParcela(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.ganhaBrinde(subtotalProdutos);

        return new ResumoResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            modalidade.obterPrazo(),
            seguro,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela,
            creditoProximaCompra,
            brinde
        );
    }

    private boolean pedidoValido(PedidoRequest request) {
        if (request.itens() == null || request.itens().isEmpty()) {
            return false;
        }
        return request.itens().stream().allMatch(ItemCarrinho::valido);
    }

    private BigDecimal calcularSubtotalProdutos(java.util.List<ItemCarrinho> itens) {
        BigDecimal subtotal = itens.stream()
            .map(ItemCarrinho::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Arredondamento.arredondar(subtotal);
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        BigDecimal seguro = subtotalProdutos.multiply(regiao.getPercentualSeguro());
        return Arredondamento.arredondar(seguro);
    }
}
