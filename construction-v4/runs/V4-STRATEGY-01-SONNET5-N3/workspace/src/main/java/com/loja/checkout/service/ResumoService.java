package com.loja.checkout.service;

import com.loja.checkout.clube.BeneficioClube;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.CodigoErro;
import com.loja.checkout.model.Item;
import com.loja.checkout.model.Regiao;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, BeneficioClube> beneficiosClube;
    private final Map<String, FormaPagamento> formasPagamento;

    public ResumoService(
            Map<String, ModalidadeEntrega> modalidades,
            Map<String, Cupom> cupons,
            Map<String, BeneficioClube> beneficiosClube,
            Map<String, FormaPagamento> formasPagamento) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.beneficiosClube = beneficiosClube;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        List<Item> itens = validarItens(request.itens());
        BigDecimal subtotalProdutos = somarSubtotal(itens);
        BigDecimal pesoTotal = somarPeso(itens);

        BeneficioClube beneficioClube = buscarBeneficioClube(request.nivelClube());
        Regiao regiao = validarRegiao(request.regiao());
        ModalidadeEntrega modalidade = buscarModalidade(request.modalidadeEntrega());

        if (!modalidade.disponivelPara(pesoTotal)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        int prazoEntregaDias = modalidade.prazoDias();
        BigDecimal frete = beneficioClube.freteGratis()
                ? Arredondamento.ZERO
                : Arredondamento.centavos(modalidade.calcularFrete(pesoTotal));

        BigDecimal descontoCupom = calcularDescontoCupom(request.cupom(), itens, subtotalProdutos, frete);

        BigDecimal seguro = Arredondamento.centavos(subtotalProdutos.multiply(regiao.percentualSeguro()));

        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);

        FormaPagamento formaPagamento = buscarFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        if (!formaPagamento.parcelasPermitidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.disponivelPara(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = Arredondamento.centavos(beneficioClube.calcularCredito(subtotalProdutos));
        boolean brinde = beneficioClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                Arredondamento.centavos(subtotalProdutos),
                descontoCupom,
                frete,
                prazoEntregaDias,
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde);
    }

    private List<Item> validarItens(List<ItemRequest> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<Item> itens = itensRequest.stream()
                .map(i -> new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()))
                .toList();
        for (Item item : itens) {
            if (!item.valido()) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
        }
        return itens;
    }

    private BigDecimal somarSubtotal(List<Item> itens) {
        return itens.stream().map(Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarPeso(List<Item> itens) {
        return itens.stream().map(Item::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BeneficioClube buscarBeneficioClube(String nivelClube) {
        BeneficioClube beneficio = nivelClube != null ? beneficiosClube.get(nivelClube) : null;
        if (beneficio == null) {
            throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
        return beneficio;
    }

    private Regiao validarRegiao(String regiao) {
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }
    }

    private ModalidadeEntrega buscarModalidade(String modalidadeEntrega) {
        ModalidadeEntrega modalidade = modalidadeEntrega != null ? modalidades.get(modalidadeEntrega) : null;
        if (modalidade == null) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }
        return modalidade;
    }

    private BigDecimal calcularDescontoCupom(
            String codigoCupom, List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        if (codigoCupom == null) {
            return Arredondamento.ZERO;
        }
        Cupom cupom = cupons.get(codigoCupom);
        if (cupom == null) {
            throw new CheckoutException(CodigoErro.CUPOM_INVALIDO);
        }
        ContextoCupom contexto = new ContextoCupom(itens, subtotalProdutos, frete);
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return Arredondamento.centavos(cupom.calcularDesconto(contexto));
    }

    private FormaPagamento buscarFormaPagamento(String formaPagamento) {
        FormaPagamento forma = formaPagamento != null ? formasPagamento.get(formaPagamento) : null;
        if (forma == null) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        return forma;
    }
}
