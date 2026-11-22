# -*- coding: utf-8 -*-
"""Установщик «Комендантского часа» для TLauncher.

Что делает:
 1. Берёт из ВАШЕЙ папки Silence in Head общие моды (библиотеки, звук, картинка, оптимизация)
    и кладёт их в папку нового профиля. Монстры и меню чужого сюжета пропускаются.
 2. Кладёт туда наш мод (komchas-*.jar), готовый мир с картой и настройки.
Ничего не удаляет и не трогает папку Silence in Head.
"""
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PACK = os.path.dirname(HERE)


def ask(prompt):
    return input(prompt + ": ").strip().strip('"').strip("'")


def main():
    print("=== Установка сборки «Комендантский час» ===\n")
    src = ask("Папка распакованной сборки Silence in Head (где лежит папка mods)")
    if not src or not os.path.isdir(os.path.join(src, "mods")):
        print("Не нашёл папку mods внутри:", src)
        return 1
    dst = ask("Папка нового профиля TLauncher (куда ставим сборку)")
    if not dst:
        print("Путь не указан.")
        return 1
    os.makedirs(dst, exist_ok=True)

    # --- исключения ---
    excl = []
    with open(os.path.join(HERE, "exclude_mods.txt"), encoding="utf-8") as f:
        for line in f:
            line = line.strip().lower()
            if line and not line.startswith("#"):
                excl.append(line)

    # --- моды ---
    mods_dst = os.path.join(dst, "mods")
    os.makedirs(mods_dst, exist_ok=True)
    copied, skipped = 0, []
    for name in sorted(os.listdir(os.path.join(src, "mods"))):
        if not name.lower().endswith(".jar"):
            continue
        if any(e in name.lower() for e in excl):
            skipped.append(name)
            continue
        shutil.copy2(os.path.join(src, "mods", name), os.path.join(mods_dst, name))
        copied += 1
    print(f"\nМодов скопировано: {copied}")
    print("Пропущено (чужие монстры и оформление):")
    for n in skipped:
        print("   -", n)

    # --- наш мод ---
    own = [n for n in os.listdir(os.path.join(PACK, "mods"))
           if n.lower().startswith("komchas") and n.lower().endswith(".jar")]
    if own:
        for n in own:
            shutil.copy2(os.path.join(PACK, "mods", n), os.path.join(mods_dst, n))
        print("\nНаш мод:", ", ".join(own))
    else:
        print("\n!!! В папке mods нашей сборки нет komchas-*.jar. Соберите его (README, шаг 1)")
        print("    и положите в", mods_dst, "вручную.")

    # --- мир и настройки ---
    saves_src = os.path.join(PACK, "saves")
    if os.path.isdir(saves_src):
        shutil.copytree(saves_src, os.path.join(dst, "saves"), dirs_exist_ok=True)
        print("\nМир скопирован: saves/")
    opt_src = os.path.join(PACK, "options.txt")
    opt_dst = os.path.join(dst, "options.txt")
    if os.path.exists(opt_src) and not os.path.exists(opt_dst):
        shutil.copy2(opt_src, opt_dst)
        print("Настройки options.txt скопированы.")
    elif os.path.exists(opt_dst):
        print("options.txt уже есть, не заменяю (чтобы не потерять ваши настройки).")

    print("\nГотово. Запустите профиль в TLauncher (Minecraft 1.20.1, Forge 47.4.22)")
    print("и выберите мир «Комендантский час».")
    return 0


if __name__ == "__main__":
    code = main()
    input("\nНажмите Enter, чтобы закрыть...")
    sys.exit(code)
