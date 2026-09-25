package com.loja.service;

import com.loja.api.ResumoRequest;
import com.loja.api.ResumoResponse;
import com.loja.domain.Cupom;
import com.loja.domain.FormaPagamento;
import com.loja.domain.Item;
import com.loja.domain.ModalidadeEntrega;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CheckoutService {
    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TAXA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal TAXA_JUROS_CARTAO = new BigDecimal("0.0199");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");

    public ResumoResponse calcularResumo(ResumoRequest request) throws CheckoutException {
        List<Item> itens = converterItens(request.getItens());
        ModalidadeEntrega modalidade = validarModalidade(request.getModalidadeEntrega());
        Cupom cupom = validarCupom(request.getCupom());
        FormaPagamento formaPagamento = validarFormaPagamento(request.getFormaPagamento());
        Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        validarPedido(itens, modalidade, cupom, formaPagamento, parcelas);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);
        BigDecimal freteBase = calcularFreteBase(modalidade, itens);
        BigDecimal frete;
        BigDecimal descontoCupom;

        if (cupom == Cupom.FRETEGRATIS) {
            descontoCupom = freteBase;
            frete = freteBase;
        } else {
            descontoCupom = calcularDescontoCupom(cupom, itens, subtotalProdutos);
            frete = freteBase;
        }

        Integer prazo = modalidade.getPrazo();

        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete);

        BigDecimal totalFinal;
        BigDecimal valorParcela;
        BigDecimal ajustePagamento;

        if (formaPagamento == FormaPagamento.CARTAO && parcelas > 3) {
            valorParcela = calcularParcelaComJuros(totalPedido, parcelas);
            totalFinal = arredondar(valorParcela.multiply(new BigDecimal(parcelas)));
            ajustePagamento = totalFinal.subtract(totalPedido);
        } else {
            ajustePagamento = calcularAjustePagamento(formaPagamento, totalPedido, parcelas);
            totalFinal = totalPedido.add(ajustePagamento);
            valorParcela = calcularValorParcela(formaPagamento, totalFinal, parcelas);
        }

        return new ResumoResponse(
            arredondar(subtotalProdutos),
            arredondar(descontoCupom),
            arredondar(frete),
            prazo,
            arredondar(ajustePagamento),
            arredondar(totalFinal),
            parcelas,
            arredondar(valorParcela)
        );
    }

    private List<Item> converterItens(List<ResumoRequest.ItemRequest> itemRequests) {
        List<Item> itens = new ArrayList<>();
        if (itemRequests != null) {
            for (ResumoRequest.ItemRequest itemReq : itemRequests) {
                itens.add(new Item(itemReq.getNome(), itemReq.getPrecoUnitario(),
                        itemReq.getQuantidade(), itemReq.getPesoKg()));
            }
        }
        return itens;
    }

    private ModalidadeEntrega validarModalidade(String modalidadeStr) throws CheckoutException {
        if (modalidadeStr == null || modalidadeStr.isEmpty()) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        return ModalidadeEntrega.fromString(modalidadeStr)
                .orElseThrow(() -> new CheckoutException("MODALIDADE_INVALIDA"));
    }

    private Cupom validarCupom(String cupomStr) throws CheckoutException {
        if (cupomStr == null || cupomStr.isEmpty()) {
            return null;
        }
        return Cupom.fromString(cupomStr)
                .orElseThrow(() -> new CheckoutException("CUPOM_INVALIDO"));
    }

    private FormaPagamento validarFormaPagamento(String formaPagamentoStr) throws CheckoutException {
        if (formaPagamentoStr == null || formaPagamentoStr.isEmpty()) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        return FormaPagamento.fromString(formaPagamentoStr)
                .orElseThrow(() -> new CheckoutException("FORMA_PAGAMENTO_INVALIDA"));
    }

    private void validarPedido(List<Item> itens, ModalidadeEntrega modalidade, Cupom cupom,
                              FormaPagamento formaPagamento, Integer parcelas) throws CheckoutException {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (Item item : itens) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().signum() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().signum() < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        BigDecimal totalPeso = itens.stream()
                .map(item -> item.getPesoKg().multiply(new BigDecimal(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (modalidade.getPesoMaximo().isPresent() && totalPeso.compareTo(modalidade.getPesoMaximo().get()) > 0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);

        if (cupom != null) {
            if (cupom == Cupom.MENOS50 && subtotalProdutos.compareTo(new BigDecimal("300.00")) < 0) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        validarFormaPagamento(formaPagamento, subtotalProdutos, parcelas);
    }

    private void validarFormaPagamento(FormaPagamento formaPagamento, BigDecimal subtotalProdutos,
                                       Integer parcelas) throws CheckoutException {
        if (parcelas == null || parcelas < 1 || parcelas > 12) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        switch (formaPagamento) {
            case PIX, BOLETO -> {
                if (parcelas != 1) {
                    throw new CheckoutException("PARCELAMENTO_INVALIDO");
                }
            }
            case CARTAO -> {
                if (parcelas < 1 || parcelas > 12) {
                    throw new CheckoutException("PARCELAMENTO_INVALIDO");
                }
            }
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<Item> itens) {
        return itens.stream()
                .map(item -> item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularDescontoCupom(Cupom cupom, List<Item> itens, BigDecimal subtotalProdutos) {
        if (cupom == null) {
            return BigDecimal.ZERO;
        }

        switch (cupom) {
            case BEMVINDO10:
                return arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
            case MENOS50:
                return arredondar(new BigDecimal("50.00"));
            case FRETEGRATIS:
                return null;
            case LEVE3PAGUE2:
                return calcularDescontoLeve3Pague2(itens);
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<Item> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (Item item : itens) {
            int unidadesGratis = item.getQuantidade() / 3;
            BigDecimal descontoItem = item.getPrecoUnitario().multiply(new BigDecimal(unidadesGratis));
            desconto = desconto.add(descontoItem);
        }
        return arredondar(desconto);
    }

    private BigDecimal calcularFreteBase(ModalidadeEntrega modalidade, List<Item> itens) {
        BigDecimal totalPeso = itens.stream()
                .map(item -> item.getPesoKg().multiply(new BigDecimal(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal frete = modalidade.getTaxa().add(
                modalidade.getTaxaPorKg().multiply(totalPeso)
        );

        return arredondar(frete);
    }

    private BigDecimal calcularAjustePagamento(FormaPagamento formaPagamento, BigDecimal totalPedido,
                                               Integer parcelas) throws CheckoutException {
        switch (formaPagamento) {
            case PIX:
                BigDecimal desconto = arredondar(totalPedido.multiply(DESCONTO_PIX));
                return desconto.negate();
            case BOLETO:
                if (totalPedido.compareTo(LIMITE_BOLETO) > 0) {
                    throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
                }
                return arredondar(TAXA_BOLETO);
            case CARTAO:
                return BigDecimal.ZERO;
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcularParcelaComJuros(BigDecimal totalPedido, Integer parcelas) {
        BigDecimal taxa = TAXA_JUROS_CARTAO;
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
        BigDecimal potencia = umMaisTaxa.pow(parcelas);
        BigDecimal numerador = totalPedido.multiply(taxa);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(potencia, 10, RoundingMode.HALF_EVEN));
        return arredondar(numerador.divide(denominador, 10, RoundingMode.HALF_EVEN));
    }

    private BigDecimal calcularValorParcela(FormaPagamento formaPagamento, BigDecimal totalFinal,
                                            Integer parcelas) {
        return arredondar(totalFinal.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN));
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static class CheckoutException extends Exception {
        private final String codigo;

        public CheckoutException(String codigo) {
            super(codigo);
            this.codigo = codigo;
        }

        public String getCodigo() {
            return codigo;
        }
    }
}
