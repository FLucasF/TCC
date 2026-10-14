package com.loja.dominio;

import java.util.List;

import static com.loja.dominio.Dinheiro.de;

/** Catálogo inicial, usado quando o banco está vazio. */
public final class Catalogo {

    private Catalogo() {
    }

    public static List<Produto> inicial() {
        return List.of(
                new Produto("CAM-001", "Camiseta básica", de("79.90"), de("0.30"), 50, false),
                new Produto("TEN-001", "Tênis corrida", de("249.90"), de("1.20"), 10, false),
                new Produto("FON-001", "Fone sem fio", de("199.90"), de("0.25"), 5, false),
                new Produto("MEI-001", "Meia esportiva", de("19.90"), de("0.10"), 100, false),
                new Produto("JAQ-001", "Jaqueta corta-vento", de("389.90"), de("0.80"), 3, false),
                new Produto("MOC-001", "Mochila urbana", de("159.90"), de("0.90"), 1, false),
                new Produto("BON-001", "Boné aba reta", de("59.90"), de("0.15"), 0, false),
                new Produto("REL-001", "Relógio digital", de("129.90"), de("0.20"), 8, true));
    }
}
