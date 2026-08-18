package com.obratrack.ui;

import com.obratrack.model.Usuario;
import com.obratrack.service.IUsuarioService;
import com.obratrack.service.ServiceFactory;
import com.obratrack.service.UsuarioService;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Dialogo modal que obliga a definir una nueva contrasena antes de entrar.
 * Se muestra cuando el usuario ingresa con una cuenta marcada como
 * "debe cambiar contrasena" (p. ej. el admin sembrado con la clave de fabrica).
 *
 * Devuelve {@code true} si la contrasena se cambio correctamente; {@code false}
 * si el usuario cancela (en cuyo caso el llamador debe cerrar la sesion).
 */
public final class CambioPasswordObligatorio {

    private static final int MIN_LONGITUD = 6;

    private CambioPasswordObligatorio() {}

    public static boolean mostrar(Usuario u) {
        IUsuarioService service = ServiceFactory.usuario();

        while (true) {
            JPasswordField nueva = campo();
            JPasswordField confirma = campo();

            JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
            panel.add(new JLabel("Por seguridad debes definir una nueva contrasena para '"
                    + u.getUsername() + "'."));
            panel.add(new JLabel("Nueva contrasena (minimo " + MIN_LONGITUD + " caracteres):"));
            panel.add(nueva);
            panel.add(new JLabel("Confirmar contrasena:"));
            panel.add(confirma);

            int opcion = JOptionPane.showConfirmDialog(null, panel,
                    "Cambio de contrasena obligatorio",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

            if (opcion != JOptionPane.OK_OPTION) {
                return false; // el usuario cancelo: el llamador cerrara la sesion
            }

            char[] a = nueva.getPassword();
            char[] b = confirma.getPassword();
            try {
                if (a.length < MIN_LONGITUD) {
                    error("La contrasena debe tener al menos " + MIN_LONGITUD + " caracteres.");
                    continue;
                }
                if (!Arrays.equals(a, b)) {
                    error("La confirmacion no coincide.");
                    continue;
                }
                if (Arrays.equals(a, UsuarioService.ADMIN_PASS_INICIAL.toCharArray())) {
                    error("No puedes reutilizar la contrasena de fabrica. Elige otra.");
                    continue;
                }
                service.cambiarPassword(u.getId(), a);
                u.setDebeCambiarPassword(false);
                JOptionPane.showMessageDialog(null, "Contrasena actualizada. Bienvenido.",
                        "Listo", JOptionPane.INFORMATION_MESSAGE);
                return true;
            } catch (Exception ex) {
                error("No se pudo cambiar la contrasena: " + ex.getMessage());
            } finally {
                Arrays.fill(a, '\0');
                Arrays.fill(b, '\0');
            }
        }
    }

    private static JPasswordField campo() {
        JPasswordField f = new JPasswordField(18);
        f.putClientProperty("JPasswordField.showRevealButton", true);
        return f;
    }

    private static void error(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Revisa los datos", JOptionPane.ERROR_MESSAGE);
    }
}
