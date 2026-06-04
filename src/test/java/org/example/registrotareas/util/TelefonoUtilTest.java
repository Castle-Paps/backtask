package org.example.registrotareas.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelefonoUtilTest {

    @Test
    void agregaPrefijoPeruANueveDigitos() {
        assertThat(TelefonoUtil.normalizarTelefonoPeru("987654321")).isEqualTo("51987654321");
    }

    @Test
    void mantieneNumeroYaNormalizado() {
        assertThat(TelefonoUtil.normalizarTelefonoPeru("51987654321")).isEqualTo("51987654321");
    }

    @Test
    void limpiaCaracteresNoNumericos() {
        assertThat(TelefonoUtil.normalizarTelefonoPeru("987-654-321")).isEqualTo("51987654321");
    }

    @Test
    void rechazaTelefonoVacio() {
        assertThatThrownBy(() -> TelefonoUtil.normalizarTelefonoPeru("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void numeroDeOtraLongitudSeDevuelveTalCual() {
        // TelefonoUtil es permisivo: números que no sean 9 o 11 dígitos se devuelven sin prefijo
        assertThat(TelefonoUtil.normalizarTelefonoPeru("12345")).isEqualTo("12345");
    }
}
