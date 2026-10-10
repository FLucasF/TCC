package com.loja.checkout.service;

import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.Regiao;
import com.loja.checkout.strategy.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveisClube;
    private final Map<String, FormaPagamento> formasPagamento;

    public CheckoutService(
        List<ModalidadeEntrega> listaModalidades,
        List<Cupom> listaCupons,
        List<NivelClube> listaNiveis,
        List<FormaPagamento> listaFormas
    ) {
        this.modalidades = listaModalidades.stream()
            .collect(Collectors.toMap(ModalidadeEntrega::getCodigo, Function.identity()));
        this.cupons = listaCupons.stream()
            .collect(Collectors.toMap(Cupom::getCodigo, Function.identity()));
        this.niveisClube = listaNiveis.stream()
            .collect(Collectors.toMap(NivelClube::getCodigo, Function.identity()));
        this.formasPagamento = listaFormas.stream()
            .collect(Collectors.toMap(FormaPagamento::getCodigo, Function.identity()));
    }

    public ResumoResponse calcularResumo(PedidoRequest request) {
        validarPedido(request);

        NivelClube nivel = obterNivelClube(request.nivelClube());
        Regiao regiao = obterRegiao(request.regiao());
        ModalidadeEntrega modalidade = obterModalidadeEntrega(request.modalidadeEntrega());

        validarModalidadeDisponivel(modalidade, request.itens());

        Cupom cupom = obterCupom(request.cupom());
        FormaPagamento formaPagamento = obterFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelas() != null ? request.parcelas() : 1;

        validarParcelamento(formaPagamento, parcelas);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.itens());

        validarCupomAplicavel(cupom, subtotalProdutos, request.itens());

        BigDecimal freteOriginal = modalidade.calcularFrete(request.itens());
        BigDecimal descontoCupom = cupom != null
            ? cupom.calcularDesconto(subtotalProdutos, freteOriginal, request.itens())
            : BigDecimal.ZERO.setScale(2);

        BigDecimal frete = nivel.ajustarFrete(freteOriginal);
        Integer prazoEntrega = modalidade.getPrazoEntregaDias();

        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);

        validarFormaPagamentoDisponivel(formaPagamento, totalPedido);

        BigDecimal ajustePagamento = formaPagamento.calcularAjuste(totalPedido, parcelas);
        BigDecimal totalFinal = totalPedido.add(ajustePagamento);
        BigDecimal valorParcela = formaPagamento.calcularValorParcela(totalPedido, ajustePagamento, parcelas);

        BigDecimal credito = nivel.calcularCredito(subtotalProdutos);
        Boolean brinde = nivel.ganhaBrinde(subtotalProdutos);

        return new ResumoResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            prazoEntrega,
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
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : request.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() == null || item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
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

    private ModalidadeEntrega obterModalidadeEntrega(String codigo) {
        if (codigo == null || !modalidades.containsKey(codigo)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        return modalidades.get(codigo);
    }

    private void validarModalidadeDisponivel(ModalidadeEntrega modalidade, List<ItemCarrinho> itens) {
        if (!modalidade.aceitaPedido(itens)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private Cupom obterCupom(String codigo) {
        if (codigo == null) {
            return null;
        }
        if (!cupons.containsKey(codigo)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
        return cupons.get(codigo);
    }

    private void validarCupomAplicavel(Cupom cupom, BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        if (cupom != null && !cupom.aplicavel(subtotalProdutos, itens)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }

    private FormaPagamento obterFormaPagamento(String codigo) {
        if (codigo == null || !formasPagamento.containsKey(codigo)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        return formasPagamento.get(codigo);
    }

    private void validarParcelamento(FormaPagamento formaPagamento, int parcelas) {
        if (!formaPagamento.parcelamentoValido(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    private void validarFormaPagamentoDisponivel(FormaPagamento formaPagamento, BigDecimal totalPedido) {
        if (!formaPagamento.aceitaTotal(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        BigDecimal subtotal = itens.stream()
            .map(item -> item.precoUnitario().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        BigDecimal seguro = subtotalProdutos.multiply(regiao.getPercentualSeguro());
        return seguro.setScale(2, RoundingMode.HALF_EVEN);
    }
}
