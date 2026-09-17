#!/usr/bin/env bash
set -euo pipefail
DEST="${1:?destination required}"
mkdir -p "$DEST"
ROOT="$DEST/piiwii-remote"
TMP="$DEST/.patches"
mkdir -p "$TMP"

cat piiwii-remote-v2-lot4/source/part_*.b64 | base64 -d > "$TMP/lot4.tar.xz"
tar -xJf "$TMP/lot4.tar.xz" -C "$DEST"
cp piiwii-remote-v2-lot4/overlay/MainActivity.kt "$ROOT/remote/src/main/java/ch/piiwii/remote2/ui/MainActivity.kt"

base64 -d piiwii-remote-v2-lot5/lot5.patch.xz.b64 | xz -dc > "$TMP/lot5.patch"
(cd "$ROOT" && patch -p2 < "$TMP/lot5.patch")
cat piiwii-remote-v2-lot6/patch/part_*.b64 | base64 -d | xz -dc > "$TMP/lot6.patch"
(cd "$ROOT" && patch -p2 < "$TMP/lot6.patch")
python - "$ROOT" <<'PY'
from pathlib import Path
import sys
p=Path(sys.argv[1])/'remote/src/main/java/ch/piiwii/remote2/ui/screens/SectionScreenFactory.kt'
s=p.read_text(encoding='utf-8')
old='ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT)'
new='android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)'
if old in s: p.write_text(s.replace(old,new),encoding='utf-8')
PY
base64 -d piiwii-remote-v2-lot7/lot7.patch.xz.b64 | xz -dc > "$TMP/lot7.patch"
(cd "$ROOT" && patch -p2 < "$TMP/lot7.patch")
base64 -d piiwii-remote-v2-lot8/lot8.patch.xz.b64 | xz -dc > "$TMP/lot8.patch"
(cd "$ROOT" && patch -p2 < "$TMP/lot8.patch")
base64 -d piiwii-remote-v2-lot9/lot9.patch.xz.b64 | xz -dc > "$TMP/lot9.patch"
(cd "$ROOT" && patch -p1 < "$TMP/lot9.patch")
base64 -d piiwii-remote-v2-lot10/lot10.patch.xz.b64 | xz -dc > "$TMP/lot10.patch"
(cd "$ROOT" && patch -p2 < "$TMP/lot10.patch")
base64 -d piiwii-remote-v2-lot11/lot11.patch.xz.b64 | xz -dc > "$TMP/lot11.patch"
(cd "$ROOT" && patch -p2 < "$TMP/lot11.patch")

cat piiwii-remote-v2-lot12/patch/part_*.b64 | tr -d '\r\n' | base64 -d > "$TMP/lot12.patch.xz"
echo "e4581fc1311caafba80e4a2d48f50c2aab5a7979039049d050a9b756b11b2b50  $TMP/lot12.patch.xz" | sha256sum -c -
xz -dc "$TMP/lot12.patch.xz" > "$TMP/lot12.patch"
echo "86668589bbd0de476f94afc1e02a526ab5e2be2b3a0bfb089a7ab683bdf35f93  $TMP/lot12.patch" | sha256sum -c -
(cd "$ROOT" && patch -p1 < "$TMP/lot12.patch")

printf '%s\n' "$ROOT"
