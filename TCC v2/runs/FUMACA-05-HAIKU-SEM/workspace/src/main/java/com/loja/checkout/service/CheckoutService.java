package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class CheckoutService {

    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    private static final int DECIMAL_PLACES = 2;

    private static final Map<String, Integer> PRAZO_ENTREGA = Map.of(
        "ECONOMICA", 7,
        "EXPRESSA", 2,
        "RETIRADA_LOJA", 1,
        "MOTOBOY", 0
    );

    private static final Set<String> MODALIDADES_VALIDAS = Set.of(
        "ECONOMICA", "EXPRESSA", "RETIRADA_LOJA", "MOTOBOY"
    );

    private static final Set<String> CUPONS_VALIDOS = Set.of(
        "BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2"
    );

    private static final Set<String> FORMAS_PAGAMENTO = Set.of(
        "PIX", "CARTAO", "BOLETO"
    );

    public CheckoutResponse calcularResumo(CheckoutRequest request) throws CheckoutException {
        // Validação 1: Pedido inválido
        validarPedido(request);

        // Validação 2: Modalidade de entrega válida
        String modalidade = request.getModalidadeEntrega();
        if (modalidade == null || !MODALIDADES_VALIDAS.contains(modalidade)) {
            throw new CheckoutException("MODALIDADE_INVALIDA", "Modalidade de entrega inválida");
        }

        // Validações 3-5: Cupom
        String cupom = request.getCupom();

        // Validação 3: Modalidade disponível
        validarDisponibilidadeModalidade(request.getItens(), modalidade);

        // Validação 4: Cupom válido
        if (cupom != null && !CUPONS_VALIDOS.contains(cupom)) {
            throw new CheckoutException("CUPOM_INVALIDO", "Cupom informado não existe");
        }

        // Cálculos
        BigDecimal subtotal = calcularSubtotalProdutos(request.getItens());

        double pesoTotal = calcularPesoTotal(request.getItens());
        BigDecimal frete = calcularFrete(modalidade, pesoTotal);

        // Validação 5: Cupom aplicável
        BigDecimal desconto = BigDecimal.ZERO;
        if (cupom != null) {
            desconto = calcularDesconto(cupom, request.getItens(), subtotal, frete);
        }

        BigDecimal totalAntesPagamento = subtotal.subtract(desconto).add(frete);

        // Validação 6: Forma de pagamento válida
        String formaPagamento = request.getFormaPagamento();
        if (formaPagamento == null || !FORMAS_PAGAMENTO.contains(formaPagamento)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA", "Forma de pagamento inválida");
        }

        // Validação 7: Parcelamento válido
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        validarParcelamento(formaPagamento, parcelas);

        // Validação 8: Forma de pagamento disponível
        validarDisponibilidadePagamento(formaPagamento, totalAntesPagamento);

        // Cálculo do ajuste de pagamento
        BigDecimal ajuste = calcularAjustePagamento(formaPagamento, totalAntesPagamento, parcelas);
        BigDecimal totalFinal = totalAntesPagamento.add(ajuste);

        BigDecimal valorParcela = calcularValorParcela(formaPagamento, totalFinal, parcelas);

        Integer prazo = PRAZO_ENTREGA.get(modalidade);

        return new CheckoutResponse(
            arredondar(subtotal),
            arredondar(desconto),
            arredondar(frete),
            prazo,
            arredondar(ajuste),
            arredondar(totalFinal),
            parcelas,
            arredondar(valorParcela)
        );
    }

    private void validarPedido(CheckoutRequest request) throws CheckoutException {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO", "Carrinho vazio");
        }

        for (ItemPedido item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO", "Item com preço, quantidade ou peso inválido");
            }
        }
    }

    private void validarDisponibilidadeModalidade(List<ItemPedido> itens, String modalidade) throws CheckoutException {
        if ("MOTOBOY".equals(modalidade)) {
            double pesoTotal = calcularPesoTotal(itens);
            if (pesoTotal > 5) {
                throw new CheckoutException("MODALIDADE_INDISPONIVEL", "Motoboy não atende pedidos acima de 5 kg");
            }
        }
    }

    private void validarParcelamento(String formaPagamento, int parcelas) throws CheckoutException {
        if ("PIX".equals(formaPagamento) || "BOLETO".equals(formaPagamento)) {
            if (parcelas != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO", "Pix e boleto apenas à vista");
            }
        } else if ("CARTAO".equals(formaPagamento)) {
            if (parcelas < 1 || parcelas > 12) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO", "Cartão permite de 1 a 12 parcelas");
            }
        }
    }

    private void validarDisponibilidadePagamento(String formaPagamento, BigDecimal total) throws CheckoutException {
        if ("BOLETO".equals(formaPagamento)) {
            if (total.compareTo(new BigDecimal("1000.00")) > 0) {
                throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL", "Boleto não disponível acima de R$ 1.000,00");
            }
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal precoUnitario = new BigDecimal(String.valueOf(item.getPrecoUnitario()));
            BigDecimal quantidade = new BigDecimal(item.getQuantidade());
            BigDecimal precoItem = precoUnitario.multiply(quantidade);
            subtotal = subtotal.add(precoItem);
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularDesconto(String cupom, List<ItemPedido> itens, BigDecimal subtotal, BigDecimal frete) throws CheckoutException {
        switch (cupom) {
            case "BEMVINDO10":
                return arredondar(subtotal.multiply(new BigDecimal("0.10")));

            case "MENOS50":
                if (subtotal.compareTo(new BigDecimal("300.00")) < 0) {
                    throw new CheckoutException("CUPOM_NAO_APLICAVEL", "Cupom MENOS50 exige mínimo de R$ 300,00");
                }
                return new BigDecimal("50.00");

            case "FRETEGRATIS":
                return frete;

            case "LEVE3PAGUE2":
                BigDecimal desconto = BigDecimal.ZERO;
                for (ItemPedido item : itens) {
                    int quantidade = item.getQuantidade();
                    int unidadesGratis = quantidade / 3;
                    if (unidadesGratis > 0) {
                        BigDecimal preco = new BigDecimal(String.valueOf(item.getPrecoUnitario()));
                        desconto = desconto.add(preco.multiply(new BigDecimal(unidadesGratis)));
                    }
                }
                return arredondar(desconto);

            default:
                return BigDecimal.ZERO;
        }
    }

    private double calcularPesoTotal(List<ItemPedido> itens) {
        double pesoTotal = 0;
        for (ItemPedido item : itens) {
            pesoTotal += item.getPesoKg() * item.getQuantidade();
        }
        return pesoTotal;
    }

    private BigDecimal calcularFrete(String modalidade, double pesoTotal) throws CheckoutException {
        switch (modalidade) {
            case "ECONOMICA":
                BigDecimal frete = new BigDecimal("12.00").add(
                    new BigDecimal("2.00").multiply(new BigDecimal(pesoTotal))
                );
                return arredondar(frete);

            case "EXPRESSA":
                frete = new BigDecimal("25.00").add(
                    new BigDecimal("4.50").multiply(new BigDecimal(pesoTotal))
                );
                return arredondar(frete);

            case "RETIRADA_LOJA":
                return BigDecimal.ZERO;

            case "MOTOBOY":
                return new BigDecimal("18.00");

            default:
                throw new CheckoutException("MODALIDADE_INVALIDA", "Modalidade de entrega inválida");
        }
    }

    private BigDecimal calcularAjustePagamento(String formaPagamento, BigDecimal total, int parcelas) throws CheckoutException {
        switch (formaPagamento) {
            case "PIX":
                return arredondar(total.multiply(new BigDecimal("0.05")).negate());

            case "BOLETO":
                return new BigDecimal("3.49");

            case "CARTAO":
                if (parcelas <= 3) {
                    return BigDecimal.ZERO;
                } else {
                    return calcularJurosCartao(total, parcelas);
                }

            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcularJurosCartao(BigDecimal total, int parcelas) {
        BigDecimal taxa = new BigDecimal("0.0199");
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);

        BigDecimal umMaisTaxaPowN = umMaisTaxa.pow(parcelas, java.math.MathContext.DECIMAL128);
        BigDecimal numerador = taxa.multiply(umMaisTaxaPowN);
        BigDecimal denominador = umMaisTaxaPowN.subtract(BigDecimal.ONE);

        BigDecimal parcela = total.multiply(numerador).divide(denominador, DECIMAL_PLACES, ROUNDING_MODE);
        BigDecimal totalComJuros = parcela.multiply(new BigDecimal(parcelas));

        return arredondar(totalComJuros.subtract(total));
    }

    private BigDecimal calcularValorParcela(String formaPagamento, BigDecimal totalFinal, int parcelas) {
        if ("CARTAO".equals(formaPagamento) && parcelas > 3) {
            return arredondar(totalFinal.divide(new BigDecimal(parcelas), DECIMAL_PLACES, ROUNDING_MODE));
        } else {
            return arredondar(totalFinal.divide(new BigDecimal(parcelas), DECIMAL_PLACES, ROUNDING_MODE));
        }
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(DECIMAL_PLACES, ROUNDING_MODE);
    }
}
