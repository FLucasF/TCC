package br.com.mare.pousada.report;

import java.util.List;

/** O miolo de um relatório: cabeçalho, linhas e a linha de total. */
public record Table(List<String> header, List<List<String>> rows, List<String> total) {
}
