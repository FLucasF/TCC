package com.loja.checkout.service;

import com.loja.checkout.api.dto.CheckoutRequest;
import com.loja.checkout.api.dto.CheckoutResponse;
import com.loja.checkout.api.dto.ItemRequest;
import com.loja.checkout.clube.ClubeRegistro;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomRegistro;
import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.ErroPedido;
import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.Pedido;
import com.loja.checkout.domain.PedidoRecusadoException;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.entrega.EntregaRegistro;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.PagamentoRegistro;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.seguro.CalculadoraSeguro;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Monta o resumo da compra na ordem combinada: produtos, desconto do cupom,
 * frete, seguro, total do pedido e, por fim, o ajuste da forma de pagamento.
 */
@Service
public class CheckoutService {

    private final EntregaRegistro entregas;
    private final CupomRegistro cupons;
    private final ClubeRegistro niveis;
    private final PagamentoRegistro pagamentos;
    private final CalculadoraSeguro seguros;

    public CheckoutService(EntregaRegistro entregas,
                           CupomRegistro cupons,
                           ClubeRegistro niveis,
                           PagamentoRegistro pagamentos,
                           CalculadoraSeguro seguros) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.niveis = niveis;
        this.pagamentos = pagamentos;
        this.seguros = seguros;
    }

    public CheckoutResponse calcular(CheckoutRequest requisicao) {
        List<Item> itens = validarItens(requisicao);
        NivelClube nivel = niveis.buscar(texto(requisicao.nivelClube()))
                .orElseThrow(() -> new PedidoRecusadoException(ErroPedido.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = regiao(texto(requisicao.regiao()));
        Pedido pedido = Pedido.de(itens, nivel, regiao);

        ModalidadeEntrega entrega = entregas.buscar(texto(requisicao.modalidadeEntrega()))
                .orElseThrow(() -> new PedidoRecusadoException(ErroPedido.MODALIDADE_INVALIDA));
        if (!entrega.atende(pedido)) {
            throw new PedidoRecusadoException(ErroPedido.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal frete = nivel.freteGratis()
                ? Dinheiro.ZERO
                : Dinheiro.centavos(entrega.calcularFrete(pedido));

        BigDecimal descontoCupom = calcularDescontoCupom(texto(requisicao.cupom()), pedido, frete);
        BigDecimal seguro = seguros.calcular(pedido);
        BigDecimal totalPedido = Dinheiro.centavos(pedido.subtotalProdutos()
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro));

        FormaPagamento pagamento = pagamentos.buscar(texto(requisicao.formaPagamento()))
                .orElseThrow(() -> new PedidoRecusadoException(ErroPedido.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new PedidoRecusadoException(ErroPedido.PARCELAMENTO_INVALIDO);
        }
        if (!pagamento.atende(totalPedido)) {
            throw new PedidoRecusadoException(ErroPedido.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        return new CheckoutResponse(
                pedido.subtotalProdutos(),
                descontoCupom,
                frete,
                entrega.prazoEntregaDias(),
                seguro,
                resultado.ajuste(),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                Dinheiro.percentual(pedido.subtotalProdutos(), nivel.percentualCredito()),
                nivel.temBrinde(pedido.subtotalProdutos()));
    }

    private List<Item> validarItens(CheckoutRequest requisicao) {
        if (requisicao == null || requisicao.itens() == null || requisicao.itens().isEmpty()) {
            throw new PedidoRecusadoException(ErroPedido.PEDIDO_INVALIDO);
        }
        List<Item> itens = new ArrayList<>();
        for (ItemRequest item : requisicao.itens()) {
            if (item == null
                    || !positivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || !positivo(item.pesoKg())) {
                throw new PedidoRecusadoException(ErroPedido.PEDIDO_INVALIDO);
            }
            itens.add(new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return itens;
    }

    private BigDecimal calcularDescontoCupom(String codigo, Pedido pedido, BigDecimal frete) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo)
                .orElseThrow(() -> new PedidoRecusadoException(ErroPedido.CUPOM_INVALIDO));
        ContextoCupom contexto = new ContextoCupom(pedido, frete);
        if (!cupom.aplicavel(contexto)) {
            throw new PedidoRecusadoException(ErroPedido.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.centavos(cupom.calcularDesconto(contexto));
    }

    private Regiao regiao(String codigo) {
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new PedidoRecusadoException(ErroPedido.REGIAO_INVALIDA);
        }
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
