package com.loja.checkout.service;

import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.exception.ResumoCheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    private static final BigDecimal PIX_DESCONTO = new BigDecimal("0.05");
    private static final BigDecimal BOLETO_TAXA = new BigDecimal("3.49");
    private static final BigDecimal JUROS_CARTAO = new BigDecimal("0.0199");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");

    public ResumoResponse calcularResumo(PedidoRequest request) {
        validarEntrada(request);

        List<ItemPedido> itens = request.getItens();
        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(request.getModalidadeEntrega());
        NivelClube nivelClube = NivelClube.valueOf(request.getNivelClube());
        Regiao regiao = Regiao.valueOf(request.getRegiao());
        FormaPagamento formaPagamento = FormaPagamento.valueOf(request.getFormaPagamento());
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        BigDecimal subtotal = calcularSubtotal(itens);
        BigDecimal descontoCupom = calcularDescontoCupom(request.getCupom(), subtotal, itens);
        BigDecimal pesoTotal = calcularPesoTotal(itens);
        BigDecimal frete = calcularFrete(modalidade, pesoTotal, nivelClube);
        BigDecimal seguro = calcularSeguro(subtotal, regiao);
        BigDecimal baseParaAjuste = subtotal.subtract(descontoCupom).add(frete).add(seguro);
        BigDecimal ajustePagamento = calcularAjustePagamento(formaPagamento, baseParaAjuste, parcelas);
        BigDecimal totalFinal = baseParaAjuste.add(ajustePagamento);
        BigDecimal valorParcela = calcularValorParcela(totalFinal, parcelas);
        BigDecimal credito = calcularCredito(subtotal, nivelClube);
        boolean temBrinde = temBrinde(subtotal, nivelClube);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.getPrazo(),
                seguro,
                ajustePagamento,
                totalFinal,
                parcelas,
                valorParcela,
                credito,
                temBrinde
        );
    }

    private void validarEntrada(PedidoRequest request) {
        validarPedido(request.getItens());
        validarNivelClube(request.getNivelClube());
        validarRegiao(request.getRegiao());
        validarModalidade(request.getModalidadeEntrega(), request.getItens());
        validarCupom(request.getCupom(), request.getItens());
        validarFormaPagamento(request.getFormaPagamento(), request.getItens(),
                             request.getCupom(), request.getRegiao());
        validarParcelamento(request.getFormaPagamento(), request.getParcelas(),
                           request.getItens(), request.getCupom(), request.getRegiao());
    }

    private void validarPedido(List<ItemPedido> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ResumoCheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemPedido item : itens) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResumoCheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarNivelClube(String nivelClube) {
        if (nivelClube == null) {
            throw new ResumoCheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        try {
            NivelClube.valueOf(nivelClube);
        } catch (IllegalArgumentException e) {
            throw new ResumoCheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private void validarRegiao(String regiao) {
        if (regiao == null) {
            throw new ResumoCheckoutException("REGIAO_INVALIDA");
        }
        try {
            Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new ResumoCheckoutException("REGIAO_INVALIDA");
        }
    }

    private void validarModalidade(String modalidade, List<ItemPedido> itens) {
        if (modalidade == null) {
            throw new ResumoCheckoutException("MODALIDADE_INVALIDA");
        }
        ModalidadeEntrega mod;
        try {
            mod = ModalidadeEntrega.valueOf(modalidade);
        } catch (IllegalArgumentException e) {
            throw new ResumoCheckoutException("MODALIDADE_INVALIDA");
        }

        BigDecimal pesoTotal = calcularPesoTotal(itens);
        if (!mod.contemPesoLimite(pesoTotal)) {
            throw new ResumoCheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private void validarCupom(String cupom, List<ItemPedido> itens) {
        if (cupom == null) {
            return;
        }

        if (!cupomExiste(cupom)) {
            throw new ResumoCheckoutException("CUPOM_INVALIDO");
        }

        if (!cupomAplicavel(cupom, itens)) {
            throw new ResumoCheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }

    private void validarFormaPagamento(String formaPagamento, List<ItemPedido> itens,
                                       String cupom, String regiao) {
        if (formaPagamento == null) {
            throw new ResumoCheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            FormaPagamento.valueOf(formaPagamento);
        } catch (IllegalArgumentException e) {
            throw new ResumoCheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        FormaPagamento forma = FormaPagamento.valueOf(formaPagamento);
        BigDecimal subtotal = calcularSubtotal(itens);
        BigDecimal descontoCupom = calcularDescontoCupom(cupom, subtotal, itens);
        BigDecimal pesoTotal = calcularPesoTotal(itens);
        ModalidadeEntrega modalidade = null;
        if (cupom != null && cupom.equals("FRETEGRATIS")) {
            modalidade = ModalidadeEntrega.EXPRESSA;
        } else {
            modalidade = ModalidadeEntrega.ECONOMICA;
        }
        BigDecimal frete = calcularFrete(modalidade, pesoTotal, NivelClube.BRONZE);
        BigDecimal seguro = calcularSeguro(subtotal, Regiao.valueOf(regiao));
        BigDecimal baseParaAjuste = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (forma == FormaPagamento.BOLETO && baseParaAjuste.compareTo(LIMITE_BOLETO) > 0) {
            throw new ResumoCheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private void validarParcelamento(String formaPagamento, Integer parcelas,
                                     List<ItemPedido> itens, String cupom, String regiao) {
        FormaPagamento forma = FormaPagamento.valueOf(formaPagamento);
        int numParcelas = parcelas != null ? parcelas : 1;

        if (forma == FormaPagamento.PIX || forma == FormaPagamento.BOLETO) {
            if (numParcelas != 1) {
                throw new ResumoCheckoutException("PARCELAMENTO_INVALIDO");
            }
        } else if (forma == FormaPagamento.CARTAO) {
            if (numParcelas < 1 || numParcelas > 12) {
                throw new ResumoCheckoutException("PARCELAMENTO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal valor = item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(valor);
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotal, List<ItemPedido> itens) {
        if (cupom == null) {
            return arredondar(BigDecimal.ZERO);
        }

        switch (cupom) {
            case "BEMVINDO10":
                return arredondar(subtotal.multiply(new BigDecimal("0.10")));
            case "MENOS50":
                return arredondar(new BigDecimal("50.00"));
            case "FRETEGRATIS":
                BigDecimal frete = calcularFrete(ModalidadeEntrega.EXPRESSA, calcularPesoTotal(itens), NivelClube.BRONZE);
                return frete;
            case "LEVE3PAGUE2":
                return calcularDescontoLeve3Pague2(itens);
            default:
                return arredondar(BigDecimal.ZERO);
        }
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemPedido> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            int quantidade = item.getQuantidade();
            if (quantidade >= 3) {
                int gratis = quantidade / 3;
                BigDecimal valorGratis = item.getPrecoUnitario().multiply(new BigDecimal(gratis));
                desconto = desconto.add(valorGratis);
            }
        }
        return arredondar(desconto);
    }

    private BigDecimal calcularPesoTotal(List<ItemPedido> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal peso = item.getPesoKg().multiply(new BigDecimal(item.getQuantidade()));
            pesoTotal = pesoTotal.add(peso);
        }
        return pesoTotal;
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, BigDecimal pesoTotal, NivelClube nivelClube) {
        if (nivelClube.temFreteGratis()) {
            return BigDecimal.ZERO;
        }

        if (modalidade == ModalidadeEntrega.RETIRADA_LOJA) {
            return BigDecimal.ZERO;
        }

        BigDecimal frete = modalidade.getValorBase().add(modalidade.getValorPorKg().multiply(pesoTotal));
        return arredondar(frete);
    }

    private BigDecimal calcularSeguro(BigDecimal subtotal, Regiao regiao) {
        BigDecimal seguro = subtotal.multiply(regiao.getPercentualSeguro());
        return arredondar(seguro);
    }

    private BigDecimal calcularAjustePagamento(FormaPagamento formaPagamento, BigDecimal baseParaAjuste, int parcelas) {
        switch (formaPagamento) {
            case PIX:
                return calcularDescontoPix(baseParaAjuste);
            case BOLETO:
                return arredondar(new BigDecimal("3.49"));
            case CARTAO:
                if (parcelas <= 3) {
                    return arredondar(BigDecimal.ZERO);
                } else {
                    return calcularJurosCartao(baseParaAjuste, parcelas);
                }
            default:
                return arredondar(BigDecimal.ZERO);
        }
    }

    private BigDecimal calcularDescontoPix(BigDecimal baseParaAjuste) {
        BigDecimal desconto = baseParaAjuste.multiply(PIX_DESCONTO);
        return arredondar(desconto).negate();
    }

    private BigDecimal calcularJurosCartao(BigDecimal baseParaAjuste, int parcelas) {
        BigDecimal parcela = calcularParcelaComJuros(baseParaAjuste, parcelas);
        BigDecimal totalComJuros = parcela.multiply(new BigDecimal(parcelas));
        BigDecimal juros = totalComJuros.subtract(baseParaAjuste);
        return arredondar(juros);
    }

    private BigDecimal calcularParcelaComJuros(BigDecimal total, int parcelas) {
        BigDecimal taxa = JUROS_CARTAO;
        BigDecimal umMaisTaxa = taxa.add(BigDecimal.ONE);
        BigDecimal potenciaPositiva = umMaisTaxa.pow(parcelas);
        BigDecimal potencia = BigDecimal.ONE.divide(potenciaPositiva, 10, RoundingMode.HALF_EVEN);
        BigDecimal denominador = BigDecimal.ONE.subtract(potencia);
        BigDecimal numerador = total.multiply(taxa);
        BigDecimal parcela = numerador.divide(denominador, 10, RoundingMode.HALF_EVEN);
        return arredondar(parcela);
    }

    private BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        BigDecimal valor = totalFinal.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN);
        return arredondar(valor);
    }

    private BigDecimal calcularCredito(BigDecimal subtotal, NivelClube nivelClube) {
        BigDecimal credito = subtotal.multiply(nivelClube.getPercentualCredito());
        return arredondar(credito);
    }

    private boolean temBrinde(BigDecimal subtotal, NivelClube nivelClube) {
        if (!nivelClube.temBrindeDisponivel()) {
            return false;
        }
        return subtotal.compareTo(new BigDecimal("500.00")) >= 0;
    }

    private boolean cupomExiste(String cupom) {
        return cupom.equals("BEMVINDO10") || cupom.equals("MENOS50") ||
               cupom.equals("FRETEGRATIS") || cupom.equals("LEVE3PAGUE2");
    }

    private boolean cupomAplicavel(String cupom, List<ItemPedido> itens) {
        if (cupom.equals("MENOS50")) {
            BigDecimal subtotal = calcularSubtotal(itens);
            return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
        }
        return true;
    }

    private BigDecimal arredondar(BigDecimal valor) {
        if (valor == null) {
            valor = BigDecimal.ZERO;
        }
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
