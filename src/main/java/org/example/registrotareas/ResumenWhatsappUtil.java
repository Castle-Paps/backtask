package org.example.registrotareas;

import org.example.registrotareas.Entidades.Tarea;

public class ResumenWhatsappUtil {

    private ResumenWhatsappUtil() {
    }

    public static String generarResumen(Tarea tarea) {
        return """
                Nueva tarea registrada

                Motivo: %s
                Monto: S/ %s
                """.formatted(
                tarea.getMotivo(),
                tarea.getTotal()
        );
    }
}
