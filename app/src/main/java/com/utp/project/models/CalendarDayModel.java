package com.utp.project.models;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Modelo de datos para representar un día en el mini calendario horizontal.
 * Contiene la fecha real (LocalDate) y el estado de selección.
 */
public class CalendarDayModel {
    private final LocalDate date;
    private boolean isSelected;

    public CalendarDayModel(LocalDate date) {
        this.date = date;
        this.isSelected = false;
    }

    public LocalDate getDate() {
        return date;
    }
    /**
     * Obtiene el nombre corto del día de la semana (Ej: LUN, MAR) en mayúsculas.
     */
    public String getDayOfWeekName() {
        // Usa el idioma por defecto del dispositivo para obtener el nombre corto.
        return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("es", "ES")).toUpperCase();
    }

    /**
     * Obtiene el número del día (Ej: 1, 24).
     */
    public String getDayNumber() {
        return String.valueOf(date.getDayOfMonth());
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}

