package com.lojaroupas.service;

import com.lojaroupas.dto.CheckoutRequest;
import com.lojaroupas.dto.CheckoutResponse;
import com.lojaroupas.dto.ItemCarrinho;
import com.lojaroupas.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final BigDecimal TAXA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");
    private static final BigDecimal LIMITE_MOTOBOY_KG = new BigDecimal("5");
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    public CheckoutResponse calcularCheckout(CheckoutRequest request) throws CheckoutException {
        validarPedido(request);
        validarNivelClube(request);
        validarRegiao(request);
        validarModalidadeEntrega(request);
        validarCupom(request);
        validarFormaPagamento(request);
        validarParcelamento(request);

        NivelClube nivelClube = NivelClube.fromCodigo(request.getNivelClube());
        Regiao regiao = Regiao.fromCodigo(request.getRegiao());
        ModalidadeEntrega modalidadeEntrega = ModalidadeEntrega.fromCodigo(request.getModalidadeEntrega());
        FormaPagamento formaPagamento = FormaPagamento.fromCodigo(request.getFormaPagamento());

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.getItens());
        BigDecimal frete = calcularFrete(modalidadeEntrega, request.getItens(), nivelClube);
        BigDecimal descontoCupom = calcularDescontoCupom(request.getCupom(), subtotalProdutos, frete, request.getItens());
        Integer prazoEntrega = modalidadeEntrega.getPrazoEntregaDias();
        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);

        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);
        totalPedido = arredondar(totalPedido);

        Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        BigDecimal ajustePagamento = calcularAjustePagamento(totalPedido, formaPagamento, parcelas);
        BigDecimal totalFinal = arredondar(totalPedido.add(ajustePagamento));

        BigDecimal valorParcela = arredondar(totalFinal.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN));

        BigDecimal creditoProximaCompra = calcularCredito(subtotalProdutos, nivelClube);
        Boolean brinde = deveDarBrinde(subtotalProdutos, nivelClube);

        return new CheckoutResponse(
                arredondar(subtotalProdutos),
                arredondar(descontoCupom),
                arredondar(frete),
                prazoEntrega,
                arredondar(seguro),
                arredondar(ajustePagamento),
                totalFinal,
                parcelas,
                valorParcela,
                arredondar(creditoProximaCompra),
                brinde
        );
    }

    private void validarPedido(CheckoutRequest request) throws CheckoutException {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarNivelClube(CheckoutRequest request) throws CheckoutException {
        if (request.getNivelClube() == null || NivelClube.fromCodigo(request.getNivelClube()) == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private void validarRegiao(CheckoutRequest request) throws CheckoutException {
        if (request.getRegiao() == null || Regiao.fromCodigo(request.getRegiao()) == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private void validarModalidadeEntrega(CheckoutRequest request) throws CheckoutException {
        if (request.getModalidadeEntrega() == null ||
            ModalidadeEntrega.fromCodigo(request.getModalidadeEntrega()) == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        if ("MOTOBOY".equals(request.getModalidadeEntrega())) {
            BigDecimal pesoTotal = calcularPesoTotal(request.getItens());
            if (pesoTotal.compareTo(LIMITE_MOTOBOY_KG) > 0) {
                throw new CheckoutException("MODALIDADE_INDISPONIVEL");
            }
        }
    }

    private void validarCupom(CheckoutRequest request) throws CheckoutException {
        if (request.getCupom() != null && !request.getCupom().isEmpty()) {
            if (Cupom.fromCodigo(request.getCupom()) == null) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }

            BigDecimal subtotal = calcularSubtotalProdutos(request.getItens());
            if ("MENOS50".equals(request.getCupom())) {
                if (subtotal.compareTo(new BigDecimal("300.00")) < 0) {
                    throw new CheckoutException("CUPOM_NAO_APLICAVEL");
                }
            }
        }
    }

    private void validarFormaPagamento(CheckoutRequest request) throws CheckoutException {
        if (request.getFormaPagamento() == null ||
            FormaPagamento.fromCodigo(request.getFormaPagamento()) == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        BigDecimal subtotal = calcularSubtotalProdutos(request.getItens());
        ModalidadeEntrega modalidadeEntrega = ModalidadeEntrega.fromCodigo(request.getModalidadeEntrega());
        NivelClube nivelClube = NivelClube.fromCodigo(request.getNivelClube());
        Regiao regiao = Regiao.fromCodigo(request.getRegiao());

        BigDecimal frete = calcularFrete(modalidadeEntrega, request.getItens(), nivelClube);
        BigDecimal descontoCupom = calcularDescontoCupom(request.getCupom(), subtotal, frete, request.getItens());
        BigDecimal seguro = calcularSeguro(subtotal, regiao);

        BigDecimal totalAproximado = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if ("BOLETO".equals(request.getFormaPagamento()) && totalAproximado.compareTo(LIMITE_BOLETO) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private void validarParcelamento(CheckoutRequest request) throws CheckoutException {
        Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        String formaPagamento = request.getFormaPagamento();

        if ("PIX".equals(formaPagamento) || "BOLETO".equals(formaPagamento)) {
            if (parcelas != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        } else if ("CARTAO".equals(formaPagamento)) {
            if (parcelas < 1 || parcelas > 12) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal valorItem = item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(valorItem);
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(String codigoCupom, BigDecimal subtotalProdutos,
                                            BigDecimal frete, List<ItemCarrinho> itens) {
        if (codigoCupom == null || codigoCupom.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Cupom cupom = Cupom.fromCodigo(codigoCupom);
        if (cupom == null) {
            return BigDecimal.ZERO;
        }

        switch (cupom) {
            case BEMVINDO10:
                return arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
            case MENOS50:
                return arredondar(new BigDecimal("50.00"));
            case FRETEGRATIS:
                return arredondar(frete);
            case LEVE3PAGUE2:
                return calcularDescontoLeve3Pague2(itens);
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemCarrinho> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            int quantidadeGratis = item.getQuantidade() / 3;
            if (quantidadeGratis > 0) {
                BigDecimal descontoItem = item.getPrecoUnitario().multiply(new BigDecimal(quantidadeGratis));
                desconto = desconto.add(descontoItem);
            }
        }
        return arredondar(desconto);
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, List<ItemCarrinho> itens,
                                     NivelClube nivelClube) {
        if (nivelClube.isContemFreteGratis()) {
            return BigDecimal.ZERO;
        }

        BigDecimal pesoTotal = calcularPesoTotal(itens);
        BigDecimal frete = modalidade.getValorFixo()
                .add(modalidade.getValorPorKg().multiply(pesoTotal));
        return arredondar(frete);
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            pesoTotal = pesoTotal.add(item.getPesoKg().multiply(new BigDecimal(item.getQuantidade())));
        }
        return pesoTotal;
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        return arredondar(subtotalProdutos.multiply(regiao.getPercentualSeguro()));
    }

    private BigDecimal calcularAjustePagamento(BigDecimal totalPedido, FormaPagamento forma,
                                               Integer parcelas) {
        switch (forma) {
            case PIX:
                return arredondar(totalPedido.multiply(new BigDecimal("-0.05")));
            case BOLETO:
                return TAXA_BOLETO;
            case CARTAO:
                if (parcelas > 3) {
                    return calcularJurosCartao(totalPedido, parcelas);
                }
                return BigDecimal.ZERO;
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcularJurosCartao(BigDecimal totalPedido, Integer parcelas) {
        BigDecimal taxa = TAXA_JUROS_MENSAL;
        BigDecimal fatorJuros = BigDecimal.ONE.add(taxa);
        BigDecimal numerador = taxa.multiply(fatorJuros.pow(parcelas));
        BigDecimal denominador = fatorJuros.pow(parcelas).subtract(BigDecimal.ONE);
        BigDecimal parcelaNaoArredondada = totalPedido.multiply(numerador).divide(denominador, 10, RoundingMode.HALF_EVEN);
        BigDecimal parcelaArredondada = arredondar(parcelaNaoArredondada);
        BigDecimal totalComJuros = parcelaArredondada.multiply(new BigDecimal(parcelas));
        return totalComJuros.subtract(totalPedido);
    }

    private BigDecimal calcularCredito(BigDecimal subtotalProdutos, NivelClube nivelClube) {
        if (nivelClube.getPercentualCreditoProdutos().compareTo(BigDecimal.ZERO) > 0) {
            return arredondar(subtotalProdutos.multiply(nivelClube.getPercentualCreditoProdutos()));
        }
        return BigDecimal.ZERO;
    }

    private Boolean deveDarBrinde(BigDecimal subtotalProdutos, NivelClube nivelClube) {
        return nivelClube == NivelClube.OURO && subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

}
