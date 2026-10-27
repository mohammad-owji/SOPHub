package com.sophub.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DokumenttypTest {

    @Test
    void erkenntBekannteEingabeUnabhaengigVonGrossschreibung() {
        assertThat(Dokumenttyp.vonEingabe("lastenheft")).isEqualTo(Dokumenttyp.LASTENHEFT);
        assertThat(Dokumenttyp.vonEingabe("PFLICHTENHEFT")).isEqualTo(Dokumenttyp.PFLICHTENHEFT);
        assertThat(Dokumenttyp.vonEingabe(" Praesentation ")).isEqualTo(Dokumenttyp.PRAESENTATION);
    }

    @Test
    void unbekannteOderLeereEingabeWirdSonstiges() {
        assertThat(Dokumenttyp.vonEingabe("DOKUMENTATION")).isEqualTo(Dokumenttyp.SONSTIGES);
        assertThat(Dokumenttyp.vonEingabe(null)).isEqualTo(Dokumenttyp.SONSTIGES);
        assertThat(Dokumenttyp.vonEingabe("   ")).isEqualTo(Dokumenttyp.SONSTIGES);
    }

    @Test
    void dateinameHeuristikErkenntTypen() {
        assertThat(Dokumenttyp.ausDateiname("Lastenheft_v2.pdf")).isEqualTo(Dokumenttyp.LASTENHEFT);
        assertThat(Dokumenttyp.ausDateiname("gruppe3-pflichtenheft.docx")).isEqualTo(Dokumenttyp.PFLICHTENHEFT);
        assertThat(Dokumenttyp.ausDateiname("Abschlussbericht.pdf")).isEqualTo(Dokumenttyp.ABSCHLUSSBERICHT);
        assertThat(Dokumenttyp.ausDateiname("Folien_final.pdf")).isEqualTo(Dokumenttyp.PRAESENTATION);
        assertThat(Dokumenttyp.ausDateiname("notizen.txt")).isEqualTo(Dokumenttyp.SONSTIGES);
    }

    @Test
    void bestimmeBevorzugtEingabeSonstDateiname() {
        // Eingabe bekannt -> Eingabe gewinnt, Dateiname egal.
        assertThat(Dokumenttyp.bestimme("LASTENHEFT", "irgendwas.pdf")).isEqualTo(Dokumenttyp.LASTENHEFT);
        // Eingabe unbekannt/SONSTIGES -> Dateiname-Heuristik greift.
        assertThat(Dokumenttyp.bestimme("SONSTIGES", "pflichtenheft.pdf")).isEqualTo(Dokumenttyp.PFLICHTENHEFT);
        assertThat(Dokumenttyp.bestimme(null, "Abschluss.pdf")).isEqualTo(Dokumenttyp.ABSCHLUSSBERICHT);
        // Beides ohne Treffer -> SONSTIGES.
        assertThat(Dokumenttyp.bestimme("egal", "datei.pdf")).isEqualTo(Dokumenttyp.SONSTIGES);
    }
}
