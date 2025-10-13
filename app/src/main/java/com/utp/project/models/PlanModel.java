package com.utp.project.models;

/**
 * Modelo de datos para representar un plan o actividad.
 */
public class PlanModel {
    private String title;
    private String timeRange; // Ej: "11:45 PM - 12:00 AM"
    private int colorResource; // Usaremos un ID de recurso de color (R.color.mi_color)
    private int backgroundResource; // Color semitransparente para el fondo

    // Constructor
    public PlanModel(String title, String timeRange, int colorResource, int backgroundResource) {
        this.title = title;
        this.timeRange = timeRange;
        this.colorResource = colorResource;
        this.backgroundResource = backgroundResource;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public int getColorResource() {
        return colorResource;
    }

    public int getBackgroundResource() {
        return backgroundResource;
    }
}