package com.loja.checkout.service;

import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.clube.*;
import com.loja.checkout.domain.cupom.*;
import com.loja.checkout.domain.modalidade.*;
import com.loja.checkout.domain.pagamento.*;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class CalculadoraResumoService {
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

    private final Map<String, NivelClube> niveisClube = Map.of(
        "BRONZE", new Bronze(),
        "PRATA", new Prata(),
        "OURO", new Ouro()
    );

    private final Map<String, FormaPagamento> formasPagamento = Map.of(
        "PIX", new Pix(),
        "CARTAO", new Cartao(),
        "BOLETO", new Boleto()
    );

    public ResumoResponse calcular(PedidoRequest pedido) {
        DadosPedido dados = validarECalcularDadosBasicos(pedido);

        FormaPagamento formaPagamento = obterFormaPagamento(pedido.formaPagamento());
        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;

        if (!formaPagamento.aceitaPedido(dados.totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        BigDecimal ajustePagamento = formaPagamento.calcularAjuste(dados.totalPedido, parcelas);
        BigDecimal totalFinal = dados.totalPedido.add(ajustePagamento);
        BigDecimal valorParcela = formaPagamento.calcularValorParcela(dados.totalPedido, parcelas);

        BigDecimal creditoProximaCompra = dados.nivelClube.calcularCredito(dados.subtotalProdutos);
        boolean brinde = dados.nivelClube.ganhaBrinde(dados.subtotalProdutos);

        return new ResumoResponse(
            dados.subtotalProdutos,
            dados.descontoCupom,
            dados.frete,
            dados.modalidade.obterPrazoDias(),
            dados.seguro,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela,
            creditoProximaCompra,
            brinde
        );
    }

    private DadosPedido validarECalcularDadosBasicos(PedidoRequest pedido) {
        validarPedidoBasico(pedido);
        validarNivelClube(pedido);
        validarRegiao(pedido);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(pedido.itens());

        ModalidadeEntrega modalidade = validarModalidade(pedido);
        NivelClube nivelClube = obterNivelClube(pedido.nivelClube());

        BigDecimal freteOriginal = modalidade.calcularFrete(pedido.itens());
        BigDecimal frete = nivelClube.zeraFrete() ? BigDecimal.ZERO.setScale(2) : freteOriginal;

        BigDecimal descontoCupom = validarECalcularCupom(pedido, subtotalProdutos, freteOriginal);

        Regiao regiao = obterRegiao(pedido.regiao());
        BigDecimal seguro = subtotalProdutos.multiply(regiao.getPercentualSeguro())
            .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);

        validarFormaPagamento(pedido);

        return new DadosPedido(subtotalProdutos, descontoCupom, frete, seguro, totalPedido, modalidade, nivelClube);
    }

    private void validarPedidoBasico(PedidoRequest pedido) {
        if (pedido.itens() == null || pedido.itens().isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        for (ItemCarrinho item : pedido.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario() <= 0 ||
                item.quantidade() == null || item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg() <= 0) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
        }
    }

    private void validarNivelClube(PedidoRequest pedido) {
        if (pedido.nivelClube() == null || !niveisClube.containsKey(pedido.nivelClube())) {
            throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
    }

    private void validarRegiao(PedidoRequest pedido) {
        try {
            if (pedido.regiao() == null) {
                throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
            }
            Regiao.valueOf(pedido.regiao());
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }
    }

    private ModalidadeEntrega validarModalidade(PedidoRequest pedido) {
        if (pedido.modalidadeEntrega() == null || !modalidades.containsKey(pedido.modalidadeEntrega())) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }

        ModalidadeEntrega modalidade = modalidades.get(pedido.modalidadeEntrega());
        if (!modalidade.aceitaPedido(pedido.itens())) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        return modalidade;
    }

    private BigDecimal validarECalcularCupom(PedidoRequest pedido, BigDecimal subtotalProdutos, BigDecimal freteOriginal) {
        if (pedido.cupom() == null || pedido.cupom().isBlank()) {
            return BigDecimal.ZERO.setScale(2);
        }

        if (!cupons.containsKey(pedido.cupom())) {
            throw new CheckoutException(CodigoErro.CUPOM_INVALIDO);
        }

        Cupom cupom = cupons.get(pedido.cupom());
        if (!cupom.aplicavel(subtotalProdutos, pedido.itens())) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        return cupom.calcularDesconto(subtotalProdutos, freteOriginal, pedido.itens());
    }

    private void validarFormaPagamento(PedidoRequest pedido) {
        if (pedido.formaPagamento() == null || !formasPagamento.containsKey(pedido.formaPagamento())) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        FormaPagamento formaPagamento = formasPagamento.get(pedido.formaPagamento());
        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;

        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal totalItem = BigDecimal.valueOf(item.precoUnitario())
                .multiply(BigDecimal.valueOf(item.quantidade()));
            subtotal = subtotal.add(totalItem);
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private NivelClube obterNivelClube(String nivel) {
        return niveisClube.get(nivel);
    }

    private ModalidadeEntrega obterModalidade(String modalidade) {
        return modalidades.get(modalidade);
    }

    private Cupom obterCupom(String codigo) {
        return cupons.get(codigo);
    }

    private Regiao obterRegiao(String regiao) {
        return Regiao.valueOf(regiao);
    }

    private FormaPagamento obterFormaPagamento(String forma) {
        return formasPagamento.get(forma);
    }

    private record DadosPedido(
        BigDecimal subtotalProdutos,
        BigDecimal descontoCupom,
        BigDecimal frete,
        BigDecimal seguro,
        BigDecimal totalPedido,
        ModalidadeEntrega modalidade,
        NivelClube nivelClube
    ) {}
}
