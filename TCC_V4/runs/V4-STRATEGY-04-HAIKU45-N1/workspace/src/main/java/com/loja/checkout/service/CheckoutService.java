package com.loja.checkout.service;

import com.loja.checkout.domain.*;
import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.model.CheckoutResponse;
import com.loja.checkout.model.ItemCarrinho;
import com.loja.checkout.util.Arredondador;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarRequisicao(request);

        NivelClube nivelClube = NivelClube.parse(request.getNivelClube());
        Regiao regiao = Regiao.parse(request.getRegiao());
        ModalidadeEntrega modalidade = ModalidadeEntrega.parse(request.getModalidadeEntrega());
        Cupom cupom = request.getCupom() != null ? Cupom.parse(request.getCupom()) : null;
        FormaPagamento formaPagamento = FormaPagamento.parse(request.getFormaPagamento());
        Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        Double pesoTotal = calcularPesoTotal(request.getItens());
        Double subtotalProdutos = calcularSubtotalComCupom(request.getItens(), cupom);
        subtotalProdutos = Arredondador.arredondar(subtotalProdutos);

        Double descontoCupom = calcularDescontoCupom(cupom, subtotalProdutos, pesoTotal, modalidade, request.getItens());
        descontoCupom = Arredondador.arredondar(descontoCupom);

        Double frete = calcularFrete(modalidade, pesoTotal, nivelClube);
        frete = Arredondador.arredondar(frete);

        Integer prazoEntrega = modalidade.getPrazo();

        Double seguro = calcularSeguro(subtotalProdutos, regiao);
        seguro = Arredondador.arredondar(seguro);

        Double totalPedido = subtotalProdutos - descontoCupom + frete + seguro;
        totalPedido = Arredondador.arredondar(totalPedido);

        Double valorParcela;
        Double ajustePagamento;
        Double totalFinal;

        if (formaPagamento == FormaPagamento.CARTAO && parcelas > 3) {
            valorParcela = calcularValorParcelaComJuros(totalPedido, parcelas);
            valorParcela = Arredondador.arredondar(valorParcela);
            totalFinal = valorParcela * parcelas;
            totalFinal = Arredondador.arredondar(totalFinal);
            ajustePagamento = totalFinal - totalPedido;
            ajustePagamento = Arredondador.arredondar(ajustePagamento);
        } else {
            ajustePagamento = calcularAjusteFormaPagamento(formaPagamento, totalPedido);
            ajustePagamento = Arredondador.arredondar(ajustePagamento);
            totalFinal = totalPedido + ajustePagamento;
            totalFinal = Arredondador.arredondar(totalFinal);
            valorParcela = totalFinal / parcelas;
            valorParcela = Arredondador.arredondar(valorParcela);
        }

        validarFormaPagamentoDisponivel(formaPagamento, totalFinal);

        Double creditoProximaCompra = calcularCredito(nivelClube, subtotalProdutos);
        creditoProximaCompra = Arredondador.arredondar(creditoProximaCompra);

        Boolean brinde = nivelClube.daBrinde(subtotalProdutos);

        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(subtotalProdutos);
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(prazoEntrega);
        response.setSeguro(seguro);
        response.setAjustePagamento(ajustePagamento);
        response.setTotalFinal(totalFinal);
        response.setParcelas(parcelas);
        response.setValorParcela(valorParcela);
        response.setCreditoProximaCompra(creditoProximaCompra);
        response.setBrinde(brinde);

        return response;
    }

    private void validarRequisicao(CheckoutRequest request) {
        validarItens(request.getItens());
        validarNivelClube(request.getNivelClube());
        validarRegiao(request.getRegiao());
        validarModalidadeEntrega(request.getModalidadeEntrega());
        validarFormaPagamento(request.getFormaPagamento(), request.getParcelas());
        validarCupom(request.getCupom(), request.getItens(), request.getModalidadeEntrega());
    }

    private void validarItens(List<ItemCarrinho> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : itens) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() < 0) {
                throw new IllegalArgumentException("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarNivelClube(String nivelClube) {
        if (nivelClube == null || NivelClube.parse(nivelClube) == null) {
            throw new IllegalArgumentException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private void validarRegiao(String regiao) {
        if (regiao == null || Regiao.parse(regiao) == null) {
            throw new IllegalArgumentException("REGIAO_INVALIDA");
        }
    }

    private void validarModalidadeEntrega(String modalidadeEntrega) {
        if (modalidadeEntrega == null || ModalidadeEntrega.parse(modalidadeEntrega) == null) {
            throw new IllegalArgumentException("MODALIDADE_INVALIDA");
        }
    }

    private void validarModalidadeDisponivel(ModalidadeEntrega modalidade, Double pesoTotal) {
        if (modalidade == ModalidadeEntrega.MOTOBOY && pesoTotal > 5.0) {
            throw new IllegalArgumentException("MODALIDADE_INDISPONIVEL");
        }
    }

    private void validarFormaPagamento(String formaPagamento, Integer parcelas) {
        if (formaPagamento == null || FormaPagamento.parse(formaPagamento) == null) {
            throw new IllegalArgumentException("FORMA_PAGAMENTO_INVALIDA");
        }

        Integer numParcelas = parcelas != null ? parcelas : 1;
        FormaPagamento forma = FormaPagamento.parse(formaPagamento);

        if (!forma.permiteParcelas(numParcelas)) {
            throw new IllegalArgumentException("PARCELAMENTO_INVALIDO");
        }
    }

    private void validarFormaPagamentoDisponivel(FormaPagamento formaPagamento, Double totalFinal) {
        if (!formaPagamento.verificaLimiteTotal(totalFinal)) {
            throw new IllegalArgumentException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private void validarCupom(String cupomCodigo, List<ItemCarrinho> itens, String modalidadeEntregaStr) {
        if (cupomCodigo == null) {
            return;
        }

        Cupom cupom = Cupom.parse(cupomCodigo);
        if (cupom == null) {
            throw new IllegalArgumentException("CUPOM_INVALIDO");
        }

        Double pesoTotal = calcularPesoTotal(itens);
        Double subtotalProdutos = calcularSubtotalProdutos(itens);
        ModalidadeEntrega modalidade = ModalidadeEntrega.parse(modalidadeEntregaStr);

        if (!cupom.validar(subtotalProdutos, pesoTotal, modalidade)) {
            throw new IllegalArgumentException("CUPOM_NAO_APLICAVEL");
        }
    }

    private Double calcularPesoTotal(List<ItemCarrinho> itens) {
        return itens.stream()
                .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
                .sum();
    }

    private Double calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        return itens.stream()
                .mapToDouble(item -> item.getPrecoUnitario() * item.getQuantidade())
                .sum();
    }

    private Double calcularSubtotalComCupom(List<ItemCarrinho> itens, Cupom cupom) {
        return calcularSubtotalProdutos(itens);
    }

    private Double calcularDescontoCupom(Cupom cupom, Double subtotalProdutos,
                                        Double pesoTotal, ModalidadeEntrega modalidade, List<ItemCarrinho> itens) {
        if (cupom == null) {
            return 0.0;
        }

        if (cupom == Cupom.LEVE3PAGUE2) {
            Double desconto = 0.0;
            for (ItemCarrinho item : itens) {
                Integer unidadesGratis = item.getQuantidade() / 3;
                desconto += item.getPrecoUnitario() * unidadesGratis;
            }
            return desconto;
        }

        return cupom.calcular(subtotalProdutos, pesoTotal, modalidade);
    }

    private Double calcularFrete(ModalidadeEntrega modalidade, Double pesoTotal, NivelClube nivelClube) {
        validarModalidadeDisponivel(modalidade, pesoTotal);

        if (nivelClube == NivelClube.OURO) {
            return 0.0;
        }

        return modalidade.calcularFrete(pesoTotal);
    }

    private Double calcularSeguro(Double subtotalProdutos, Regiao regiao) {
        return subtotalProdutos * regiao.getTaxaSeguro();
    }

    private Double calcularAjustePagamento(FormaPagamento formaPagamento, Double totalFinal, Integer parcelas) {
        Double ajuste = formaPagamento.calcularAjuste(totalFinal, parcelas);
        validarFormaPagamentoDisponivel(formaPagamento, totalFinal + ajuste);
        return ajuste;
    }

    private Double calcularValorParcelaComJuros(Double totalPedido, Integer parcelas) {
        double taxaMensal = 0.0199;
        double parcela = totalPedido * taxaMensal / (1.0 - Math.pow(1.0 + taxaMensal, -parcelas));
        long parcelaRounded = Math.round(parcela * 100);
        return parcelaRounded / 100.0;
    }

    private Double calcularAjusteFormaPagamento(FormaPagamento formaPagamento, Double totalPedido) {
        if (formaPagamento == FormaPagamento.PIX) {
            return -totalPedido * 0.05;
        } else if (formaPagamento == FormaPagamento.BOLETO) {
            return 3.49;
        }
        return 0.0;
    }

    private Double calcularCredito(NivelClube nivelClube, Double subtotalProdutos) {
        return nivelClube.calcularCredito(subtotalProdutos);
    }
}
