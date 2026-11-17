#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out src/loja/*.java
java -cp out loja.Main
