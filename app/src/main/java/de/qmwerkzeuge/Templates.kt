package de.qmwerkzeuge

import java.util.Locale
import kotlin.math.min

/** Ein Abschnitt einer Vorlage. */
sealed interface Sec {
    val label: String
}

/** Einzeiliges Feld. */
data class LineSec(override val label: String) : Sec

/** Mehrzeiliges Textfeld. */
data class TextSec(override val label: String, val lines: Int = 3) : Sec

/** Reiner Hinweistext, nicht ausfüllbar. */
data class NoteSec(override val label: String) : Sec

/** Tabelle mit Spalten und Zeilen. prefill: Spaltenindex -> vorgegebene Werte je Zeile. */
data class TableSec(
    override val label: String,
    val cols: List<String>,
    val weights: List<Float>,
    val rows: Int,
    val prefill: Map<Int, List<String>> = emptyMap(),
) : Sec {
    /** Zeilen, die in der App zu Beginn angezeigt werden. */
    val appRows: Int get() = if (prefill.isNotEmpty()) rows else min(rows, 5)
}

data class Template(val sections: List<Sec>)

/** Kopfzeilen jeder Vorlage (Schlüssel h0 bis h3). */
val headerLabels = listOf("Thema / Gegenstand", "Bereich / Abteilung", "Erstellt von", "Datum")

private fun line(l: String) = LineSec(l)
private fun text(l: String, n: Int = 3) = TextSec(l, n)
private fun note(l: String) = NoteSec(l)

/** Spalten als "Name|Name:2|Name" – eine Zahl nach dem letzten ":" ist die relative Breite. */
private fun tbl(l: String, rows: Int, cols: String, prefill: Map<Int, List<String>> = emptyMap()): TableSec {
    val names = mutableListOf<String>()
    val weights = mutableListOf<Float>()
    for (p in cols.split("|")) {
        val w = p.substringAfterLast(":", "").toFloatOrNull()
        if (w != null) {
            names += p.substringBeforeLast(":")
            weights += w
        } else {
            names += p
            weights += 1f
        }
    }
    return TableSec(l, names, weights, rows, prefill)
}

private fun nums(n: Int) = (1..n).map { it.toString() }

