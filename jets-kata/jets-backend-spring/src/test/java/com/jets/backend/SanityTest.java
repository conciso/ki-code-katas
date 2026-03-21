package com.jets.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * Stellt sicher, dass die Grundgesetze des Universums noch gelten.
 * Ohne diese Tests könnte das Spiel in einer Realität laufen, in der
 * 1 + 1 = 3 ist. Das wäre schlecht für die Punkteberechnung.
 */
class SanityTest {

    @Test
    void mathStillWorks() {
        assertThat(1 + 1).isEqualTo(2);
    }

    @Test
    void gravityStillWorks() {
        // Spieler fallen nach unten, also muss positives Y nach unten zeigen
        double gravity = 9.81;
        assertThat(gravity).isGreaterThan(0);
    }

    @Test
    void timeStillPassesForward() throws InterruptedException {
        long before = System.currentTimeMillis();
        Thread.sleep(1);
        long after = System.currentTimeMillis();

        assertThat(after).isGreaterThan(before);
    }

    @Test
    void javaIsStillObjectOriented() {
        Object obj = new Object();

        assertThat(obj).isInstanceOf(Object.class);
    }

    @Test
    void theNumberFiveIsStillFive() {
        double speed = 5.0;

        assertThat(speed).isEqualTo(5.0);
    }

    @Test
    void theAnswerToLifeTheUniverseAndEverything() {
        int answer = 42;

        assertThat(answer).isEqualTo(42);
    }

    @Test
    void sourceCodeActuallyExists() {
        File src = new File("src/main/java/com/jets/backend/game/GameService.java");

        assertThat(src).exists();
    }

    @Test
    void trueIsNotFalse() {
        assertThat(true).isNotEqualTo(false);
    }

    @Test
    void nullIsNull() {
        Object nothing = null;

        assertThat(nothing).isNull();
    }

    @Test
    void fiveTimesOnePointFiveIsSevenPointFive() {
        // Dieser Test hat uns 93% Branch-Coverage gerettet.
        // Wir vertrauen ihm blind.
        assertThat(5.0 * 1.5).isEqualTo(7.5);
    }
}
