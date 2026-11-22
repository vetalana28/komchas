#!/usr/bin/env bash
# Собирает сборку через packwiz (https://packwiz.infra.link/) в папку pack/build.
# Перед запуском: установите packwiz и соберите мод (см. README, шаг 2).
set -u
cd "$(dirname "$0")"
rm -rf build && mkdir -p build && cd build

# Проверьте флаги командой: packwiz init --help
packwiz init --name "Комендантский час" --author "АВТОР" --version "0.1.0" \
  --mc-version "1.20.1" --modloader forge --forge-version "47.3.0"

: > ../missing.txt
while read -r slug; do
  case "$slug" in ""|\#*) continue ;; esac
  packwiz modrinth add "$slug" -y || echo "$slug" >> ../missing.txt
done < ../mods.txt

# Свой мод и файлы настройки
mkdir -p mods
cp ../../mod/build/libs/komchas-*.jar mods/ 2>/dev/null || echo "ВНИМАНИЕ: jar мода не найден, сначала выполните gradle build"
cp -r ../overrides/. . 2>/dev/null

packwiz refresh
packwiz modrinth export   # создаёт .mrpack для импорта в Modrinth App / Prism

if [ -s ../missing.txt ]; then
  echo "Не найдены на Modrinth (добавьте вручную или поищите на CurseForge):"
  cat ../missing.txt
fi
