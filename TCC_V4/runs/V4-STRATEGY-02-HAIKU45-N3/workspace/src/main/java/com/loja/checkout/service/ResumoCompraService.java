package com.loja.checkout.service;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.domain.*;
import com.loja.checkout.exception.ErroCheckout;
import com.loja.checkout.estrategia.*;
import com.loja.checkout.util.Arredondamento;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResumoCompraService {

    public ResumoResponse calcular(ResumoRequest request) throws ErroCheckout {
        validarPedido(request);

        List<ItemRequest> itens = request.itens();
        double subtotalProdutos = calcularSubtotal(itens);

        NivelClube nivelClube = NivelClube.de(request.nivelClube());
        Regiao regiao = Regiao.de(request.regiao());
        ModalidadeEntrega modalidade = ModalidadeEntrega.de(request.modalidadeEntrega());
        Cupom cupom = Cupom.de(request.cupom());
        FormaPagamento formaPagamento = FormaPagamento.de(request.formaPagamento());

        validarNivelClubeExiste(nivelClube);
        validarRegiaoExiste(regiao);
        validarModalidadeExiste(modalidade);
        validarCupomExiste(cupom, request.cupom());
        validarFormaPagamentoExiste(formaPagamento);

        EstrategiaClube estrategiaClube = FabricaClube.criar(nivelClube);
        EstrategiaEntrega estrategiaEntrega = FabricaEntrega.criar(modalidade);
        EstrategiaCupom estrategiaCupom = FabricaCupom.criar(cupom);
        EstrategiaFormaPagamento estrategiaFormaPagamento = FabricaFormaPagamento.criar(formaPagamento);

        validarModalidadeDisponivel(estrategiaEntrega, itens);
        validarCupomAplicavel(estrategiaCupom, subtotalProdutos, itens, cupom);

        double pesoTotal = calcularPesoTotal(itens);

        double frete = estrategiaEntrega.calcularFrete(pesoTotal);

        if (estrategiaClube.ehIsento(request.modalidadeEntrega())) {
            frete = 0.0;
        }

        double descontoCupom = estrategiaCupom.calcularDesconto(subtotalProdutos, frete, itens);
        frete = estrategiaCupom.ajustarFretePorCupom(frete);

        double seguro = calcularSeguro(subtotalProdutos, regiao);
        int prazoEntregaDias = estrategiaEntrega.prazoEntregaDias();

        double totalPedido = Arredondamento.arredondar(subtotalProdutos - descontoCupom + frete + seguro);

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        validarParcelamento(formaPagamento, parcelas, totalPedido);
        validarFormaPagamentoDisponivel(formaPagamento, totalPedido);

        double ajustePagamento = estrategiaFormaPagamento.calcularAjuste(totalPedido, parcelas);
        double totalFinal = Arredondamento.arredondar(totalPedido + ajustePagamento);

        int parcelasNoResponse = estrategiaFormaPagamento.getParcelasNoResponse(parcelas);
        double valorParcela = estrategiaFormaPagamento.getValorParcela(totalFinal, parcelasNoResponse);

        double creditoProximaCompra = estrategiaClube.calcularCredito(subtotalProdutos);
        boolean temBrinde = estrategiaClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            prazoEntregaDias,
            seguro,
            ajustePagamento,
            totalFinal,
            parcelasNoResponse,
            valorParcela,
            creditoProximaCompra,
            temBrinde
        );
    }

    private void validarPedido(ResumoRequest request) throws ErroCheckout {
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }

        for (ItemRequest item : request.itens()) {
            if (item.precoUnitario() <= 0 || item.quantidade() <= 0 || item.pesoKg() <= 0) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarNivelClubeExiste(NivelClube nivelClube) throws ErroCheckout {
        if (nivelClube == null) {
            throw new ErroCheckout("NIVEL_CLUBE_INVALIDO");
        }
    }

    private void validarRegiaoExiste(Regiao regiao) throws ErroCheckout {
        if (regiao == null) {
            throw new ErroCheckout("REGIAO_INVALIDA");
        }
    }

    private void validarModalidadeExiste(ModalidadeEntrega modalidade) throws ErroCheckout {
        if (modalidade == null) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }
    }

    private void validarCupomExiste(Cupom cupom, String cupomOriginal) throws ErroCheckout {
        if (cupomOriginal != null && cupom == null) {
            throw new ErroCheckout("CUPOM_INVALIDO");
        }
    }

    private void validarFormaPagamentoExiste(FormaPagamento formaPagamento) throws ErroCheckout {
        if (formaPagamento == null) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private void validarModalidadeDisponivel(EstrategiaEntrega estrategia, List<ItemRequest> itens) throws ErroCheckout {
        double pesoTotal = calcularPesoTotal(itens);
        if (!estrategia.disponivel(pesoTotal)) {
            throw new ErroCheckout("MODALIDADE_INDISPONIVEL");
        }
    }

    private void validarCupomAplicavel(EstrategiaCupom estrategia, double subtotal, List<ItemRequest> itens, Cupom cupom) throws ErroCheckout {
        if (cupom == null) {
            return;
        }
        if (!estrategia.aplicavel(subtotal, itens)) {
            throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
        }
    }

    private void validarParcelamento(FormaPagamento forma, int parcelas, double total) throws ErroCheckout {
        if (forma == FormaPagamento.PIX || forma == FormaPagamento.BOLETO) {
            if (parcelas != 1) {
                throw new ErroCheckout("PARCELAMENTO_INVALIDO");
            }
        } else if (forma == FormaPagamento.CARTAO) {
            if (parcelas < 1 || parcelas > 12) {
                throw new ErroCheckout("PARCELAMENTO_INVALIDO");
            }
        }
    }

    private void validarFormaPagamentoDisponivel(FormaPagamento forma, double total) throws ErroCheckout {
        if (forma == FormaPagamento.BOLETO && total > 1000.00) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private double calcularSubtotal(List<ItemRequest> itens) {
        double subtotal = 0.0;
        for (ItemRequest item : itens) {
            subtotal += item.precoUnitario() * item.quantidade();
        }
        return Arredondamento.arredondar(subtotal);
    }

    private double calcularPesoTotal(List<ItemRequest> itens) {
        double peso = 0.0;
        for (ItemRequest item : itens) {
            peso += item.pesoKg() * item.quantidade();
        }
        return peso;
    }

    private double calcularSeguro(double subtotalProdutos, Regiao regiao) {
        double percentual = switch (regiao) {
            case SUDESTE, SUL -> 0.01;
            case CENTRO_OESTE -> 0.015;
            case NORTE -> 0.025;
            case NORDESTE -> 0.02;
        };
        return Arredondamento.arredondar(subtotalProdutos * percentual);
    }
}
