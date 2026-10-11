package com.loja.checkout;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.comum.Carrinho;
import com.loja.checkout.comum.Catalogo;
import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.comum.Item;
import com.loja.checkout.comum.Regiao;
import com.loja.checkout.comum.ResumoException;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.SemCupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.Pagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Calcula o resumo da compra, validando na ordem dos códigos de erro. */
@Service
public class ResumoService {

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;

    public ResumoService(List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
            List<NivelClube> niveis, List<FormaPagamento> formasPagamento) {
        this.modalidades = new Catalogo<>(modalidades);
        this.cupons = new Catalogo<>(cupons);
        this.niveis = new Catalogo<>(niveis);
        this.formasPagamento = new Catalogo<>(formasPagamento);
    }

    public ResumoResponse calcular(ResumoRequest request) {
        Carrinho carrinho = carrinho(request.itens());
        NivelClube nivel = niveis.buscar(request.nivelClube()).orElseThrow(() -> erro("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = Regiao.buscar(request.regiao()).orElseThrow(() -> erro("REGIAO_INVALIDA"));

        ModalidadeEntrega modalidade = modalidades.buscar(request.modalidadeEntrega())
                .orElseThrow(() -> erro("MODALIDADE_INVALIDA"));
        exigir(modalidade.atende(carrinho), "MODALIDADE_INDISPONIVEL");

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.frete(modalidade.frete(carrinho));

        Cupom cupom = cupom(request.cupom());
        ContextoCupom contextoCupom = new ContextoCupom(carrinho, frete);
        exigir(cupom.aplicavel(contextoCupom), "CUPOM_NAO_APLICAVEL");
        BigDecimal desconto = cupom.desconto(contextoCupom);

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        FormaPagamento forma = formasPagamento.buscar(request.formaPagamento())
                .orElseThrow(() -> erro("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = Objects.requireNonNullElse(request.parcelas(), 1);
        exigir(forma.parcelasPermitidas(parcelas), "PARCELAMENTO_INVALIDO");
        exigir(forma.disponivel(totalPedido), "FORMA_PAGAMENTO_INDISPONIVEL");
        Pagamento pagamento = forma.pagar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.totalFinal().subtract(totalPedido),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                Dinheiro.percentual(subtotal, nivel.percentualCredito()),
                nivel.daBrinde(subtotal));
    }

    private static Carrinho carrinho(List<ItemRequest> itens) {
        exigir(itens != null && !itens.isEmpty() && itens.stream().allMatch(ResumoService::itemValido),
                "PEDIDO_INVALIDO");
        return new Carrinho(itens.stream()
                .map(i -> new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()))
                .toList());
    }

    private static boolean itemValido(ItemRequest item) {
        return item != null
                && positivo(item.precoUnitario())
                && item.quantidade() != null && item.quantidade() > 0
                && positivo(item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    /** Sem código, o cliente não usou cupom; com código, ele precisa existir. */
    private Cupom cupom(String codigo) {
        if (codigo == null) {
            return SemCupom.INSTANCIA;
        }
        return cupons.buscar(codigo).orElseThrow(() -> erro("CUPOM_INVALIDO"));
    }

    private static void exigir(boolean condicao, String codigoErro) {
        if (!condicao) {
            throw erro(codigoErro);
        }
    }

    private static ResumoException erro(String codigo) {
        return new ResumoException(codigo);
    }
}