private val templates: Map<String, Template> = mapOf(
    // ---------- Q7 ----------
    "fehlersammelliste" to Template(listOf(
        line("Prüfgegenstand"),
        line("Zeitraum"),
        tbl("Strichliste", 8, "Fehlerart:3|Mo|Di|Mi|Do|Fr|Sa|Summe:1.3"),
        text("Auffälligkeiten"),
    )),
    "histogramm" to Template(listOf(
        line("Merkmal und Einheit"),
        line("Toleranz (untere / obere Grenze)"),
        line("Anzahl Messwerte"),
        tbl("Klassen", 10, "Nr.:0.6|Klasse von|bis|Anzahl|Strichliste:2.5"),
        text("Beobachtung (Lage, Streuung, Ausreißer)"),
    )),
    "regelkarte" to Template(listOf(
        line("Merkmal und Einheit"),
        line("Mittellinie"),
        line("Obere Eingriffsgrenze"),
        line("Untere Eingriffsgrenze"),
        tbl("Stichproben", 20, "Nr.:0.6|Datum / Uhrzeit:1.6|Messwert / Mittelwert:1.4|Spannweite|Bemerkung:2"),
        text("Eingriffe und Maßnahmen"),
    )),
    "pareto" to Template(listOf(
        line("Fragestellung"),
        line("Zeitraum"),
        tbl("Ursachen (absteigend sortiert)", 10, "Rang:0.6|Ursache / Fehlerart:3|Anzahl|Anteil %|kumuliert %"),
        text("Schlussfolgerung: Welche Ursachen zuerst angehen?"),
    )),
    "korrelation" to Template(listOf(
        line("Größe X (Einheit)"),
        line("Größe Y (Einheit)"),
        tbl("Wertepaare", 20, "Nr.:0.6|Wert X|Wert Y|Bemerkung:2"),
        text("Ist ein Zusammenhang erkennbar? (steigend, fallend, keiner)"),
    )),
    "ishikawa" to Template(listOf(
        line("Problem (Fischkopf)"),
        text("Mensch", 3),
        text("Maschine", 3),
        text("Material", 3),
        text("Methode", 3),
        text("Milieu (Umwelt)", 3),
        text("Messung", 3),
        text("Wahrscheinlichste Ursache(n)", 2),
    )),
    "flussdiagramm" to Template(listOf(
        line("Prozess"),
        line("Startereignis"),
        line("Endergebnis"),
        tbl("Prozessschritte", 12, "Nr.:0.6|Schritt / Entscheidung:3|Verantwortlich:1.5|Weiter mit (ja / nein):1.6"),
        text("Schwachstellen im Ablauf"),
    )),
    // ---------- M7 ----------
    "affinitaet" to Template(listOf(
        line("Fragestellung"),
        tbl("Gruppen", 6, "Oberbegriff:1.2|Zugeordnete Ideen / Aussagen:3.5"),
        text("Erkenntnisse"),
    )),
    "relationen" to Template(listOf(
        line("Kernproblem"),
        tbl("Faktoren", 10, "Nr.:0.6|Faktor:3|beeinflusst Nr.:1.4|Pfeile aus|Pfeile ein"),
        text("Hauptursache(n) – meiste ausgehende Pfeile", 2),
        text("Hauptwirkung(en) – meiste eingehende Pfeile", 2),
    )),
    "baum" to Template(listOf(
        line("Oberziel"),
        tbl("Zerlegung", 10, "Teilziel:2|Maßnahme:2.5|Verantwortlich:1.3|Termin"),
    )),
    "matrix" to Template(listOf(
        line("Zeilen = Gruppe A (z. B. Kundenwünsche)"),
        line("Spalten = Gruppe B (z. B. Maßnahmen)"),
        tbl("Matrix", 6, "A \\ B:2|B1|B2|B3|B4|B5"),
        note("Zeichen: ● = starker, ○ = mittlerer, △ = schwacher Zusammenhang (oder 9 / 3 / 1)"),
        text("Erkenntnisse", 2),
    )),
    "portfolio" to Template(listOf(
        line("Kriterium X-Achse (z. B. Aufwand)"),
        line("Kriterium Y-Achse (z. B. Nutzen)"),
        tbl("Optionen", 10, "Option:2.5|Wert X (1–10)|Wert Y (1–10)|Feld / Entscheidung:2"),
        note("Felder: hoher Nutzen + geringer Aufwand = sofort umsetzen; hoher Nutzen + hoher Aufwand = planen; geringer Nutzen + geringer Aufwand = nebenbei; geringer Nutzen + hoher Aufwand = lassen."),
    )),
    "netzplan" to Template(listOf(
        line("Projekt"),
        tbl("Vorgänge", 12, "Nr.:0.6|Vorgang:2.5|Dauer|Vorgänger|Frühester Start|Puffer"),
        text("Kritischer Pfad und Gesamtdauer", 2),
    )),
    "pdpc" to Template(listOf(
        line("Ziel des Plans"),
        tbl("Schritte und Risiken", 8, "Schritt:1.8|Mögliches Problem:2|Gegenmaßnahme:2|Verantwortlich:1.2"),
    )),
    // ---------- Problemlösung ----------
    "pdca" to Template(listOf(
        line("Problem / Ziel"),
        text("Plan – Ursache, Ziel und geplante Maßnahme", 4),
        text("Do – Umsetzung im kleinen Rahmen", 3),
        text("Check – Ergebnis prüfen (vorher / nachher)", 3),
        text("Act – Als Standard übernehmen oder neuer Durchlauf", 3),
    )),
    "5why" to Template(listOf(
        line("Problem"),
        tbl("Warum-Kette", 5, "Frage:1|Antwort:4.5", mapOf(0 to listOf("1. Warum?", "2. Warum?", "3. Warum?", "4. Warum?", "5. Warum?"))),
        text("Grundursache", 2),
        text("Maßnahme gegen die Grundursache", 2),
    )),
    "8d" to Template(listOf(
        line("Kunde / Reklamationsnummer"),
        text("D1 – Team", 2),
        text("D2 – Problembeschreibung (was, wo, wann, wie viele)", 3),
        text("D3 – Sofortmaßnahmen", 3),
        text("D4 – Grundursache", 3),
        text("D5 – Abstellmaßnahmen festlegen", 3),
        text("D6 – Umsetzen und Wirksamkeit prüfen", 3),
        text("D7 – Wiederholung verhindern", 3),
        text("D8 – Abschluss und Würdigung des Teams", 2),
    )),
    "a3" to Template(listOf(
        text("1 Hintergrund", 3),
        text("2 Ist-Zustand (Zahlen, Fakten)", 4),
        text("3 Ziel", 2),
        text("4 Ursachenanalyse", 4),
        tbl("5 Maßnahmen", 5, "Maßnahme:3|Verantwortlich:1.3|Termin|Status"),
        text("6 Ergebnis und Nachverfolgung", 3),
    )),
    "kvp" to Template(listOf(
        text("Ist-Situation", 3),
        text("Verbesserungsidee", 3),
        text("Erwarteter Nutzen", 2),
        tbl("Umsetzung", 5, "Schritt:3|Verantwortlich:1.3|Termin|Erledigt"),
        text("Ergebnis", 2),
    )),
    "dmaic" to Template(listOf(
        text("Define – Problem, Ziel, Umfang", 3),
        text("Measure – Kennzahl und Ist-Wert", 3),
        text("Analyze – Hauptursachen", 3),
        text("Improve – Lösungen und Test", 3),
        text("Control – Absicherung des Ergebnisses", 3),
    )),
    // ---------- Vorbeugen ----------
    "fmea" to Template(listOf(
        line("System / Prozess"),
        tbl("FMEA", 10, "Fehler:2|Folge:2|Ursache:2|B:0.6|A:0.6|E:0.6|RPZ:0.8|Maßnahme:2.2|Verantw.:1.2"),
        note("B = Bedeutung, A = Auftreten, E = Entdeckung (je 1 bis 10). RPZ = B × A × E – wird beim Speichern automatisch berechnet, wenn leer."),
    )),
    "qfd" to Template(listOf(
        line("Produkt / Leistung"),
        tbl("Technische Merkmale", 5, "Nr.:0.6|Merkmal:3|Zielwert:1.5|Wichtigkeit:1.2", mapOf(0 to listOf("M1", "M2", "M3", "M4", "M5"))),
        tbl("Kundenwünsche × Merkmale", 6, "Kundenwunsch:2.6|Gewicht (1–5):1.2|M1|M2|M3|M4|M5"),
        note("Stärke des Zusammenhangs: 9 = stark, 3 = mittel, 1 = schwach. Wichtigkeit je Merkmal = Summe aus Gewicht × Stärke."),
    )),
    "kano" to Template(listOf(
        line("Produkt / Leistung"),
        tbl("Merkmale", 10, "Merkmal:2.4|Kategorie (Basis / Leistung / Begeisterung):2|Begründung:2.4"),
    )),
    "pokayoke" to Template(listOf(
        line("Arbeitsschritt"),
        text("Welcher Fehler passiert?", 2),
        text("Ursache", 2),
        text("Lösung – wie wird der Fehler unmöglich oder sofort sichtbar?", 3),
        line("Art (Verhindern / Warnen / Kontrollieren)"),
        text("Ergebnis", 2),
    )),
    "doe" to Template(listOf(
        line("Zielgröße (was soll besser werden?)"),
        tbl("Faktoren", 3, "Faktor:0.8|Bezeichnung:2|Stufe niedrig (-):1.4|Stufe hoch (+):1.4", mapOf(0 to listOf("A", "B", "C"))),
        tbl("Versuchsplan", 8, "Versuch|A|B|C|Ergebnis:2.2", mapOf(
            0 to nums(8),
            1 to listOf("-", "+", "-", "+", "-", "+", "-", "+"),
            2 to listOf("-", "-", "+", "+", "-", "-", "+", "+"),
            3 to listOf("-", "-", "-", "-", "+", "+", "+", "+"),
        )),
        text("Auswertung – welcher Faktor wirkt am stärksten?", 3),
    )),
    // ---------- Messen ----------
    "cpk" to Template(listOf(
        line("Merkmal und Einheit"),
        line("Untere Grenze (UG)"),
        line("Obere Grenze (OG)"),
        line("Mittelwert"),
        line("Standardabweichung s"),
        line("Cp = (OG − UG) / 6s"),
        line("Cpk = min(OG − Mittelwert, Mittelwert − UG) / 3s"),
        note("Cp und Cpk werden beim Speichern automatisch berechnet, wenn UG, OG, Mittelwert und s als Zahl eingetragen sind. Ab 1,33 gilt ein Prozess meist als fähig."),
        text("Bewertung und Maßnahmen", 3),
    )),
    "msa" to Template(listOf(
        line("Messmittel"),
        line("Merkmal und Toleranz"),
        tbl("Messwerte", 10, "Teil:0.7|Prüfer A 1|Prüfer A 2|Prüfer B 1|Prüfer B 2|Prüfer C 1|Prüfer C 2", mapOf(0 to nums(10))),
        line("%GRR (Anteil an der Toleranz)"),
        note("Unter 10 % = geeignet, 10 bis 30 % = bedingt geeignet, über 30 % = nicht geeignet."),
        text("Bewertung", 2),
    )),
    "5s" to Template(listOf(
        line("Bereich / Arbeitsplatz"),
        tbl("Bewertung (1 = schlecht bis 5 = sehr gut)", 5, "S:1.6|Prüfpunkt:2.6|Punkte:0.8|Bemerkung:2", mapOf(
            0 to listOf("Sortieren", "Systematisieren", "Säubern", "Standardisieren", "Selbstdisziplin"),
            1 to listOf(
                "Nur Nötiges am Platz?",
                "Fester, beschrifteter Platz?",
                "Sauber und gepflegt?",
                "Regeln sichtbar und einheitlich?",
                "Regeln werden eingehalten?",
            ),
        )),
        line("Gesamtpunkte (max. 25)"),
        text("Maßnahmen", 3),
    )),
    "sipoc" to Template(listOf(
        line("Prozess"),
        tbl("SIPOC", 6, "Lieferant (S)|Eingabe (I)|Prozessschritt (P)|Ergebnis (O)|Kunde (C)"),
    )),
    "wertstrom" to Template(listOf(
        line("Produkt / Produktfamilie"),
        line("Kundenbedarf pro Tag"),
        tbl("Prozessschritte", 8, "Nr.:0.6|Prozess:2|Zykluszeit|Rüstzeit|Bestand davor (Tage):1.3|Personen"),
        line("Durchlaufzeit gesamt"),
        line("Bearbeitungszeit gesamt"),
        text("Verbesserungsideen", 3),
    )),
    "audit" to Template(listOf(
        line("Auditor"),
        line("Auditierter Bereich / Person"),
        line("Norm / Grundlage (z. B. ISO 9001)"),
        tbl("Fragen", 12, "Nr.:0.6|Frage:3|Ergebnis (erfüllt / teilweise / Abweichung):1.8|Bemerkung:2"),
        text("Maßnahmen mit Termin", 3),
    )),
    "kpi" to Template(listOf(
        line("Zeitraum"),
        tbl("Kennzahlen", 8, "Kennzahl:2|Formel / Definition:2.2|Ziel|Ist|Trend|Maßnahme:2"),
    )),
    "benchmarking" to Template(listOf(
        line("Vergleichsgröße (Einheit)"),
        line("Unser Wert"),
        tbl("Vergleich", 6, "Vergleichspartner:2|Wert|Abstand zu uns|Was macht er anders?:3"),
        text("Übertragbare Ideen", 3),
    )),
)

