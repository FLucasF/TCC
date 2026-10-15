package com.loja.service;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ItemRequest;
import com.loja.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    private static final BigDecimal PIX_DESCONTO = new BigDecimal("0.05");
    private static final BigDecimal BOLETO_TAXA = new BigDecimal("3.49");
    private static final BigDecimal TAXA_MENSAL_CARTAO = new BigDecimal("0.0199");
    private static final BigDecimal BOLETO_LIMITE = new BigDecimal("1000.00");
    private static final BigDecimal MOTOBOY_PESO_LIMITE = new BigDecimal("5.00");

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarPedido(request);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.getItens());
        BigDecimal descontoCupom = calcularDescontoCupom(request.getCupom(), request.getItens(), subtotalProdutos);
        BigDecimal totalAposCupom = subtotalProdutos.subtract(descontoCupom);

        BigDecimal frete = calcularFrete(request.getModalidadeEntrega(), request.getItens());

        BigDecimal freteComCupom = frete;
        if (request.getCupom() != null && !request.getCupom().isEmpty()) {
            String cupom = request.getCupom().trim().toUpperCase();
            if (cupom.equals("FRETEGRATIS")) {
                freteComCupom = BigDecimal.ZERO;
            }
        }

        int prazoEntrega = obterPrazoEntrega(request.getModalidadeEntrega());

        BigDecimal totalAntesPagamento = totalAposCupom.add(freteComCupom);

        validarFormaPagamento(request.getFormaPagamento(), totalAntesPagamento);

        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        BigDecimal[] pagamento = calcularAjustePagamento(request.getFormaPagamento(), totalAntesPagamento, frete, parcelas, request.getCupom());
        BigDecimal ajustePagamento = pagamento[0];
        BigDecimal valorParcela = pagamento[1];

        BigDecimal totalFinal = totalAntesPagamento.add(ajustePagamento);

        return new CheckoutResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            prazoEntrega,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela
        );
    }

    private void validarPedido(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemRequest item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        if (request.getModalidadeEntrega() == null || request.getModalidadeEntrega().trim().isEmpty()) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        String modalidade = request.getModalidadeEntrega().trim().toUpperCase();
        if (!isModalidadeValida(modalidade)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        BigDecimal pesoTotal = calcularPesoTotal(request.getItens());
        if (modalidade.equals("MOTOBOY") && pesoTotal.compareTo(MOTOBOY_PESO_LIMITE) > 0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        if (request.getCupom() != null && !request.getCupom().isEmpty()) {
            String cupom = request.getCupom().trim().toUpperCase();
            if (!isCupomValido(cupom)) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
            validarCupomAplicavel(cupom, calcularSubtotalProdutos(request.getItens()));
        }

        if (request.getFormaPagamento() == null || request.getFormaPagamento().trim().isEmpty()) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        String formaPagamento = request.getFormaPagamento().trim().toUpperCase();
        if (!isFormaPagamentoValida(formaPagamento)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        validarParcelamento(formaPagamento, parcelas);
    }

    private boolean isModalidadeValida(String modalidade) {
        return modalidade.equals("ECONOMICA") || modalidade.equals("EXPRESSA") ||
               modalidade.equals("RETIRADA_LOJA") || modalidade.equals("MOTOBOY");
    }

    private boolean isCupomValido(String cupom) {
        return cupom.equals("BEMVINDO10") || cupom.equals("MENOS50") ||
               cupom.equals("FRETEGRATIS") || cupom.equals("LEVE3PAGUE2");
    }

    private void validarCupomAplicavel(String cupom, BigDecimal subtotalProdutos) {
        if (cupom.equals("MENOS50")) {
            if (subtotalProdutos.compareTo(new BigDecimal("300.00")) < 0) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }
    }

    private boolean isFormaPagamentoValida(String formaPagamento) {
        return formaPagamento.equals("PIX") || formaPagamento.equals("CARTAO") ||
               formaPagamento.equals("BOLETO");
    }

    private void validarParcelamento(String formaPagamento, int parcelas) {
        if (formaPagamento.equals("PIX") || formaPagamento.equals("BOLETO")) {
            if (parcelas != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        } else if (formaPagamento.equals("CARTAO")) {
            if (parcelas < 1 || parcelas > 12) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (ItemRequest item : itens) {
            BigDecimal precoPorItem = new BigDecimal(item.getPrecoUnitario())
                .multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(precoPorItem);
        }

        return arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(String cupom, List<ItemRequest> itens, BigDecimal subtotalProdutos) {
        if (cupom == null || cupom.isEmpty()) {
            return BigDecimal.ZERO;
        }

        cupom = cupom.trim().toUpperCase();

        if (cupom.equals("BEMVINDO10")) {
            BigDecimal desconto = subtotalProdutos.multiply(new BigDecimal("0.10"));
            return arredondar(desconto);
        } else if (cupom.equals("MENOS50")) {
            return arredondar(new BigDecimal("50.00"));
        } else if (cupom.equals("LEVE3PAGUE2")) {
            return calcularDescontoLeve3Pague2(itens);
        } else if (cupom.equals("FRETEGRATIS")) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemRequest> itens) {
        BigDecimal descontoTotal = BigDecimal.ZERO;

        for (ItemRequest item : itens) {
            int quantidade = item.getQuantidade();
            int unidadesGratis = quantidade / 3;
            BigDecimal desconto = new BigDecimal(unidadesGratis)
                .multiply(new BigDecimal(item.getPrecoUnitario()));
            descontoTotal = descontoTotal.add(desconto);
        }

        return arredondar(descontoTotal);
    }

    private BigDecimal calcularFrete(String modalidade, List<ItemRequest> itens) {
        modalidade = modalidade.trim().toUpperCase();
        BigDecimal pesoTotal = calcularPesoTotal(itens);

        if (modalidade.equals("RETIRADA_LOJA")) {
            return BigDecimal.ZERO;
        } else if (modalidade.equals("ECONOMICA")) {
            BigDecimal frete = new BigDecimal("12.00").add(pesoTotal.multiply(new BigDecimal("2.00")));
            return arredondar(frete);
        } else if (modalidade.equals("EXPRESSA")) {
            BigDecimal frete = new BigDecimal("25.00").add(pesoTotal.multiply(new BigDecimal("4.50")));
            return arredondar(frete);
        } else if (modalidade.equals("MOTOBOY")) {
            return new BigDecimal("18.00");
        }

        return BigDecimal.ZERO;
    }

    private int obterPrazoEntrega(String modalidade) {
        modalidade = modalidade.trim().toUpperCase();

        return switch (modalidade) {
            case "RETIRADA_LOJA" -> 1;
            case "MOTOBOY" -> 0;
            case "EXPRESSA" -> 2;
            case "ECONOMICA" -> 7;
            default -> 0;
        };
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            BigDecimal peso = new BigDecimal(item.getPesoKg()).multiply(new BigDecimal(item.getQuantidade()));
            pesoTotal = pesoTotal.add(peso);
        }
        return pesoTotal;
    }

    private void validarFormaPagamento(String formaPagamento, BigDecimal totalAntesPagamento) {
        formaPagamento = formaPagamento.trim().toUpperCase();

        if (formaPagamento.equals("BOLETO")) {
            if (totalAntesPagamento.compareTo(BOLETO_LIMITE) > 0) {
                throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
            }
        }
    }

    private BigDecimal[] calcularAjustePagamento(String formaPagamento, BigDecimal total, BigDecimal frete, int parcelas, String cupom) {
        formaPagamento = formaPagamento.trim().toUpperCase();

        if (formaPagamento.equals("PIX")) {
            BigDecimal desconto = total.multiply(PIX_DESCONTO);
            desconto = arredondar(desconto);
            BigDecimal valorParcela = arredondar(total.subtract(desconto));
            return new BigDecimal[]{desconto.negate(), valorParcela};
        } else if (formaPagamento.equals("BOLETO")) {
            BigDecimal taxa = BOLETO_TAXA;
            BigDecimal valorParcela = arredondar(total.add(taxa));
            return new BigDecimal[]{taxa, valorParcela};
        } else if (formaPagamento.equals("CARTAO")) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = arredondar(total.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN));
                return new BigDecimal[]{BigDecimal.ZERO, valorParcela};
            } else {
                BigDecimal taxa = TAXA_MENSAL_CARTAO;
                BigDecimal numerador = total.multiply(taxa);

                BigDecimal base = taxa.add(BigDecimal.ONE);
                BigDecimal potencia = base;
                for (int i = 1; i < parcelas; i++) {
                    potencia = potencia.multiply(base);
                }

                BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(potencia, 10, RoundingMode.HALF_EVEN));

                BigDecimal valorParcela = arredondar(numerador.divide(denominador, 10, RoundingMode.HALF_EVEN));

                BigDecimal totalComParcelas = valorParcela.multiply(new BigDecimal(parcelas));
                BigDecimal ajuste = arredondar(totalComParcelas.subtract(total));
                return new BigDecimal[]{ajuste, valorParcela};
            }
        }

        return new BigDecimal[]{BigDecimal.ZERO, total};
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
