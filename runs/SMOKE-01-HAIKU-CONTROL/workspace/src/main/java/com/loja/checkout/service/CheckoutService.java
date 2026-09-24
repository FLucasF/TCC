package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemPedidoRequest;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.Cupom;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.ModalidadeEntrega;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {

    private static final BigDecimal PIX_DESCONTO_PERCENTUAL = new BigDecimal("0.05");
    private static final BigDecimal CARTAO_TAXA_MENSAL = new BigDecimal("0.0199");
    private static final BigDecimal BOLETO_TARIFA = new BigDecimal("3.49");
    private static final BigDecimal BOLETO_LIMITE = new BigDecimal("1000.00");
    private static final BigDecimal MOTOBOY_PESO_LIMITE = new BigDecimal("5.00");

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarPedido(request);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.getItens());

        ModalidadeEntrega modalidade = validarEAcualizarModalidade(request.getModalidadeEntrega(), request.getItens());
        BigDecimal pesoTotal = calcularPesoTotal(request.getItens());
        BigDecimal frete = calcularFrete(modalidade, pesoTotal);

        Cupom cupom = validarEAcualizarCupom(request.getCupom(), subtotalProdutos, modalidade.equals(ModalidadeEntrega.RETIRADA_LOJA));
        BigDecimal descontoCupom = calcularDescontoCupom(cupom, subtotalProdutos, request.getItens(), frete);

        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete);
        totalPedido = arredondar(totalPedido);

        FormaPagamento forma = validarFormaPagamento(request.getFormaPagamento());
        Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        validarParcelamento(forma, parcelas, totalPedido);

        BigDecimal valorParcela = calcularValorParcela(forma, parcelas, totalPedido);
        BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
        totalFinal = arredondar(totalFinal);

        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        ajuste = arredondar(ajuste);

        return new CheckoutResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            modalidade.getPrazoDias(),
            ajuste,
            totalFinal,
            parcelas,
            valorParcela
        );
    }

    private void validarPedido(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemPedidoRequest item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().signum() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().signum() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemPedidoRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedidoRequest item : itens) {
            BigDecimal itemTotal = item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(itemTotal);
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemPedidoRequest> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemPedidoRequest item : itens) {
            BigDecimal pesoPorItem = item.getPesoKg().multiply(new BigDecimal(item.getQuantidade()));
            pesoTotal = pesoTotal.add(pesoPorItem);
        }
        return pesoTotal;
    }

    private ModalidadeEntrega validarEAcualizarModalidade(String modalidadeStr, List<ItemPedidoRequest> itens) {
        ModalidadeEntrega modalidade = ModalidadeEntrega.fromString(modalidadeStr);

        if (modalidade == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        if (modalidade.equals(ModalidadeEntrega.MOTOBOY)) {
            BigDecimal pesoTotal = calcularPesoTotal(itens);
            if (pesoTotal.compareTo(MOTOBOY_PESO_LIMITE) > 0) {
                throw new CheckoutException("MODALIDADE_INDISPONIVEL");
            }
        }

        return modalidade;
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        BigDecimal frete = modalidade.getValorFixo().add(
            modalidade.getValorPorKg().multiply(pesoTotal)
        );
        return arredondar(frete);
    }

    private Cupom validarEAcualizarCupom(String cupomStr, BigDecimal subtotalProdutos, boolean isRetiradaLoja) {
        if (cupomStr == null || cupomStr.isEmpty()) {
            return null;
        }

        Cupom cupom = Cupom.fromString(cupomStr);
        if (cupom == null) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }

        if (cupom.equals(Cupom.MENOS50)) {
            if (subtotalProdutos.compareTo(new BigDecimal("300.00")) < 0) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        return cupom;
    }

    private BigDecimal calcularDescontoCupom(Cupom cupom, BigDecimal subtotalProdutos, List<ItemPedidoRequest> itens, BigDecimal frete) {
        if (cupom == null) {
            return BigDecimal.ZERO;
        }

        switch (cupom) {
            case BEMVINDO10:
                return arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));

            case MENOS50:
                return new BigDecimal("50.00");

            case FRETEGRATIS:
                return arredondar(frete);

            case LEVE3PAGUE2:
                return calcularDescontoLeve3Pague2(itens);

            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemPedidoRequest> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemPedidoRequest item : itens) {
            int unidadesGratis = item.getQuantidade() / 3;
            if (unidadesGratis > 0) {
                BigDecimal descontoItem = item.getPrecoUnitario().multiply(new BigDecimal(unidadesGratis));
                desconto = desconto.add(descontoItem);
            }
        }
        return arredondar(desconto);
    }

    private FormaPagamento validarFormaPagamento(String formaPagamentoStr) {
        FormaPagamento forma = FormaPagamento.fromString(formaPagamentoStr);
        if (forma == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        return forma;
    }

    private void validarParcelamento(FormaPagamento forma, Integer parcelas, BigDecimal totalPedido) {
        if (forma.equals(FormaPagamento.PIX) || forma.equals(FormaPagamento.BOLETO)) {
            if (parcelas != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        } else if (forma.equals(FormaPagamento.CARTAO)) {
            if (parcelas < 1 || parcelas > 12) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }

        if (forma.equals(FormaPagamento.BOLETO)) {
            if (totalPedido.compareTo(BOLETO_LIMITE) > 0) {
                throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
            }
        }
    }

    private BigDecimal calcularAjustePagamento(FormaPagamento forma, Integer parcelas, BigDecimal totalPedido) {
        switch (forma) {
            case PIX:
                return arredondar(totalPedido.multiply(PIX_DESCONTO_PERCENTUAL).negate());

            case BOLETO:
                return BOLETO_TARIFA;

            case CARTAO:
                if (parcelas <= 3) {
                    return BigDecimal.ZERO;
                } else {
                    return calcularJurosCartao(totalPedido, parcelas);
                }

            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcularJurosCartao(BigDecimal totalPedido, Integer parcelas) {
        BigDecimal taxa = CARTAO_TAXA_MENSAL;
        BigDecimal parcBig = new BigDecimal(parcelas);

        BigDecimal numerador = taxa.multiply(BigDecimal.ONE.add(taxa).pow(parcelas));
        BigDecimal denominador = BigDecimal.ONE.add(taxa).pow(parcelas).subtract(BigDecimal.ONE);
        BigDecimal parcela = totalPedido.multiply(numerador.divide(denominador, 10, RoundingMode.HALF_EVEN));
        parcela = arredondar(parcela);

        BigDecimal totalComJuros = parcela.multiply(parcBig);
        BigDecimal juros = totalComJuros.subtract(totalPedido);
        return arredondar(juros);
    }

    private BigDecimal calcularValorParcela(FormaPagamento forma, Integer parcelas, BigDecimal totalPedido) {
        if (forma.equals(FormaPagamento.CARTAO) && parcelas > 3) {
            BigDecimal taxa = CARTAO_TAXA_MENSAL;
            BigDecimal numerador = taxa.multiply(BigDecimal.ONE.add(taxa).pow(parcelas));
            BigDecimal denominador = BigDecimal.ONE.add(taxa).pow(parcelas).subtract(BigDecimal.ONE);
            BigDecimal parcela = totalPedido.multiply(numerador.divide(denominador, 10, RoundingMode.HALF_EVEN));
            return arredondar(parcela);
        } else {
            BigDecimal ajuste = calcularAjusteFormaPagamento(forma, totalPedido);
            BigDecimal totalComAjuste = totalPedido.add(ajuste);
            return arredondar(totalComAjuste.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN));
        }
    }

    private BigDecimal calcularAjusteFormaPagamento(FormaPagamento forma, BigDecimal totalPedido) {
        switch (forma) {
            case PIX:
                return arredondar(totalPedido.multiply(PIX_DESCONTO_PERCENTUAL).negate());
            case BOLETO:
                return BOLETO_TARIFA;
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
