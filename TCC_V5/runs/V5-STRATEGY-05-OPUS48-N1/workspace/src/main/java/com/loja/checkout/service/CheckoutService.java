package com.loja.checkout.service;

import com.loja.checkout.domain.ItemPedido;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.comum.Dinheiro;
import com.loja.checkout.domain.comum.Registro;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.cupom.CupomContexto;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.CheckoutResponse;
import com.loja.checkout.web.ItemRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Monta o resumo da compra. Esta classe guarda o que e igual em todo pedido:
 * a ordem das etapas do calculo e a ordem em que os problemas sao conferidos.
 * O que muda de caso para caso (entrega, cupom, clube, pagamento) ela delega
 * para o caso escolhido, achado por busca nos registros.
 */
@Service
public class CheckoutService {

    private final Registro<ModalidadeEntrega> entregas;
    private final Registro<Cupom> cupons;
    private final Registro<NivelClube> niveisClube;
    private final Registro<FormaPagamento> pagamentos;

    public CheckoutService(Registro<ModalidadeEntrega> entregas,
                           Registro<Cupom> cupons,
                           Registro<NivelClube> niveisClube,
                           Registro<FormaPagamento> pagamentos) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.niveisClube = niveisClube;
        this.pagamentos = pagamentos;
    }

    public CheckoutResponse calcular(CheckoutRequest pedido) {
        List<ItemPedido> itens = validarItens(pedido.itens());

        NivelClube clube = niveisClube.buscar(pedido.nivelClube())
                .orElseThrow(() -> new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.porNome(pedido.regiao())
                .orElseThrow(() -> new CheckoutException(CodigoErro.REGIAO_INVALIDA));
        ModalidadeEntrega entrega = entregas.buscar(pedido.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(CodigoErro.MODALIDADE_INVALIDA));

        BigDecimal subtotal = Dinheiro.centavos(somaProdutos(itens));
        BigDecimal peso = somaPeso(itens);

        if (!entrega.atende(peso)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal frete = clube.freteAplicado(Dinheiro.centavos(entrega.frete(peso)));

        BigDecimal desconto = aplicarCupom(pedido.cupom(), new CupomContexto(itens, subtotal, frete));

        BigDecimal seguro = Dinheiro.centavos(subtotal.multiply(regiao.percentual()));
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotal.subtract(desconto).add(frete).add(seguro));

        FormaPagamento pagamento = pagamentos.buscar(pedido.formaPagamento())
                .orElseThrow(() -> new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!pagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!pagamento.atende(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = Dinheiro.centavos(resultado.valorFinal().subtract(totalPedido));

        return new CheckoutResponse(
                subtotal,
                desconto,
                frete,
                entrega.prazoDias(),
                seguro,
                ajuste,
                resultado.valorFinal(),
                parcelas,
                resultado.valorParcela(),
                Dinheiro.centavos(clube.credito(subtotal)),
                clube.brinde(subtotal));
    }

    private BigDecimal aplicarCupom(String codigo, CupomContexto contexto) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(CodigoErro.CUPOM_INVALIDO));
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.centavos(cupom.desconto(contexto));
    }

    private List<ItemPedido> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<ItemPedido> validos = new ArrayList<>(itens.size());
        for (ItemRequest item : itens) {
            if (item == null
                    || positivo(item.precoUnitario()) == null
                    || positivo(item.pesoKg()) == null
                    || item.quantidade() == null
                    || item.quantidade() <= 0) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
            validos.add(new ItemPedido(item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return validos;
    }

    private BigDecimal positivo(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return valor;
    }

    private BigDecimal somaProdutos(List<ItemPedido> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.totalItem());
        }
        return soma;
    }

    private BigDecimal somaPeso(List<ItemPedido> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.pesoTotalItem());
        }
        return soma;
    }
}
