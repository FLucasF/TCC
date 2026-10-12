package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.SemCupom;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Orquestra a ordem do cálculo e as conferências. As regras de cada caso moram
 * nas implementações de modalidade, cupom, nível de clube e forma de pagamento.
 */
@Service
public class CalculadoraDeResumo {

    private static final Cupom SEM_CUPOM = new SemCupom();

    private final Catalogo<ModalidadeEntrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveisClube;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CalculadoraDeResumo(Catalogo<ModalidadeEntrega> entregas, Catalogo<Cupom> cupons,
            Catalogo<NivelClube> niveisClube, Catalogo<FormaPagamento> formasPagamento) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.niveisClube = niveisClube;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(ResumoRequest pedido) {
        List<Item> itens = conferirItens(pedido.itens());
        NivelClube clube = niveisClube.obrigatorio(pedido.nivelClube());
        Regiao regiao = Regiao.doCodigo(pedido.regiao());

        BigDecimal peso = pesoTotal(itens);
        ModalidadeEntrega entrega = entregas.obrigatorio(pedido.modalidadeEntrega());
        if (!entrega.atende(peso)) {
            throw new PedidoRecusadoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotal = subtotalProdutos(itens);
        BigDecimal frete = clube.frete(entrega.frete(peso));

        Cupom cupom = cupom(pedido.cupom());
        ContextoCupom contexto = new ContextoCupom(itens, subtotal, frete);
        if (!cupom.aplicavel(contexto)) {
            throw new PedidoRecusadoException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        BigDecimal desconto = cupom.desconto(contexto);

        BigDecimal seguro = regiao.seguroSobre(subtotal);
        BigDecimal totalPedido =
                Dinheiro.centavos(subtotal.subtract(desconto).add(frete).add(seguro));

        FormaPagamento pagamento = formasPagamento.obrigatorio(pedido.formaPagamento());
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new PedidoRecusadoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!pagamento.atende(totalPedido)) {
            throw new PedidoRecusadoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                entrega.prazoDias(),
                seguro,
                Dinheiro.centavos(resultado.totalFinal().subtract(totalPedido)),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                clube.credito(subtotal),
                clube.temBrinde(subtotal));
    }

    private List<Item> conferirItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<Item> conferidos = new ArrayList<>();
        for (ItemRequest item : itens) {
            if (item == null || naoEPositivo(item.precoUnitario()) || naoEPositivo(item.pesoKg())
                    || item.quantidade() == null || item.quantidade() <= 0) {
                throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
            }
            conferidos.add(new Item(item.nome(), item.precoUnitario(), item.quantidade(),
                    item.pesoKg()));
        }
        return conferidos;
    }

    private boolean naoEPositivo(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) <= 0;
    }

    private Cupom cupom(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return SEM_CUPOM;
        }
        return cupons.obrigatorio(codigo);
    }

    private BigDecimal subtotalProdutos(List<Item> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : itens) {
            subtotal = subtotal.add(item.total());
        }
        return Dinheiro.centavos(subtotal);
    }

    private BigDecimal pesoTotal(List<Item> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (Item item : itens) {
            peso = peso.add(item.peso());
        }
        return peso;
    }
}
