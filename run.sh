#!/bin/sh
# LeetCode 热题 100 桌面应用 一键编译并运行（Linux/macOS）
cd "$(dirname "$0")"

if ! command -v java >/dev/null 2>&1; then
    echo "[错误] 未找到 java，请先安装 JDK 并加入 PATH。"
    exit 1
fi

mkdir -p out
find src -name '*.java' > out/sources.txt
javac -encoding UTF-8 -d out @out/sources.txt || { echo "[错误] 编译失败"; exit 1; }
rm out/sources.txt

java -cp out hot100.Hot100App
