# 🚀 Groq API Migration Guide

## ✨ Was hat sich geändert?

**Alte Lösung:** Google Gemini API (kostenpflichtig, begrenzte kostenlose Nutzung)
**Neue Lösung:** Groq API (komplett kostenlos mit riesigem Kontingent!)

## 🎯 Vorteile von Groq:

✅ **Komplett kostenlos** - Keine Kreditkarte erforderlich
✅ **Großzügige Quote** - 10.000+ Anfragen pro Monat kostenlos (~330/Tag)
✅ **Super schnell** - Schneller als GPT-4o und Gemini
✅ **Open-Source Modelle** - Mixtral, Llama, etc.
✅ **Einfache API** - Kompatibel mit OpenAI API Standard

## 📋 Setup-Anleitung (5 Minuten)

### Schritt 1: Groq Account erstellen
1. Gehe zu https://console.groq.com/
2. Melde dich mit GitHub, Google oder Email an
3. Bestätige deine Email

### Schritt 2: API Key generieren
1. In der Groq Console: Gehe zu "API Keys"
2. Klick auf "Create API Key"
3. Kopiere den Key (sieht etwa so aus: `gsk_...`)

### Schritt 3: Key in deine App eintragen
1. **Option A - Lokale Entwicklung:**
   - Öffne die Datei `.env` in deinem Projektordner
   - Ersetze `your_groq_api_key_here` mit deinem echten Key
   ```
   GROQ_API_KEY=gsk_xxxxxxxxxxxxxxxxxxxxxxx
   ```

2. **Option B - In der App (Benutzer-freundlich):**
   - Öffne die Hello KI App
   - Gehe zu Einstellungen (⚙️ Zahnrad oben rechts)
   - Tippe deinen Groq API Key ins Feld "Groq API Key eintragen"
   - Der Key wird lokal auf deinem Telefon gespeichert

### Schritt 4: Testen
1. Schreib eine Nachricht in den Chat
2. Die KI sollte jetzt über Groq antworten
3. Keine Fehlermeldung? ✅ Du bist fertig!

## 💡 Häufige Fragen

**F: Kann ich den Key überall teilen?**
A: Nein! Halte deinen API Key privat. Nicht auf GitHub, nicht auf Fotos.

**F: Was passiert wenn ich über mein Limit gehe?**
A: Mit 10.000 Anfragen/Monat und nur 10-20 Chats/Tag bist du noch LANGE nicht am Limit. Aber falls doch: Die API wird einfach langsamer, nicht teurer.

**F: Warum funktioniert die KI manchmal ohne Key?**
A: Ohne Key nutzt die App den lokalen "0-Punkt Logik Modus" - das funktioniert auch offline!

**F: Kann ich immer noch Gemini nutzen?**
A: Ja, die alte Gemini-Unterstützung ist noch im Code (Fallback). Aber Groq ist jetzt das Default.

## 🔧 Technische Details

- **Endpoint:** `https://api.groq.com/openai/v1/chat/completions`
- **Standard-Modell:** `mixtral-8x7b-32768` (schnell & leistungsstark)
- **Alternative Modelle:** 
  - `llama-2-70b-chat` (großes Modell, langsamer)
  - `llama2-13b-chat` (kleineres Modell, schneller)

## 📞 Support

Probleme? Hier sind die nächsten Schritte:
1. Groq Status checken: https://status.groq.com/
2. API Key doppelt überprüfen (kein Leerzeichen am Anfang/Ende!)
3. Internetverbindung testen
4. App neu starten

---

**Viel Spaß mit deiner kostenlosen Super-KI! 🎉**
