#!/bin/bash
# Vuelve a "// TODO Txx" los marcadores "lo completa otra subtarea" que pedir.py deja en los archivos.
sed -i 's#// lo completa otra subtarea (\(T[0-9a-z]*\)): no lo cambies#// TODO \1#' "$@"
