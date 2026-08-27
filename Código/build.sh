#!/bin/bash
# Compila el analizador léxico y arma out/interpretador.jar (REQ-MP-07).
# Uso:
#   ./build.sh
#   java -jar out/interpretador.jar <archivo-fuente>
set -e
cd "$(dirname "$0")"

rm -rf out/classes
mkdir -p out/classes

javac -d out/classes $(find src/Model src/Controller src/View -name "*.java")

jar --create --file out/interpretador.jar --main-class View.ModuloPrincipal -C out/classes .

echo "Listo: out/interpretador.jar"
