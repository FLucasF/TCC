package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.OpcaoEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.web.PedidoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {
    private final Map<String, OpcaoEntrega> entregas;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveis;
    private final Map<String, FormaPagamento> pagamentos;

    public ResumoService(List<OpcaoEntrega> entregas, List<Cupom> cupons,
                         List<NivelClube> niveis, List<FormaPagamento> pagamentos) {
        this.entregas = indexar(entregas, OpcaoEntrega::codigo);
        this.cupons = indexar(cupons, Cupom::codigo);
        this.niveis = indexar(niveis, NivelClube::codigo);
        this.pagamentos = indexar(pagamentos, FormaPagamento::codigo);
    }

    private static <T> Map<String, T> indexar(List<T> lista, Function<T, String> chave) {
        return lista.stream().collect(Collectors.toUnmodifiableMap(chave, Function.identity()));
    }

    public ResumoResponse calcular(PedidoRequest req) {
        // 1
        List<Item> itens = validarItens(req.itens());
        // 2
        NivelClube nivel = buscar(niveis, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        // 3
        Regiao regiao = regiao(req.regiao());
        // 4 e 5
        OpcaoEntrega entrega = buscar(entregas, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        BigDecimal peso = itens.stream().map(Item::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!entrega.atende(peso)) {
            throw new ErroCheckout("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = Dinheiro.arredondar(
                itens.stream().map(Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal frete = nivel.freteGratis() ? Dinheiro.ZERO : entrega.frete(peso);

        // 6 e 7
        BigDecimal desconto = Dinheiro.ZERO;
        if (req.cupom() != null) {
            Cupom cupom = buscar(cupons, req.cupom(), "CUPOM_INVALIDO");
            var ctx = new Cupom.ContextoCupom(itens, subtotal, frete);
            if (!cupom.aplicavel(ctx)) {
                throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
            }
            desconto = cupom.desconto(ctx);
        }

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);

        // 8, 9 e 10
        FormaPagamento pagamento = buscar(pagamentos, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!pagamento.parcelasPermitidas(parcelas)) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }
        if (!pagamento.atende(total)) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        FormaPagamento.Resultado resultado = pagamento.calcular(total, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                entrega.prazoDias(),
                seguro,
                resultado.valorFinal().subtract(total),
                resultado.valorFinal(),
                parcelas,
                resultado.valorParcela(),
                Dinheiro.percentual(subtotal, nivel.taxaCredito()),
                nivel.brinde(subtotal));
    }

    private static List<Item> validarItens(List<PedidoRequest.ItemRequest> brutos) {
        if (brutos == null || brutos.isEmpty()) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }
        List<Item> itens = new ArrayList<>();
        for (PedidoRequest.ItemRequest i : brutos) {
            if (i == null || i.precoUnitario() == null || i.precoUnitario().signum() <= 0
                    || i.quantidade() == null || i.quantidade() <= 0
                    || i.pesoKg() == null || i.pesoKg().signum() <= 0) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
            itens.add(new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()));
        }
        return itens;
    }

    private static Regiao regiao(String valor) {
        for (Regiao r : Regiao.values()) {
            if (r.name().equals(valor)) {
                return r;
            }
        }
        throw new ErroCheckout("REGIAO_INVALIDA");
    }

    private static <T> T buscar(Map<String, T> mapa, String chave, String erro) {
        T valor = chave == null ? null : mapa.get(chave);
        if (valor == null) {
            throw new ErroCheckout(erro);
        }
        return valor;
    }
}
