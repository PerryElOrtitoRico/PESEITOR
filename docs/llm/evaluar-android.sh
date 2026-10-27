#!/bin/bash
# Uso: docs/llm/evaluar-android.sh T31a   → compila, corre los tests propios, compara firmas y corre los ocultos de T31a.
# Con GENERAR_FIRMAS=1 guarda las firmas actuales como referencia (hacerlo con el esqueleto).
cd "$(dirname "$0")/../.." || exit 1
T=$1
AND=android
OCULTOS=docs/llm/evaluacion/android
FIRMAS=docs/llm/evaluacion/firmas-android
DESTINO=$AND/app/src/test/java/com/rutina/app
LOG=/tmp/evaluar-rutina-android-$USER.log
CLASES=$(ls -d $AND/app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes 2>/dev/null | head -1)

if [ -n "$GENERAR_FIRMAS" ]; then
  (cd $AND && ./gradlew -q compileDebugJavaWithJavac >/dev/null 2>&1)
  mkdir -p $FIRMAS
  for f in $(cd $CLASES && find com/rutina/app -name '*.class' | grep -v '\$'); do
    c=$(echo ${f%.class} | tr / .)
    javap -p -cp $CLASES "$c" | grep -vF 'lambda$' > "$FIRMAS/$c.txt"
  done
  echo "firmas guardadas en $FIRMAS"; exit 0
fi

echo "== tests propios (todos)"
(cd $AND && ./gradlew testDebugUnitTest > $LOG 2>&1); echo "gradle test: exit $?"
grep -E "COMPILATION|error:|FAILED|expected:|but was|Exception" $LOG | head -30
echo "== firmas"
for f in $FIRMAS/*.txt; do
  [ -e "$f" ] || continue
  c=$(basename "$f" .txt)
  javap -p -cp $CLASES "$c" 2>/dev/null | grep -vF 'lambda$' | diff -q - "$f" >/dev/null || echo "CAMBIÓ: $c"
done
echo "== ocultos $T"
mkdir -p $DESTINO/logica $DESTINO/datos
for f in $OCULTOS/${T}*OcultoTest.java; do
  pkg=$(grep -m1 '^package' $f | sed 's/package com.rutina.app.\(.*\);/\1/')
  cp $f $DESTINO/$pkg/
done
(cd $AND && ./gradlew testDebugUnitTest --tests "*${T}*OcultoTest" > $LOG 2>&1); echo "ocultos: exit $?"
grep -E "COMPILATION|error:|FAILED|expected:|but was|Exception|AssertionFailed" $LOG | head -20
python3 - "$AND" "$T" <<'PY'
import glob, re, sys
tot = fal = 0
for f in glob.glob(f"{sys.argv[1]}/app/build/test-results/testDebugUnitTest/*{sys.argv[2]}*Oculto*.xml"):
    m = re.search(r'tests="(\d+)" skipped="\d+" failures="(\d+)" errors="(\d+)"', open(f, encoding="utf-8").read())
    tot += int(m.group(1)); fal += int(m.group(2)) + int(m.group(3))
print(f"ocultos {sys.argv[2]}: {tot - fal}/{tot}")
PY
rm -f $DESTINO/*/${T}*OcultoTest.java
