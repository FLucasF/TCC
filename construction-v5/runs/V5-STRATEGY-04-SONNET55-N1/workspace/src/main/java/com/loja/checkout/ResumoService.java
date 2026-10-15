package com.loja.checkout;

import com.loja.checkout.clube.Nivel;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.Entrega;
import com.loja.checkout.pagamento.Cobranca;
import com.loja.checkout.pagamento.Pagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {
    private final Catalogo<Entrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<Nivel> niveis;
    private final Catalogo<Pagamento> pagamentos;

    public ResumoService(Catalogo<Entrega> entregas, Catalogo<Cupom> cupons,
                         Catalogo<Nivel> niveis, Catalogo<Pagamento> pagamentos) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.niveis = niveis;
        this.pagamentos = pagamentos;
    }

    public ResumoResponse calcular(ResumoRequest req) {
        List<Item> itens = itens(req);
        Nivel nivel = niveis.buscar(req.nivelClube()).orElseThrow(() -> new ErroNegocio("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = Regiao.de(req.regiao()).orElseThrow(() -> new ErroNegocio("REGIAO_INVALIDA"));
        Entrega entrega = entregas.buscar(req.modalidadeEntrega())
                .orElseThrow(() -> new ErroNegocio("MODALIDADE_INVALIDA"));

        BigDecimal subtotal = itens.stream().map(Item::subtotal).reduce(Dinheiro.ZERO, BigDecimal::add);
        BigDecimal peso = itens.stream().map(Item::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!entrega.atende(peso)) {
            throw new ErroNegocio("MODALIDADE_INDISPONIVEL");
        }
        BigDecimal frete = nivel.isentaFrete() ? Dinheiro.ZERO : entrega.custo(peso);

        BigDecimal desconto = descontoCupom(req.cupom(), new ContextoCupom(itens, subtotal, frete));
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);

        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        Pagamento pagamento = pagamentos.buscar(req.formaPagamento())
                .orElseThrow(() -> new ErroNegocio("FORMA_PAGAMENTO_INVALIDA"));
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new ErroNegocio("PARCELAMENTO_INVALIDO");
        }
        if (!pagamento.disponivel(total)) {
            throw new ErroNegocio("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        Cobranca cobranca = pagamento.cobrar(total, parcelas);

        return new ResumoResponse(subtotal, desconto, frete, entrega.prazoDias(), seguro,
                cobranca.totalFinal().subtract(total), cobranca.totalFinal(), parcelas,
                cobranca.valorParcela(), nivel.creditoProximaCompra(subtotal), nivel.ganhaBrinde(subtotal));
    }

    private BigDecimal descontoCupom(String codigo, ContextoCupom contexto) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo).orElseThrow(() -> new ErroNegocio("CUPOM_INVALIDO"));
        if (!cupom.aplicavel(contexto)) {
            throw new ErroNegocio("CUPOM_NAO_APLICAVEL");
        }
        return cupom.desconto(contexto);
    }

    private static List<Item> itens(ResumoRequest req) {
        if (req.itens() == null || req.itens().isEmpty()) {
            throw new ErroNegocio("PEDIDO_INVALIDO");
        }
        return req.itens().stream().map(i -> {
            if (i == null || !positivo(i.precoUnitario()) || !positivo(i.pesoKg())
                    || i.quantidade() == null || i.quantidade() <= 0) {
                throw new ErroNegocio("PEDIDO_INVALIDO");
            }
            return new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg());
        }).toList();
    }

    private static boolean positivo(BigDecimal v) {
        return Objects.nonNull(v) && v.signum() > 0;
    }
}
