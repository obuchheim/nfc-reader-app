# README.md
# 🔍 NFC Tag Reader App

Eine native Android-App zum Erkennen und Auslesen von NFC-Tags mit umfassender Format-Unterstützung.

## ✨ Features

- 🚀 **Automatische NFC-Erkennung** - Sofortige Erkennung beim Annähern
- 📱 **Umfassende Format-Unterstützung** - NDEF, MIFARE, ISO14443, etc.
- 📄 **Detaillierte Informationen** - Tag-ID, Technologien, Inhalte
- 🎯 **Benutzerfreundlich** - Intuitive Oberfläche mit Dialogen
- 📲 **Vibrations-Feedback** - Bestätigung bei Tag-Erkennung

## 📋 Unterstützte NFC-Formate

- **NDEF** (Text, URI, MIME-Daten)
- **ISO14443-A/B** (NFC-A/B)
- **FeliCa** (NFC-F)
- **ISO15693** (NFC-V)  
- **ISO-DEP** (Kontaktlose Zahlung)
- **MIFARE Classic/Ultralight**

## 🚀 Automatische APK-Erstellung

Diese App wird automatisch mit GitHub Actions erstellt:

1. **Code-Änderung** → Automatischer Build
2. **Tag erstellen** → Release mit APK-Download
3. **Keine lokale Entwicklungsumgebung nötig**

## 📱 Installation

### Vom Release herunterladen:
1. Gehe zu [Releases](../../releases)
2. Lade `app-debug.apk` herunter
3. Aktiviere "Unbekannte Quellen" in Android-Einstellungen
4. Installiere die APK

### Voraussetzungen:
- Android 5.0+ (API 21)
- NFC-Hardware erforderlich
- Etwa 5 MB freier Speicher

## 🔧 Nutzung

1. **App starten**
2. **NFC aktivieren** (falls deaktiviert)
3. **NFC-Tag an das Gerät halten**
4. **Informationen im Dialog bestätigen**
5. **Detaillierte Ergebnisse anzeigen lassen**

## 🛠️ Entwicklung

Die App ist vollständig mit GitHub Actions automatisiert:

- **Push/PR** → Automatischer Build
- **Release-Tag** → APK-Download verfügbar
- **Keine lokale Einrichtung erforderlich**

## 📄 Lizenz

MIT License - Siehe [LICENSE](LICE
NSE) für Details.
