#!/bin/bash
# Uso: docs/llm/evaluar.sh T1x   → compila, corre los tests propios, compara firmas y corre los ocultos de T1x.
cd "$(dirname "$0")/../.." || exit 1
T=$1
FIRMAS=docs/llm/evaluacion/firmas
EV=docs/llm/evaluacion/java/com/rutina
echo "== tests propios (todos)"
mvn -B -q -o test > /tmp/evaluar-rutina-pc-$USER.log 2>&1; echo "mvn test: exit $?"
grep -E "Tests run:|COMPILATION ERROR|ERROR\] /|expected:|but was|Exception:|\.java:[0-9]+\)" /tmp/evaluar-rutina-pc-$USER.log | grep -v "^\s*at org\.\|^\s*at java\." | head -30
echo "== firmas"
for f in "$FIRMAS"/*.txt; do
  c=$(basename "$f" .txt)
  javap -p -cp target/classes "$c" 2>/dev/null | grep -vF 'lambda$' | diff - <(grep -vF 'lambda$' "$f") > /dev/null || echo "CAMBIÓ: $c"
done
echo "== ocultos $T"
cp $EV/registro/${T}[A-Z]*OcultoTest.java src/test/java/com/rutina/registro/ 2>/dev/null
cp $EV/service/calculo/${T}[A-Z]*OcultoTest.java src/test/java/com/rutina/service/calculo/ 2>/dev/null
rm -f target/surefire-reports/*${T}*Oculto*
mvn -B -o test -Dtest="${T}*OcultoTest" -Dsurefire.failIfNoSpecifiedTests=false > /tmp/evaluar-rutina-pc-oculto-$USER.log 2>&1
grep -E "Tests run:.*Oculto|COMPILATION ERROR|ERROR\] /" /tmp/evaluar-rutina-pc-oculto-$USER.log | head -10
grep -h -E "^Tests run|<<<|expected" target/surefire-reports/*${T}*OcultoTest.txt 2>/dev/null | head -20
rm -f src/test/java/com/rutina/registro/T1*OcultoTest.java src/test/java/com/rutina/service/calculo/T1*OcultoTest.java
