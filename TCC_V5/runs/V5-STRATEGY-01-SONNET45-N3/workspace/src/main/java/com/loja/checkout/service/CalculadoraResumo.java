package com.loja.checkout.service;

import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.cupom.*;
import com.loja.checkout.domain.modalidade.*;
import com.loja.checkout.domain.pagamento.*;
import com.loja.checkout.dto.*;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.loja.checkout.service.ArredondamentoUtil.arredondar;

@Service
public class CalculadoraResumo {

    private final Map<String, ModalidadeEntrega> modalidades = Map.of(
        "ECONOMICA", new Economica(),
        "EXPRESSA", new Expressa(),
        "RETIRADA_LOJA", new RetiradaLoja(),
        "MOTOBOY", new Motoboy()
    );

    private final Map<String, Cupom> cupons = Map.of(
        "BEMVINDO10", new BemVindo10(),
        "MENOS50", new Menos50(),
        "FRETEGRATIS", new FreteGratis(),
        "LEVE3PAGUE2", new Leve3Pague2()
    );

    private final Map<String, FormaPagamento> formasPagamento = Map.of(
        "PIX", new Pix(),
        "CARTAO", new Cartao(),
        "BOLETO", new Boleto()
    );

    public ResumoResponse calcular(PedidoRequest request) {
        validarPedido(request);

        NivelClube nivelClube = parseNivelClube(request.nivelClube());
        Regiao regiao = parseRegiao(request.regiao());

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.itens());
        BigDecimal pesoTotal = calcularPesoTotal(request.itens());

        ModalidadeEntrega modalidade = obterModalidade(request.modalidadeEntrega());
        validarModalidade(modalidade, pesoTotal);

        BigDecimal frete = nivelClube.isFreteGratis()
            ? new BigDecimal("0.00")
            : modalidade.calcularFrete(pesoTotal);

        BigDecimal descontoCupom = calcularDescontoCupom(
            request.cupom(),
            subtotalProdutos,
            frete,
            request.itens()
        );

        BigDecimal seguro = arredondar(subtotalProdutos.multiply(regiao.getPercentualSeguro()));

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        FormaPagamento formaPagamento = obterFormaPagamento(request.formaPagamento());

        validarParcelamento(formaPagamento, parcelas);
        validarFormaPagamento(formaPagamento, totalPedido);

        BigDecimal ajustePagamento = formaPagamento.calcularAjuste(totalPedido, parcelas);
        BigDecimal totalFinal = arredondar(totalPedido.add(ajustePagamento));
        BigDecimal valorParcela = formaPagamento.calcularValorParcela(totalPedido, ajustePagamento, parcelas);

        BigDecimal credito = arredondar(subtotalProdutos.multiply(nivelClube.getPercentualCredito()));
        boolean brinde = nivelClube.isElegiveParaBrinde()
            && subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;

        return new ResumoResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            modalidade.getPrazoEntregaDias(),
            seguro,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela,
            credito,
            brinde
        );
    }

    private void validarPedido(PedidoRequest request) {
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }

        for (ItemRequest item : request.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() == null || item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
        }
    }

    private NivelClube parseNivelClube(String nivel) {
        if (nivel == null) {
            throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
        try {
            return NivelClube.valueOf(nivel);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
    }

    private Regiao parseRegiao(String regiao) {
        if (regiao == null) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            BigDecimal totalItem = item.precoUnitario().multiply(new BigDecimal(item.quantidade()));
            subtotal = subtotal.add(totalItem);
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return peso;
    }

    private ModalidadeEntrega obterModalidade(String codigo) {
        if (codigo == null || !modalidades.containsKey(codigo)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }
        return modalidades.get(codigo);
    }

    private void validarModalidade(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (!modalidade.aceitaPedido(pesoTotal)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
    }

    private BigDecimal calcularDescontoCupom(String codigo, BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {
        if (codigo == null || codigo.isEmpty()) {
            return new BigDecimal("0.00");
        }

        if (!cupons.containsKey(codigo)) {
            throw new CheckoutException(CodigoErro.CUPOM_INVALIDO);
        }

        Cupom cupom = cupons.get(codigo);

        if (!cupom.aplicavel(subtotal)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        List<Cupom.ItemCupom> itensCupom = itens.stream()
            .map(i -> new Cupom.ItemCupom(i.nome(), i.precoUnitario(), i.quantidade()))
            .toList();

        return cupom.calcularDesconto(subtotal, frete, itensCupom);
    }

    private FormaPagamento obterFormaPagamento(String codigo) {
        if (codigo == null || !formasPagamento.containsKey(codigo)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        return formasPagamento.get(codigo);
    }

    private void validarParcelamento(FormaPagamento forma, int parcelas) {
        if (!forma.aceitaParcelamento(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    private void validarFormaPagamento(FormaPagamento forma, BigDecimal total) {
        if (!forma.aceitaPedido(total)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
    }
}
