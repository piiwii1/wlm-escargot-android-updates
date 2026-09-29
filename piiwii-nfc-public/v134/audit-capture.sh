#!/usr/bin/env bash
set -euo pipefail

APK=/tmp/nfc134-audit/PiiWii-NFC-1.0.34/app/build/outputs/apk/debug/app-debug.apk
OUT=/tmp/nfc134-screens
mkdir -p "$OUT"

adb shell wm size 1080x2400
adb shell wm density 420
adb install -r "$APK"
adb shell am force-stop ch.piiwii.nfc.app
adb shell am start -W -n ch.piiwii.nfc.app/ch.piiwii.nfc.MainActivity
sleep 2

click_id() {
  local ID="$1"
  adb shell uiautomator dump /sdcard/window.xml >/dev/null
  adb pull /sdcard/window.xml /tmp/window.xml >/dev/null

  local NODE BOUNDS C X1 Y1 X2 Y2 X Y
  NODE=$(grep -o '<node[^>]*resource-id="[^"]*:id/'"$ID"'"[^>]*>' /tmp/window.xml | head -1 || true)
  if [[ -z "$NODE" ]]; then
    echo "ERROR: view id not found: $ID" >&2
    cp /tmp/window.xml "$OUT/failure-$ID.xml" || true
    adb exec-out screencap -p > "$OUT/failure-$ID.png" || true
    return 2
  fi

  BOUNDS=$(printf '%s' "$NODE" | sed -n 's/.*bounds="\([^"]*\)".*/\1/p')
  if [[ -z "$BOUNDS" ]]; then
    echo "ERROR: bounds not found for: $ID" >&2
    return 2
  fi

  C=$(printf '%s' "$BOUNDS" | sed -E 's/\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]/\1 \2 \3 \4/')
  read -r X1 Y1 X2 Y2 <<< "$C"
  X=$(( (X1 + X2) / 2 ))
  Y=$(( (Y1 + Y2) / 2 ))
  echo "CLICK $ID @ $X,$Y ($BOUNDS)"
  adb shell input tap "$X" "$Y"
  sleep 1
}

shot() {
  local NAME="$1"
  echo "SHOT $NAME"
  adb exec-out screencap -p > "$OUT/$NAME.png"
}

shot 01-home
click_id readButton
shot 02-read
click_id readBackButton
click_id writeButton
shot 03-write
click_id writeBackButton
click_id copyButton
shot 04-copy
click_id copyBackButton
click_id eraseButton
shot 05-erase
click_id eraseBackButton
click_id modelsShortcutButton
shot 06-models
click_id modelsBackButton
click_id headerSettingsButton
shot 10-settings
click_id settingsDiagnosticsPageButton
shot 07-diagnostics
click_id diagnosticsBackButton
click_id settingsBackupPageButton
shot 08-backup
click_id backupBackButton
click_id navHistory
shot 09-history

adb shell uiautomator dump /sdcard/window.xml >/dev/null || true
adb pull /sdcard/window.xml "$OUT/last-window.xml" >/dev/null || true
ls -lh "$OUT"
