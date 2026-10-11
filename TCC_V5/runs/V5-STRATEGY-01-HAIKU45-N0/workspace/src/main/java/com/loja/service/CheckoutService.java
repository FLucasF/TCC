package com.loja.service;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ItemCarrinho;
import com.loja.model.FormaPagamento;
import com.loja.model.ModalidadeEntrega;
import com.loja.model.NivelClube;
import com.loja.model.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    private static final BigDecimal PIX_DESCONTO = new BigDecimal("0.05");
    private static final BigDecimal BOLETO_TARIFA = new BigDecimal("3.49");
    private static final BigDecimal CARTAO_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final int PESO_MAX_MOTOBOY = 5;
    private static final BigDecimal VALOR_MAX_BOLETO = new BigDecimal("1000.00");
    private static final BigDecimal VALOR_MIN_MENOS50 = new BigDecimal("300.00");
    private static final BigDecimal VALOR_BRINDE = new BigDecimal("500.00");

    public CheckoutResponse calcular(CheckoutRequest request) {
        validarCarrinho(request);
        validarNivelClube(request);
        validarRegiao(request);
        validarModalidadeEntrega(request);
        validarCupom(request);
        validarAplicabilidadeCupom(request);
        validarFormaPagamento(request);
        validarParcelamento(request.getFormaPagamento(), request.getParcelas());

        return procesarCheckout(request);
    }

    private void validarCarrinho(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarNivelClube(CheckoutRequest request) {
        if (request.getNivelClube() == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        if (NivelClube.fromCodigo(request.getNivelClube()) == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private void validarRegiao(CheckoutRequest request) {
        if (request.getRegiao() == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        if (Regiao.fromCodigo(request.getRegiao()) == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private void validarModalidadeEntrega(CheckoutRequest request) {
        if (request.getModalidadeEntrega() == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        if (ModalidadeEntrega.fromCodigo(request.getModalidadeEntrega()) == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }

    private void validarCupom(CheckoutRequest request) {
        String cupom = request.getCupom();
        if (cupom != null && !isValidCupom(cupom)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
    }

    private void validarFormaPagamento(CheckoutRequest request) {
        if (request.getFormaPagamento() == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        if (FormaPagamento.fromCodigo(request.getFormaPagamento()) == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private CheckoutResponse procesarCheckout(CheckoutRequest request) {
        CheckoutResponse response = new CheckoutResponse();

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.getItens());
        BigDecimal descontoCupom = calcularDescontoCupom(request.getItens(), subtotalProdutos, request.getCupom());
        BigDecimal pesoTotal = calcularPesoTotal(request.getItens());

        validarDisponibilidadeModalidade(request.getModalidadeEntrega(), pesoTotal);

        ModalidadeEntrega modalidade = ModalidadeEntrega.fromCodigo(request.getModalidadeEntrega());
        NivelClube nivel = NivelClube.fromCodigo(request.getNivelClube());
        Regiao regiao = Regiao.fromCodigo(request.getRegiao());
        FormaPagamento forma = FormaPagamento.fromCodigo(request.getFormaPagamento());

        BigDecimal frete = calcularFrete(modalidade, pesoTotal, nivel, request.getCupom());
        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);

        BigDecimal totalAntesDePagamento = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);

        validarDisponibilidadeFormaPagamento(forma, totalAntesDePagamento);

        int parcelasNum = request.getParcelas() != null && request.getParcelas() > 0 ? request.getParcelas() : 1;

        BigDecimal[] resultado = calcularAjusteEParcela(forma, totalAntesDePagamento, parcelasNum);
        BigDecimal ajustePagamento = resultado[0];
        BigDecimal valorParcela = resultado[1];

        BigDecimal totalFinal = totalAntesDePagamento.add(ajustePagamento);

        BigDecimal credito = calcularCredito(subtotalProdutos, nivel);
        boolean brinde = isBrinde(subtotalProdutos, nivel);

        response.setSubtotalProdutos(subtotalProdutos.doubleValue());
        response.setDescontoCupom(descontoCupom.doubleValue());
        response.setFrete(frete.doubleValue());
        response.setPrazoEntregaDias(modalidade.getPrazoEntregaDias());
        response.setSeguro(seguro.doubleValue());
        response.setAjustePagamento(ajustePagamento.doubleValue());
        response.setTotalFinal(totalFinal.doubleValue());
        response.setParcelas(parcelasNum);
        response.setValorParcela(valorParcela.doubleValue());
        response.setCreditoProximaCompra(credito.doubleValue());
        response.setBrinde(brinde);

        return response;
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal preco = new BigDecimal(String.valueOf(item.getPrecoUnitario()));
            BigDecimal quantidade = new BigDecimal(item.getQuantidade());
            subtotal = subtotal.add(preco.multiply(quantidade));
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(List<ItemCarrinho> itens, BigDecimal subtotal, String cupom) {
        if (cupom == null || cupom.isEmpty()) {
            return BigDecimal.ZERO;
        }

        switch (cupom) {
            case "BEMVINDO10":
                return arredondar(subtotal.multiply(new BigDecimal("0.10")));

            case "MENOS50":
                return arredondar(new BigDecimal("50.00"));

            case "FRETEGRATIS":
                return BigDecimal.ZERO;

            case "LEVE3PAGUE2":
                return calcularDescontoLeve3Pague2(itens);

            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemCarrinho> itens) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (ItemCarrinho item : itens) {
            int quantidade = item.getQuantidade();
            int gruposLeve3 = quantidade / 3;

            if (gruposLeve3 > 0) {
                BigDecimal precoUnitario = new BigDecimal(String.valueOf(item.getPrecoUnitario()));
                BigDecimal descontoItem = precoUnitario.multiply(new BigDecimal(gruposLeve3));
                desconto = desconto.add(descontoItem);
            }
        }

        return arredondar(desconto);
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal peso = new BigDecimal(String.valueOf(item.getPesoKg()));
            BigDecimal quantidade = new BigDecimal(item.getQuantidade());
            pesoTotal = pesoTotal.add(peso.multiply(quantidade));
        }
        return pesoTotal;
    }

    private void validarDisponibilidadeModalidade(String modalidade, BigDecimal peso) {
        if (modalidade.equals("MOTOBOY") && peso.compareTo(new BigDecimal(PESO_MAX_MOTOBOY)) > 0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private void validarDisponibilidadeFormaPagamento(FormaPagamento forma, BigDecimal totalAntesDePagamento) {
        if (forma == FormaPagamento.BOLETO) {
            if (totalAntesDePagamento.compareTo(VALOR_MAX_BOLETO) > 0) {
                throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
            }
        }
    }

    private void validarParcelamento(String forma, Integer parcelas) {
        int parcelasNum = parcelas != null && parcelas > 0 ? parcelas : 1;

        if (forma.equals("PIX") || forma.equals("BOLETO")) {
            if (parcelasNum != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        } else if (forma.equals("CARTAO")) {
            if (parcelasNum < 1 || parcelasNum > 12) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }
    }

    private void validarAplicabilidadeCupom(CheckoutRequest request) {
        String cupom = request.getCupom();
        if (cupom == null || cupom.isEmpty()) {
            return;
        }

        BigDecimal subtotal = calcularSubtotalProdutos(request.getItens());

        if (cupom.equals("MENOS50")) {
            if (subtotal.compareTo(VALOR_MIN_MENOS50) < 0) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        } else if (cupom.equals("LEVE3PAGUE2")) {
            boolean temItem = false;
            for (ItemCarrinho item : request.getItens()) {
                if (item.getQuantidade() >= 3) {
                    temItem = true;
                    break;
                }
            }
            if (!temItem) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, BigDecimal peso, NivelClube nivel, String cupom) {
        if (nivel.isFretGratis()) {
            return BigDecimal.ZERO;
        }

        if (modalidade == ModalidadeEntrega.RETIRADA_LOJA) {
            return BigDecimal.ZERO;
        }

        if ("FRETEGRATIS".equals(cupom)) {
            return BigDecimal.ZERO;
        }

        BigDecimal frete = new BigDecimal(String.valueOf(modalidade.getBaseFixa()))
            .add(new BigDecimal(String.valueOf(modalidade.getPorKg())).multiply(peso));

        return arredondar(frete);
    }

    private BigDecimal calcularSeguro(BigDecimal subtotal, Regiao regiao) {
        BigDecimal percentual = new BigDecimal(String.valueOf(regiao.getSeguroPercentual()));
        BigDecimal seguro = subtotal.multiply(percentual);
        return arredondar(seguro);
    }

    private BigDecimal[] calcularAjusteEParcela(FormaPagamento forma, BigDecimal totalAntes, int parcelas) {
        if (forma == FormaPagamento.PIX) {
            BigDecimal desconto = arredondar(totalAntes.multiply(PIX_DESCONTO));
            BigDecimal ajuste = desconto.negate();
            BigDecimal totalFinal = totalAntes.add(ajuste);
            BigDecimal parcela = arredondar(totalFinal.divide(new BigDecimal(parcelas), 20, RoundingMode.HALF_EVEN));
            return new BigDecimal[]{ajuste, parcela};
        }

        if (forma == FormaPagamento.BOLETO) {
            BigDecimal ajuste = arredondar(BOLETO_TARIFA);
            BigDecimal totalFinal = totalAntes.add(ajuste);
            BigDecimal parcela = arredondar(totalFinal.divide(new BigDecimal(parcelas), 20, RoundingMode.HALF_EVEN));
            return new BigDecimal[]{ajuste, parcela};
        }

        if (forma == FormaPagamento.CARTAO) {
            if (parcelas <= 3) {
                BigDecimal parcela = arredondar(totalAntes.divide(new BigDecimal(parcelas), 20, RoundingMode.HALF_EVEN));
                return new BigDecimal[]{BigDecimal.ZERO, parcela};
            }

            BigDecimal taxa = CARTAO_JUROS_MENSAL;
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
            BigDecimal taisoExpoente = umMaisTaxa.pow(parcelas);
            BigDecimal denominador = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(taisoExpoente, 20, RoundingMode.HALF_EVEN)
            );

            BigDecimal parcelaExata = totalAntes.multiply(taxa).divide(denominador, 20, RoundingMode.HALF_EVEN);
            BigDecimal parcelaArredondada = arredondar(parcelaExata);
            BigDecimal totalFinal = parcelaArredondada.multiply(new BigDecimal(parcelas));
            BigDecimal ajuste = totalFinal.subtract(totalAntes);

            return new BigDecimal[]{ajuste, parcelaArredondada};
        }

        BigDecimal parcela = arredondar(totalAntes.divide(new BigDecimal(parcelas), 20, RoundingMode.HALF_EVEN));
        return new BigDecimal[]{BigDecimal.ZERO, parcela};
    }

    private BigDecimal calcularCredito(BigDecimal subtotal, NivelClube nivel) {
        BigDecimal percentual = new BigDecimal(String.valueOf(nivel.getCreditoPercentual()));
        BigDecimal credito = subtotal.multiply(percentual);
        return arredondar(credito);
    }

    private boolean isBrinde(BigDecimal subtotal, NivelClube nivel) {
        if (nivel != NivelClube.OURO) {
            return false;
        }
        return subtotal.compareTo(VALOR_BRINDE) > 0;
    }

    private boolean isValidCupom(String cupom) {
        return cupom.equals("BEMVINDO10") ||
               cupom.equals("MENOS50") ||
               cupom.equals("FRETEGRATIS") ||
               cupom.equals("LEVE3PAGUE2");
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static class CheckoutException extends RuntimeException {
        private final String codigo;

        public CheckoutException(String codigo) {
            super("Erro no checkout: " + codigo);
            this.codigo = codigo;
        }

        public String getCodigo() {
            return codigo;
        }
    }
}
