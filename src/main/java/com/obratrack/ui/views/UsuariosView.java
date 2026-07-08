package com.obratrack.ui.views;

import com.obratrack.model.Usuario;
import com.obratrack.service.UsuarioService;
import com.obratrack.ui.Icons;
import com.obratrack.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Gestion de usuarios (solo ADMIN): crear, editar, activar/desactivar y resetear contrasena. */
public class UsuariosView extends JPanel {

    private final UsuarioService usuarioService = new UsuarioService();

    private final JTextField campoUsername = new JTextField();
    private final JTextField campoNombre = new JTextField();
    private final JComboBox<Usuario.Rol> comboRol = new JComboBox<>(Usuario.Rol.values());
    private final JPasswordField campoPassword = new JPasswordField();
    private final JLabel labelMensaje = new JLabel(" ");

    private final List<Usuario> usuariosActuales = new ArrayList<>();
    private JTable tabla;
    private DefaultTableModel tablaModelo;

    private static final String[] COLS = {"Usuario", "Nombre", "Rol", "Activo"};

    public UsuariosView() {
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG_PRIMARY);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titulo = new JLabel("Usuarios");
        titulo.setFont(Theme.FONT_TITLE);
        titulo.setForeground(Theme.TEXT_PRIMARY);
        add(titulo, BorderLayout.NORTH);

