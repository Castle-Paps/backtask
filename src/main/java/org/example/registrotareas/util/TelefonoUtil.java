package org.example.registrotareas.util;

public class TelefonoUtil {

    private TelefonoUtil() {
    }

    /**
     * Normaliza un teléfono al formato internacional con prefijo de Perú (51).
     * Acepta:
     *   - 9 dígitos  → agrega "51" al inicio
     *   - 11 dígitos empezando con "51" → lo devuelve tal cual
     *   - Otro formato → devuelve solo los dígitos (sin prefijo forzado)
     */
    public static String normalizarTelefonoPeru(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            throw new IllegalArgumentException("El teléfono no puede estar vacío.");
        }

        String soloNumeros = telefono.replaceAll("\\D", "");

        if (soloNumeros.isEmpty()) {
            throw new IllegalArgumentException("El teléfono debe contener al menos un dígito.");
        }

        // Teléfono peruano de 9 dígitos → agregar código de país
        if (soloNumeros.length() == 9) {
            return "51" + soloNumeros;
        }

        // Ya tiene código de país Perú (51 + 9 dígitos = 11 total)
        if (soloNumeros.length() == 11 && soloNumeros.startsWith("51")) {
            return soloNumeros;
        }

        // Cualquier otro formato: devolver solo dígitos tal como están
        // (para números internacionales u otros formatos)
        return soloNumeros;
    }
}
