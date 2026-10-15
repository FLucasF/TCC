package com.loja.checkout.domain;

import com.loja.checkout.api.dto.CheckoutRequest;
import com.loja.checkout.api.dto.CheckoutResponse;
import com.loja.checkout.api.dto.ItemRequest;
import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.clube.RegistroNiveis;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.cupom.CupomContexto;
import com.loja.checkout.domain.cupom.RegistroCupons;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.entrega.RegistroModalidades;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.RegistroPagamentos;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Monta o resumo da compra. Segue a ordem de cálculo do enunciado e, para os
 * erros, confere na ordem definida e recusa no primeiro problema encontrado.
 * As decisões que mudam de caso para caso ficam nas estratégias (entrega,
 * cupom, clube, pagamento); aqui mora só a sequência que é igual em todo pedido.
 */
@Service
public class ResumoCheckoutService {

    private final RegistroModalidades modalidades;
    private final RegistroCupons cupons;
    private final RegistroNiveis niveis;
    private final RegistroPagamentos pagamentos;

    public ResumoCheckoutService(RegistroModalidades modalidades, RegistroCupons cupons,
                                 RegistroNiveis niveis, RegistroPagamentos pagamentos) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.niveis = niveis;
        this.pagamentos = pagamentos;
    }

    public CheckoutResponse resumir(CheckoutRequest req) {
        List<ItemCalculo> itens = validarItens(req.itens());
        BigDecimal subtotal = somaProdutos(itens);
        BigDecimal peso = somaPeso(itens);

        NivelClube nivel = niveis.buscar(req.nivelClube())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.NIVEL_CLUBE_INVALIDO));

        Regiao regiao = Regiao.porCodigo(req.regiao())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.REGIAO_INVALIDA));

        ModalidadeEntrega modalidade = modalidades.buscar(req.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.MODALIDADE_INVALIDA));
        if (!modalidade.disponivel(peso)) {
            throw new CheckoutException(ErroCheckout.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal frete = nivel.freteDevido(modalidade.frete(peso));
        BigDecimal descontoCupom = calcularDesconto(req.cupom(), itens, subtotal, frete);
        BigDecimal seguro = Dinheiro.percentual(subtotal, regiao.percentualSeguro());
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        FormaPagamento pagamento = pagamentos.buscar(req.formaPagamento())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INVALIDA));
        if (!pagamento.parcelasPermitidas(parcelas)) {
            throw new CheckoutException(ErroCheckout.PARCELAMENTO_INVALIDO);
        }
        if (!pagamento.disponivel(totalPedido)) {
            throw new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = Dinheiro.centavos(resultado.totalFinal().subtract(totalPedido));

        return new CheckoutResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajuste,
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private List<ItemCalculo> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }
        return itens.stream().map(this::validarItem).toList();
    }

    private ItemCalculo validarItem(ItemRequest item) {
        if (item == null
                || !positivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || !positivo(item.pesoKg())) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }
        return new ItemCalculo(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal somaProdutos(List<ItemCalculo> itens) {
        return itens.stream()
                .map(ItemCalculo::totalLinha)
                .reduce(Dinheiro.ZERO, BigDecimal::add);
    }

    private BigDecimal somaPeso(List<ItemCalculo> itens) {
        return itens.stream()
                .map(ItemCalculo::pesoLinha)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularDesconto(String codigoCupom, List<ItemCalculo> itens,
                                        BigDecimal subtotal, BigDecimal frete) {
        if (codigoCupom == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigoCupom)
                .orElseThrow(() -> new CheckoutException(ErroCheckout.CUPOM_INVALIDO));
        CupomContexto ctx = new CupomContexto(itens, subtotal, frete);
        if (!cupom.aplicavel(ctx)) {
            throw new CheckoutException(ErroCheckout.CUPOM_NAO_APLICAVEL);
        }
        return cupom.desconto(ctx);
    }
}