fun templateFor(id: String): Template = templates[id] ?: Template(listOf(text("Notizen", 10)))

/** Schlüssel der gespeicherten Werte. */
fun cellKey(sec: Int, row: Int, col: Int) = "s${sec}_r${row}_c$col"
fun rowsKey(sec: Int) = "s${sec}_rows"
fun secKey(sec: Int) = "s$sec"

fun rowCount(values: Map<String, String>, sec: Int, t: TableSec): Int =
    values[rowsKey(sec)]?.toIntOrNull() ?: t.appRows

fun cellValue(values: Map<String, String>, sec: Int, row: Int, col: Int, t: TableSec): String =
    values[cellKey(sec, row, col)] ?: t.prefill[col]?.getOrNull(row) ?: ""

/** Rechnet abgeleitete Werte aus (nur leere Felder werden gefüllt). */
fun computeValues(id: String, t: Template, values: MutableMap<String, String>) {
    fun num(s: String?): Double? = s?.trim()?.replace(',', '.')?.toDoubleOrNull()
    fun fmt(d: Double) = String.format(Locale.GERMANY, "%.2f", d)
    when (id) {
        "fmea" -> {
            val i = t.sections.indexOfFirst { it is TableSec }
            if (i < 0) return
            val sec = t.sections[i] as TableSec
            for (r in 0 until rowCount(values, i, sec)) {
                val b = num(values[cellKey(i, r, 3)])
                val a = num(values[cellKey(i, r, 4)])
                val e = num(values[cellKey(i, r, 5)])
                val k = cellKey(i, r, 6)
                if (b != null && a != null && e != null && values[k].isNullOrBlank()) {
                    values[k] = (b * a * e).toInt().toString()
                }
            }
        }
        "cpk" -> {
            val ug = num(values[secKey(1)])
            val og = num(values[secKey(2)])
            val m = num(values[secKey(3)])
            val s = num(values[secKey(4)])
            if (ug != null && og != null && s != null && s > 0) {
                if (values[secKey(5)].isNullOrBlank()) values[secKey(5)] = fmt((og - ug) / (6 * s))
                if (m != null && values[secKey(6)].isNullOrBlank()) {
                    values[secKey(6)] = fmt(minOf(og - m, m - ug) / (3 * s))
                }
            }
        }
    }
}
