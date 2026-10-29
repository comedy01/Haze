package dev.haze.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FogTest {
    private static final long SEED = 0x48415A45L;

    private static Fog.Conditions at(long time, float rain, float wetness, float humidity, float frequency) {
        return new Fog.Conditions(time, SEED, rain, wetness, humidity, false, frequency, true, true, false, false);
    }

    private static long foggyDawn(float humidity) {
        for (int day = 0; day < 64; day++) {
            long time = 23500L + day * Fog.DAY;
            if (Fog.layer(at(time, 0F, 0F, humidity, 2F)).density() > 0F) {
                return time;
            }
        }
        throw new AssertionError("no foggy dawn in 64 days");
    }

    @Test
    void hourStartsAtSixInTheMorning() {
        assertEquals(6F, Fog.hour(0L));
        assertEquals(12F, Fog.hour(6000L));
        assertEquals(0F, Fog.hour(18000L));
    }

    @Test
    void dryBiomesNeverFog() {
        for (long t = 0; t < 4 * Fog.DAY; t += 500) {
            assertEquals(Fog.Layer.NONE, Fog.layer(at(t, 1F, 1F, 0F, 2F)));
        }
    }

    @Test
    void clearNoonsAreClear() {
        for (int day = 0; day < 64; day++) {
            assertEquals(0F, Fog.layer(at(6000L + day * Fog.DAY, 0F, 0F, 0.8F, 2F)).density());
        }
    }

    @Test
    void foggyMorningsPeakAtSunriseAndBurnOff() {
        long dawn = foggyDawn(0.8F);
        Fog.Layer sunrise = Fog.layer(at(dawn, 0F, 0F, 0.8F, 2F));
        Fog.Layer late = Fog.layer(at(dawn + 4500L, 0F, 0F, 0.8F, 2F));
        assertTrue(sunrise.density() > 0.2F);
        assertTrue(sunrise.warmth() > 0F);
        assertTrue(late.density() < sunrise.density());
    }

    @Test
    void foggyMorningsGetMoreCommonWithHumidity() {
        assertTrue(Fog.chance(0.9F, 0F, 1F) > Fog.chance(0.3F, 0F, 1F));
        assertTrue(Fog.chance(0.5F, 1F, 1F) > Fog.chance(0.5F, 0F, 1F));
        assertEquals(0F, Fog.chance(1F, 1F, 0F));
        assertTrue(Fog.chance(1F, 1F, 2F) <= 0.95F);
    }

    @Test
    void dewNightsFogMoreOften() {
        assertTrue(Fog.chance(0.5F, 0F, 1F, true) > Fog.chance(0.5F, 0F, 1F, false) + 0.3F);
        assertEquals(0F, Fog.chance(0.5F, 0F, 0F, true));
        int plain = 0;
        int dewy = 0;
        for (int day = 0; day < 200; day++) {
            long time = 23500L + day * Fog.DAY;
            if (Fog.layer(new Fog.Conditions(time, SEED, 0F, 0F, 0.5F, false, 1F, true, false, false, false)).density() > 0F) {
                plain++;
            }
            if (Fog.layer(new Fog.Conditions(time, SEED, 0F, 0F, 0.5F, false, 1F, true, false, true, false)).density() > 0F) {
                dewy++;
            }
        }
        assertTrue(dewy > plain + 40);
    }

    @Test
    void frequencyZeroMeansNoMorningFog() {
        for (int day = 0; day < 64; day++) {
            assertEquals(0F, Fog.layer(at(23500L + day * Fog.DAY, 0F, 0F, 0.8F, 0F)).density());
        }
    }

    @Test
    void rainBringsATallMist() {
        Fog.Layer rain = Fog.layer(at(6000L, 1F, 0.5F, 0.8F, 0F));
        assertTrue(rain.density() > 0.2F);
        assertTrue(rain.scale() > 2F);
        assertEquals(0F, rain.warmth());
    }

    @Test
    void mistLingersAfterRain() {
        assertTrue(Fog.layer(at(18000L, 0F, 0.8F, 0.8F, 0F)).density() > 0.05F);
    }

    @Test
    void sunBurnsOffFogEvenWhenSoaked() {
        assertTrue(Fog.layer(at(6000L, 0F, 1F, 0.8F, 0F)).density() < 0.01F);
        long dawn = foggyDawn(0.8F);
        assertEquals(0F, Fog.layer(at(dawn + 6500L, 0F, 1F, 0.8F, 2F)).density());
    }

    @Test
    void sunBurnsMistOffWithinHoursAfterRain() {
        float w = 1F;
        for (int i = 0; i < 3600; i++) {
            w = Fog.wetness(w, 0F, false);
        }
        assertTrue(Fog.layer(at(6000L, 0F, w, 0.8F, 0F)).density() < 0.01F);
    }

    @Test
    void swampsStayMistyAndThickenAtNight() {
        float noon = Swamp.density(Fog.hour(6000L), false);
        float night = Swamp.density(Fog.hour(18000L), false);
        assertTrue(noon > 0.2F);
        assertTrue(night > noon);
        assertTrue(Swamp.density(Fog.hour(6000L), true) < noon);
    }

    @Test
    void swampGridSnapsToTheWorld() {
        assertEquals(-768, Swamp.origin(0.5));
        assertEquals(-768, Swamp.origin(300));
        assertEquals(-1280, Swamp.origin(-0.5));
        assertEquals(-760, Swamp.center(-768, 0));
    }

    @Test
    void coldThinsTheFog() {
        Fog.Conditions warm = at(6000L, 1F, 0F, 0.8F, 0F);
        Fog.Conditions cold = new Fog.Conditions(6000L, SEED, 1F, 0F, 0.8F, true, 0F, true, true, false, false);
        assertEquals(Fog.layer(warm).density() * 0.6F, Fog.layer(cold).density(), 1.0E-5F);
    }

    @Test
    void togglesTurnSourcesOff() {
        Fog.Conditions off = new Fog.Conditions(18000L, SEED, 1F, 1F, 0.95F, false, 2F, false, false, false, false);
        assertEquals(Fog.Layer.NONE, Fog.layer(off));
    }

    @Test
    void skyClearsADayAfterRain() {
        float w = 1F;
        for (int i = 0; i < 24000; i++) {
            w = Fog.wetness(w, 0F, false);
        }
        assertTrue(Fog.layer(at(6000L, 0F, w, 0.8F, 0F)).density() < 0.02F);
    }

    @Test
    void wetnessRisesInRainAndDriesSlowly() {
        assertTrue(Fog.wetness(0F, 1F, true) > 0F);
        assertEquals(0F, Fog.wetness(0F, 1F, false));
        float w = 1F;
        for (int i = 0; i < 18000; i++) {
            w = Fog.wetness(w, 0F, false);
        }
        assertEquals(0.5F, w, 0.01F);
    }

    @Test
    void sunWarmthOnlyAroundSunrise() {
        assertEquals(0F, Fog.sunWarmth(3F));
        assertTrue(Fog.sunWarmth(6.5F) > 0.9F);
        assertEquals(0F, Fog.sunWarmth(12F));
    }

    @Test
    void approachClampsItsRate() {
        assertEquals(1F, Fog.approach(0F, 1F, 5F));
        assertEquals(0.5F, Fog.approach(0F, 1F, 0.5F));
    }

    @Test
    void alwaysFogHoldsAllDayEverywhere() {
        for (long time = 0L; time < Fog.DAY; time += 500L) {
            Fog.Layer dry = Fog.layer(new Fog.Conditions(time, SEED, 0F, 0F, 0F, false, 0F, false, false, false, true));
            assertTrue(dry.density() >= Fog.ALWAYS - 0.001F);
            Fog.Layer humid = Fog.layer(new Fog.Conditions(time, SEED, 0F, 0F, 0.8F, false, 1F, true, true, false, true));
            assertTrue(humid.density() >= Fog.ALWAYS - 0.001F);
        }
        assertEquals(0F, Fog.layer(new Fog.Conditions(6000L, SEED, 0F, 0F, 0F, false, 1F, true, true, false, false)).density());
    }
}
