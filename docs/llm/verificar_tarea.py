#!/usr/bin/env python3
"""Verifica una tarea ANTES de mandarla: aplica sus pasos de código tal cual (sacados del propio .md) a los
esqueletos, corre propios + ocultos y vuelve a dejar los esqueletos como estaban.

Uso: python3 docs/llm/verificar_tarea.py docs/llm/tareas/T31a-….md [--dejar]
En el .md: `## `firma` (pasos en orden)` seguido de líneas `N. `código``, y en `## Tests` los items
`- `nombreDelTest`:` con sub-items `  - `aserción``. Con --dejar no restaura los archivos (para inspeccionar).
"""
import pathlib
import re
import shutil
import subprocess
import sys

REPO = pathlib.Path(__file__).resolve().parents[2]


def fin_llave(t, i):
    n = 0
    while i < len(t):
        c = t[i]
        if t.startswith("//", i):
            i = t.find("\n", i)
            continue
        if c == '"':
            i += 1
            while t[i] != '"':
                i += 2 if t[i] == "\\" else 1
        elif c == "'":
            i += 1
            while t[i] != "'":
                i += 2 if t[i] == "\\" else 1
        elif c == "{":
            n += 1
        elif c == "}":
            n -= 1
            if n == 0:
                return i
        i += 1
    raise ValueError("llave sin cerrar")


def reemplazar_cuerpo(codigo, nombre, lineas, tid):
    # el método cuyo cuerpo tiene "// TODO <tid>"
    for m in re.finditer(r"\b" + re.escape(nombre) + r"\s*\([^)]*\)\s*(?:throws [\w., ]+)?\{", codigo):
        ini = m.end() - 1
        fin = fin_llave(codigo, ini)
        if f"TODO {tid}" in codigo[ini:fin]:
            cuerpo = "\n" + "\n".join("        " + l for l in lineas) + "\n    "
            return codigo[:ini + 1] + cuerpo + codigo[fin:]
    raise SystemExit(f"no encontré {nombre} con TODO {tid}")


def main():
    tarea = pathlib.Path(sys.argv[1])
    dejar = "--dejar" in sys.argv
    texto = tarea.read_text(encoding="utf-8")
    m = re.match(r"<!--\s*contrato:\s*([\d,\s]*)\|\s*archivos:\s*(.*?)\s*-->", texto)
    archivos = [a.strip() for a in m.group(2).split(",")]
    tid = tarea.name.split("-")[0]
    src = [a for a in archivos if "/src/main/" in a][0]
    tst = [a for a in archivos if "/src/test/" in a][0]
    originales = {a: (REPO / a).read_text(encoding="utf-8") for a in archivos}
    try:
        codigo = originales[src]
        # métodos: "## `metodo(...)` (pasos…)" + líneas "N. `código`"
        for sec in re.split(r"(?m)^(?=## )", texto):
            h = re.match(r"## `(?:[\w<>\[\], ?.]+\s+)?(\w+)\(.*?\)` \(pasos", sec)
            if h:
                lineas = re.findall(r"(?m)^\d+\. `(.*)`\s*$", sec)
                codigo = reemplazar_cuerpo(codigo, h.group(1), lineas, tid)
        (REPO / src).write_text(codigo, encoding="utf-8")
        # tests
        ct = originales[tst]
        sec = texto[texto.index("## Tests"):]
        for h in re.finditer(r"(?m)^- `(\w+)`:\s*\n((?:  - `.*`\s*\n?)+)", sec):
            lineas = re.findall(r"(?m)^  - `(.*)`\s*$", h.group(2))
            ct = reemplazar_cuerpo(ct, h.group(1), lineas, tid)
        (REPO / tst).write_text(ct, encoding="utf-8")
        r = subprocess.run([str(REPO / "docs/llm/evaluar-android.sh"), tid], cwd=REPO, text=True, capture_output=True)
        print(r.stdout, r.stderr)
    finally:
        if not dejar:
            for a, t in originales.items():
                (REPO / a).write_text(t, encoding="utf-8")


main()
