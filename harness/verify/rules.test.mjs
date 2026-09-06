// @ts-check
/**
 * The rule cap of CLAUDE.md, as an invariant rather than an intention.
 *
 * A rule is one paragraph holding one independent imperative. Every paragraph below
 * the first `##` heading counts, except blockquotes, which explain a mechanism and
 * command nothing.
 */

import { test } from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const RULES_FILE = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)), "..", "CLAUDE.md",
);
const RULE_CAP = 10;

/**
 * @param {string} text
 * @returns {string[]}
 */
export function rules(text) {
  /** @type {string[]} */
  const counted = [];
  /** @type {string[]} */
  let current = [];
  let started = false;

  for (const raw of text.split(/\r?\n/)) {
    let line = raw;
    if (line.startsWith("## ")) {
      started = true;
      line = "";
    }
    if (!started) continue;
    if (line.trim()) {
      current.push(line);
    } else if (current.length) {
      counted.push(current.join("\n"));
      current = [];
    }
  }
  if (current.length) counted.push(current.join("\n"));

  return counted.filter((p) => !p.startsWith(">"));
}

const AMOSTRA = `# Título

Introdução, não é regra.

## A

Regra um.

Regra dois, em
duas linhas.

## B

> Mecanismo explicado, não é regra.

Regra três.
`;

test("conta um parágrafo por regra abaixo dos títulos", () => {
  assert.deepEqual(rules(AMOSTRA), [
    "Regra um.",
    "Regra dois, em\nduas linhas.",
    "Regra três.",
  ]);
});

test("um parágrafo a mais conta uma regra a mais", () => {
  assert.equal(
    rules(`${AMOSTRA}\nNunca faça X.\n`).length,
    rules(AMOSTRA).length + 1,
  );
});

test("CLAUDE.md respeita o cap", () => {
  const texto = fs.readFileSync(RULES_FILE, "utf-8");
  const encontradas = rules(texto);
  assert.ok(
    encontradas.length <= RULE_CAP,
    `${encontradas.length} regras, teto é ${RULE_CAP}:\n${encontradas.join("\n---\n")}`,
  );
});
