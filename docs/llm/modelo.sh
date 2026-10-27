#!/bin/bash
# Uso: docs/llm/modelo.sh <qwen|gemma> python3 docs/llm/pedir.py ...
# Dos subagentes comparten la GPU: este script toma un candado, carga el modelo pedido si no es el que está
# cargado y recién ahí corre el comando. Así un subagente nunca descarga el modelo en medio del pedido del otro.
set -e
declare -A ID=([qwen]=qwen/qwen3.5-9b [gemma]=google/gemma-4-e4b-qat)
M=${ID[$1]:?modelo: qwen o gemma}; shift
LMS=~/.lmstudio/bin/lms
exec 9>"${XDG_RUNTIME_DIR:-/tmp}/lmstudio-rutineitor.lock"
flock 9
$LMS server status >/dev/null 2>&1 || $LMS server start >/dev/null
if ! $LMS ps 2>/dev/null | grep -q "$M"; then
  $LMS unload --all >/dev/null 2>&1 || true
  $LMS load "$M" --context-length 8192 --parallel 1 -y >/dev/null
fi
"$@"
