package com.loja.checkout.service;

import com.loja.checkout.api.CartItem;
import com.loja.checkout.api.CheckoutRequest;
import com.loja.checkout.api.OrderSummary;
import com.loja.checkout.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    public OrderSummary calcularResumo(CheckoutRequest request) {
        // Validar dados de entrada
        String erroValidacao = validarPedido(request);
        if (erroValidacao != null) {
            return OrderSummary.erro(erroValidacao);
        }

        try {
            List<CartItem> itens = request.getItens();
            NivelClube nivelClube = NivelClube.fromCodigo(request.getNivelClube());
            Regiao regiao = Regiao.fromCodigo(request.getRegiao());
            ModalidadeEntrega modalidade = ModalidadeEntrega.fromCodigo(request.getModalidadeEntrega());
            Cupom cupom = Cupom.fromCodigo(request.getCupom());
            FormaPagamento formaPagamento = FormaPagamento.fromCodigo(request.getFormaPagamento());
            int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

            // 1. Calcula subtotal dos produtos
            BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);

            // 3. Calcula frete (antes do desconto para FRETEGRATIS)
            BigDecimal frete = calcularFrete(modalidade, itens, nivelClube);
            int prazoEntregaDias = modalidade.getPrazoDias();

            // 2. Calcula desconto do cupom (agora conhece o frete)
            BigDecimal descontoCupom = calcularDescontoCupom(cupom, subtotalProdutos, itens, frete);

            // 4. Calcula seguro
            BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);

            // 5. Calcula total (antes do ajuste de pagamento)
            BigDecimal total = subtotalProdutos
                    .subtract(descontoCupom)
                    .add(frete)
                    .add(seguro);

            // 6. Calcula ajuste de pagamento e valor final
            BigDecimal[] resultadoPagamento = calcularAjustePagamento(formaPagamento, total, parcelas);
            BigDecimal ajustePagamento = resultadoPagamento[0];
            BigDecimal totalFinal = resultadoPagamento[1];
            BigDecimal valorParcela = resultadoPagamento[2];

            // 7. Calcula crédito para a próxima compra
            BigDecimal creditoProximaCompra = calcularCredito(nivelClube, subtotalProdutos);

            // 8. Verifica se deve enviar brinde
            boolean brinde = nivelClube == NivelClube.OURO && subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;

            // Monta resposta
            OrderSummary resposta = new OrderSummary();
            resposta.setSubtotalProdutos(subtotalProdutos.doubleValue());
            resposta.setDescontoCupom(descontoCupom.doubleValue());
            resposta.setFrete(frete.doubleValue());
            resposta.setPrazoEntregaDias(prazoEntregaDias);
            resposta.setSeguro(seguro.doubleValue());
            resposta.setAjustePagamento(ajustePagamento.doubleValue());
            resposta.setTotalFinal(totalFinal.doubleValue());
            resposta.setParcelas(parcelas);
            resposta.setValorParcela(valorParcela.doubleValue());
            resposta.setCreditoProximaCompra(creditoProximaCompra.doubleValue());
            resposta.setBrinde(brinde);

            return resposta;
        } catch (Exception e) {
            return OrderSummary.erro("PEDIDO_INVALIDO");
        }
    }

    private String validarPedido(CheckoutRequest request) {
        // 1. Validar carrinho
        if (request.getItens() == null || request.getItens().isEmpty()) {
            return "PEDIDO_INVALIDO";
        }

        for (CartItem item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() < 0) {
                return "PEDIDO_INVALIDO";
            }
        }

        // 2. Validar nível do clube
        NivelClube nivelClube = NivelClube.fromCodigo(request.getNivelClube());
        if (nivelClube == null) {
            return "NIVEL_CLUBE_INVALIDO";
        }

        // 3. Validar região
        Regiao regiao = Regiao.fromCodigo(request.getRegiao());
        if (regiao == null) {
            return "REGIAO_INVALIDA";
        }

        // 4. Validar modalidade de entrega
        ModalidadeEntrega modalidade = ModalidadeEntrega.fromCodigo(request.getModalidadeEntrega());
        if (modalidade == null) {
            return "MODALIDADE_INVALIDA";
        }

        // 5. Validar se modalidade é disponível para o pedido
        double pesoTotal = request.getItens().stream()
                .mapToDouble(i -> i.getPesoKg() * i.getQuantidade())
                .sum();

        if (modalidade.temLimiteKg() && pesoTotal > modalidade.getLimiteKg()) {
            return "MODALIDADE_INDISPONIVEL";
        }

        // 6. Validar cupom
        if (request.getCupom() != null && !request.getCupom().isEmpty()) {
            Cupom cupom = Cupom.fromCodigo(request.getCupom());
            if (cupom == null) {
                return "CUPOM_INVALIDO";
            }

            // 7. Validar se cupom é aplicável
            if (!isCupomAplicavel(cupom, request.getItens())) {
                return "CUPOM_NAO_APLICAVEL";
            }
        }

        // 8. Validar forma de pagamento
        FormaPagamento formaPagamento = FormaPagamento.fromCodigo(request.getFormaPagamento());
        if (formaPagamento == null) {
            return "FORMA_PAGAMENTO_INVALIDA";
        }

        // 9. Validar parcelas
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        if (!isParcelamentoValido(formaPagamento, parcelas)) {
            return "PARCELAMENTO_INVALIDO";
        }

        // 10. Validar se forma de pagamento é disponível para o pedido
        if (!isFormaPagamentoDisponivel(formaPagamento, request.getItens())) {
            return "FORMA_PAGAMENTO_INDISPONIVEL";
        }

        return null;
    }

    private boolean isCupomAplicavel(Cupom cupom, List<CartItem> itens) {
        if (cupom == Cupom.MENOS50) {
            BigDecimal subtotal = calcularSubtotalProdutos(itens);
            return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
        }
        return true;
    }

    private boolean isParcelamentoValido(FormaPagamento formaPagamento, int parcelas) {
        if (formaPagamento == FormaPagamento.PIX || formaPagamento == FormaPagamento.BOLETO) {
            return parcelas == 1;
        }
        if (formaPagamento == FormaPagamento.CARTAO) {
            return parcelas >= 1 && parcelas <= 12;
        }
        return false;
    }

    private boolean isFormaPagamentoDisponivel(FormaPagamento formaPagamento, List<CartItem> itens) {
        if (formaPagamento == FormaPagamento.BOLETO) {
            BigDecimal subtotal = calcularSubtotalProdutos(itens);
            return subtotal.compareTo(new BigDecimal("1000.00")) <= 0;
        }
        return true;
    }

    private BigDecimal calcularSubtotalProdutos(List<CartItem> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : itens) {
            BigDecimal preco = new BigDecimal(item.getPrecoUnitario().toString());
            BigDecimal quantidade = new BigDecimal(item.getQuantidade());
            subtotal = subtotal.add(preco.multiply(quantidade));
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(Cupom cupom, BigDecimal subtotalProdutos, List<CartItem> itens, BigDecimal frete) {
        if (cupom == null) {
            return BigDecimal.ZERO;
        }

        switch (cupom) {
            case BEMVINDO10 -> {
                return arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
            }
            case MENOS50 -> {
                return arredondar(new BigDecimal("50.00"));
            }
            case FRETEGRATIS -> {
                return frete;
            }
            case LEVE3PAGUE2 -> {
                return calcularDescontoLeve3Pague2(itens);
            }
            default -> {
                return BigDecimal.ZERO;
            }
        }
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<CartItem> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (CartItem item : itens) {
            int quantidade = item.getQuantidade();
            int unidadesGratis = quantidade / 3;
            BigDecimal preco = new BigDecimal(item.getPrecoUnitario().toString());
            BigDecimal descontoItem = preco.multiply(new BigDecimal(unidadesGratis));
            desconto = desconto.add(descontoItem);
        }
        return arredondar(desconto);
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, List<CartItem> itens, NivelClube nivelClube) {
        if (nivelClube == NivelClube.OURO) {
            return BigDecimal.ZERO;
        }

        double pesoTotal = itens.stream()
                .mapToDouble(i -> i.getPesoKg() * i.getQuantidade())
                .sum();

        BigDecimal custoFixo = new BigDecimal(modalidade.getCustoFixo());
        BigDecimal custoPorKg = new BigDecimal(modalidade.getCustoPorKg()).multiply(new BigDecimal(pesoTotal));

        return arredondar(custoFixo.add(custoPorKg));
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        BigDecimal percentual = new BigDecimal(regiao.getPercentualSeguro());
        return arredondar(subtotalProdutos.multiply(percentual));
    }

    private BigDecimal[] calcularAjustePagamento(FormaPagamento formaPagamento, BigDecimal total, int parcelas) {
        BigDecimal ajuste = BigDecimal.ZERO;
        BigDecimal totalFinal = total;
        BigDecimal valorParcela = total;

        switch (formaPagamento) {
            case PIX -> {
                ajuste = total.multiply(new BigDecimal("0.05")).negate();
                totalFinal = arredondar(total.add(ajuste));
                valorParcela = totalFinal;
            }
            case BOLETO -> {
                ajuste = new BigDecimal("3.49");
                totalFinal = arredondar(total.add(ajuste));
                valorParcela = totalFinal;
            }
            case CARTAO -> {
                if (parcelas <= 3) {
                    totalFinal = total;
                    ajuste = BigDecimal.ZERO;
                    valorParcela = arredondar(total.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN));
                } else {
                    BigDecimal taxa = new BigDecimal("0.0199");
                    BigDecimal fator = BigDecimal.ONE.add(taxa).pow(parcelas);
                    BigDecimal denominador = fator.subtract(BigDecimal.ONE).divide(taxa.multiply(fator), 10, RoundingMode.HALF_EVEN);
                    valorParcela = arredondar(total.divide(denominador, 2, RoundingMode.HALF_EVEN));
                    totalFinal = arredondar(valorParcela.multiply(new BigDecimal(parcelas)));
                    ajuste = totalFinal.subtract(total);
                }
            }
        }

        return new BigDecimal[]{ajuste, totalFinal, valorParcela};
    }

    private BigDecimal calcularCredito(NivelClube nivelClube, BigDecimal subtotalProdutos) {
        if (nivelClube.getPercentualCredito() == 0) {
            return BigDecimal.ZERO;
        }
        return arredondar(subtotalProdutos.multiply(new BigDecimal(nivelClube.getPercentualCredito())));
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
