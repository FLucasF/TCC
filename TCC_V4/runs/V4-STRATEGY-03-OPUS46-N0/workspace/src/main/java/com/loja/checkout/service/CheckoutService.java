package com.loja.checkout.service;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.Regiao;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private static final BigDecimal TAXA_PIX = new BigDecimal("0.05");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");
    private static final BigDecimal TAXA_JUROS_CARTAO = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MAXIMAS = 12;

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveisClube;

    public CheckoutService(List<ModalidadeEntrega> modalidades,
                           List<Cupom> cupons,
                           List<NivelClube> niveisClube) {
        this.modalidades = modalidades.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
        this.cupons = cupons.stream()
                .collect(Collectors.toMap(Cupom::codigo, Function.identity()));
        this.niveisClube = niveisClube.stream()
                .collect(Collectors.toMap(NivelClube::codigo, Function.identity()));
    }

    public CheckoutResponse calcular(CheckoutRequest request) {
        validarItens(request.itens());

        NivelClube nivel = obterNivelClube(request.nivelClube());
        Regiao regiao = obterRegiao(request.regiao());
        ModalidadeEntrega modalidade = obterModalidade(request.modalidadeEntrega());

        BigDecimal pesoTotal = calcularPesoTotal(request.itens());
        if (!modalidade.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = calcularSubtotal(request.itens());

        Cupom cupom = null;
        if (request.cupom() != null && !request.cupom().isEmpty()) {
            cupom = obterCupom(request.cupom());
            if (!cupom.aplicavel(request.itens(), subtotal)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        FormaPagamento formaPagamento = obterFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        validarParcelas(formaPagamento, parcelas);

        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        if (nivel.freteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom;
        if (cupom != null) {
            descontoCupom = cupom.calcularDesconto(request.itens(), subtotal, frete);
        } else {
            descontoCupom = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal seguro = round(subtotal.multiply(regiao.getTaxaSeguro()));

        BigDecimal totalPedido = round(subtotal.subtract(descontoCupom).add(frete).add(seguro));

        if (formaPagamento == FormaPagamento.BOLETO && totalPedido.compareTo(LIMITE_BOLETO) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        BigDecimal totalFinal;
        BigDecimal valorParcela;

        switch (formaPagamento) {
            case PIX -> {
                BigDecimal desconto = round(totalPedido.multiply(TAXA_PIX));
                totalFinal = round(totalPedido.subtract(desconto));
                valorParcela = totalFinal;
            }
            case BOLETO -> {
                totalFinal = round(totalPedido.add(TARIFA_BOLETO));
                valorParcela = totalFinal;
            }
            case CARTAO -> {
                if (parcelas <= PARCELAS_SEM_JUROS) {
                    totalFinal = totalPedido;
                    valorParcela = round(totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN));
                } else {
                    double taxa = TAXA_JUROS_CARTAO.doubleValue();
                    double total = totalPedido.doubleValue();
                    double parcelaCalc = total * taxa / (1 - Math.pow(1 + taxa, -parcelas));
                    valorParcela = new BigDecimal(parcelaCalc).setScale(2, RoundingMode.HALF_EVEN);
                    totalFinal = round(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
                }
            }
            default -> throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        BigDecimal ajustePagamento = round(totalFinal.subtract(totalPedido));

        BigDecimal credito = nivel.calcularCredito(subtotal);
        boolean brinde = nivel.brinde(subtotal);

        return new CheckoutResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento,
                totalFinal,
                parcelas,
                valorParcela,
                credito,
                brinde
        );
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemRequest item : itens) {
            if (item == null
                    || item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private NivelClube obterNivelClube(String codigo) {
        if (codigo == null || !niveisClube.containsKey(codigo)) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        return niveisClube.get(codigo);
    }

    private Regiao obterRegiao(String codigo) {
        if (codigo == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega obterModalidade(String codigo) {
        if (codigo == null || !modalidades.containsKey(codigo)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        return modalidades.get(codigo);
    }

    private Cupom obterCupom(String codigo) {
        if (!cupons.containsKey(codigo)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
        return cupons.get(codigo);
    }

    private FormaPagamento obterFormaPagamento(String codigo) {
        if (codigo == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            return FormaPagamento.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private void validarParcelas(FormaPagamento forma, int parcelas) {
        switch (forma) {
            case PIX, BOLETO -> {
                if (parcelas != 1) {
                    throw new CheckoutException("PARCELAMENTO_INVALIDO");
                }
            }
            case CARTAO -> {
                if (parcelas < 1 || parcelas > PARCELAS_MAXIMAS) {
                    throw new CheckoutException("PARCELAMENTO_INVALIDO");
                }
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return round(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