        JPanel centro = new JPanel(new GridLayout(1, 2, 20, 0));
        centro.setOpaque(false);
        centro.add(construirFormulario());
        centro.add(construirListado());
        add(centro, BorderLayout.CENTER);
    }

    private JPanel construirFormulario() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BG_SECONDARY);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        panel.add(seccion("Nuevo usuario"));
        panel.add(Box.createVerticalStrut(12));

        panel.add(etiqueta("Usuario *"));
        estilizar(campoUsername);
        panel.add(campoUsername);
        panel.add(Box.createVerticalStrut(10));

        panel.add(etiqueta("Nombre completo"));
        estilizar(campoNombre);
        panel.add(campoNombre);
        panel.add(Box.createVerticalStrut(10));

        panel.add(etiqueta("Rol"));
        comboRol.setFont(Theme.FONT_BASE);
        comboRol.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        comboRol.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(comboRol);
        panel.add(Box.createVerticalStrut(10));

        panel.add(etiqueta("Contrasena *"));
        campoPassword.setFont(Theme.FONT_BASE);
        campoPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        campoPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        campoPassword.putClientProperty("JPasswordField.showRevealButton", true);
        panel.add(campoPassword);
        panel.add(Box.createVerticalStrut(16));

        labelMensaje.setFont(Theme.FONT_BASE);
        labelMensaje.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(labelMensaje);
        panel.add(Box.createVerticalStrut(6));

        JButton btnCrear = new JButton("Crear usuario", Icons.get("add", 16, Color.WHITE));
        btnCrear.setIconTextGap(8);
        btnCrear.setFont(Theme.FONT_BOLD);
        btnCrear.setBackground(Theme.PRIMARY);
        btnCrear.setForeground(Color.WHITE);
        btnCrear.setFocusPainted(false);
        btnCrear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCrear.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btnCrear.addActionListener(e -> crearUsuario());
        panel.add(btnCrear);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel construirListado() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Theme.BG_SECONDARY);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.add(seccion("Usuarios registrados"), BorderLayout.NORTH);

        tablaModelo = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tabla = new JTable(tablaModelo);
        tabla.setFont(Theme.FONT_BASE);
        tabla.setRowHeight(26);
        tabla.getTableHeader().setFont(Theme.FONT_BOLD);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barra.setOpaque(false);
        JButton btnEditar = new JButton("Editar", Icons.get("settings", 15, Theme.TEXT_SECONDARY));
        btnEditar.setIconTextGap(6);
        estilizarSec(btnEditar);
        btnEditar.addActionListener(e -> editarSeleccionado());
        JButton btnActivo = new JButton("Activar/Desactivar", Icons.get("account", 15, Theme.PRIMARY));
        btnActivo.setIconTextGap(6);
        estilizarSec(btnActivo);
        btnActivo.addActionListener(e -> alternarActivo());
        JButton btnReset = new JButton("Resetear contrasena", Icons.get("lock", 15, Theme.WARNING));
        btnReset.setIconTextGap(6);
        estilizarSec(btnReset);
        btnReset.addActionListener(e -> resetearPassword());
        barra.add(btnEditar);
        barra.add(btnActivo);
        barra.add(btnReset);
        panel.add(barra, BorderLayout.SOUTH);

        return panel;
    }

    private void crearUsuario() {
        String username = campoUsername.getText().trim();
        String nombre = campoNombre.getText().trim();
        char[] pass = campoPassword.getPassword();
        Usuario.Rol rol = (Usuario.Rol) comboRol.getSelectedItem();
        if (username.isEmpty()) { mostrar("El usuario es obligatorio.", Theme.DANGER); return; }
        if (pass.length == 0) { mostrar("La contrasena es obligatoria.", Theme.DANGER); return; }
        try {
            usuarioService.crear(username, nombre, pass, rol);
            java.util.Arrays.fill(pass, '\0');
            mostrar("Usuario '" + username + "' creado.", Theme.SUCCESS);
            campoUsername.setText("");
            campoNombre.setText("");
            campoPassword.setText("");
            comboRol.setSelectedIndex(0);
            refrescar();
        } catch (IllegalArgumentException e) {
            mostrar(e.getMessage(), Theme.DANGER);
        } catch (SQLException e) {
            mostrar("No se pudo crear: " + e.getMessage(), Theme.DANGER);
        }
    }

    private Usuario usuarioSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= usuariosActuales.size()) return null;
        return usuariosActuales.get(fila);
    }

    private void editarSeleccionado() {
        Usuario u = usuarioSeleccionado();
        if (u == null) { mostrar("Selecciona un usuario para editar.", Theme.WARNING); return; }

        JTextField nombre = new JTextField(u.getNombre() != null ? u.getNombre() : "");
        JComboBox<Usuario.Rol> rol = new JComboBox<>(Usuario.Rol.values());
        rol.setSelectedItem(u.getRol());
        JCheckBox activo = new JCheckBox("Usuario activo", u.isActivo());

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.add(new JLabel("Usuario: " + u.getUsername()));
        form.add(new JLabel("Nombre completo"));
        form.add(nombre);
        form.add(new JLabel("Rol"));
        form.add(rol);
        form.add(activo);

        int op = JOptionPane.showConfirmDialog(this, form, "Editar usuario",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;

        Usuario.Rol nuevoRol = (Usuario.Rol) rol.getSelectedItem();
        boolean nuevoActivo = activo.isSelected();
        // No dejar el sistema sin ningun admin activo
        if (u.getRol() == Usuario.Rol.ADMIN && u.isActivo()
                && (nuevoRol != Usuario.Rol.ADMIN || !nuevoActivo) && esUltimoAdmin()) {
            mostrar("No puedes quitar el ultimo administrador activo.", Theme.DANGER);
            return;
        }
        u.setNombre(nombre.getText().trim());
        u.setRol(nuevoRol);
        u.setActivo(nuevoActivo);
        try {
            usuarioService.actualizar(u);
            mostrar("Usuario actualizado.", Theme.SUCCESS);
            refrescar();
        } catch (SQLException e) {
            mostrar("No se pudo actualizar: " + e.getMessage(), Theme.DANGER);
        }
    }

    private void alternarActivo() {
        Usuario u = usuarioSeleccionado();
        if (u == null) { mostrar("Selecciona un usuario.", Theme.WARNING); return; }
        if (u.isActivo() && u.getRol() == Usuario.Rol.ADMIN && esUltimoAdmin()) {
            mostrar("No puedes desactivar el ultimo administrador activo.", Theme.DANGER);
            return;
        }
        u.setActivo(!u.isActivo());
        try {
            usuarioService.actualizar(u);
            mostrar("Usuario " + (u.isActivo() ? "activado" : "desactivado") + ".", Theme.SUCCESS);
            refrescar();
        } catch (SQLException e) {
            mostrar("No se pudo cambiar el estado: " + e.getMessage(), Theme.DANGER);
        }
    }

    private void resetearPassword() {
        Usuario u = usuarioSeleccionado();
        if (u == null) { mostrar("Selecciona un usuario.", Theme.WARNING); return; }
        JPasswordField nueva = new JPasswordField();
        nueva.putClientProperty("JPasswordField.showRevealButton", true);
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.add(new JLabel("Nueva contrasena para " + u.getUsername()));
        form.add(nueva);
        int op = JOptionPane.showConfirmDialog(this, form, "Resetear contrasena",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;
        char[] pass = nueva.getPassword();
        if (pass.length == 0) { mostrar("La contrasena no puede estar vacia.", Theme.DANGER); return; }
        try {
            usuarioService.cambiarPassword(u.getId(), pass);
            java.util.Arrays.fill(pass, '\0');
            mostrar("Contrasena de '" + u.getUsername() + "' actualizada.", Theme.SUCCESS);
        } catch (SQLException e) {
            mostrar("No se pudo resetear: " + e.getMessage(), Theme.DANGER);
        }
    }

    private boolean esUltimoAdmin() {
        try {
            return usuarioService.contarAdminsActivos() <= 1;
        } catch (SQLException e) {
            return true; // ante la duda, no permitir quedarnos sin admin
        }
    }

    public void refrescar() {
        try {
            usuariosActuales.clear();
            tablaModelo.setRowCount(0);
            for (Usuario u : usuarioService.listar()) {
                usuariosActuales.add(u);
                tablaModelo.addRow(new Object[]{
                        u.getUsername(),
                        u.getNombre() != null ? u.getNombre() : "",
                        u.getRol(),
                        u.isActivo() ? "Si" : "No"
                });
            }
        } catch (SQLException e) {
            mostrar("No se pudieron cargar los usuarios: " + e.getMessage(), Theme.DANGER);
        }
    }

    private void mostrar(String texto, Color color) {
        labelMensaje.setText(texto);
        labelMensaje.setForeground(color);
    }

    private JLabel seccion(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Theme.FONT_BOLD);
        l.setForeground(Theme.TEXT_PRIMARY);
        return l;
    }

    private JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Theme.FONT_BASE);
        l.setForeground(Theme.TEXT_SECONDARY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void estilizar(JTextField campo) {
        campo.setFont(Theme.FONT_BASE);
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        campo.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void estilizarSec(JButton btn) {
        btn.setFont(Theme.FONT_BASE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
