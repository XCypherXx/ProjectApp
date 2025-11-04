package com.utp.project.models;

/**
 * Modelo de datos para representar un plan o actividad.
 */
public class PlanModel {
    private String id; // ID de documento en Firestore
    private String title;
    private String timeRange; // Ej: "11:45 PM - 12:00 AM"
    private int colorResource; // Usaremos un ID de recurso de color (R.color.mi_color)
    private int backgroundResource; // Color semitransparente para el fondo
    private boolean completed; // Estado derivado de "estado"
    private long startTimeMs; // Para ordenar por fecha de inicio

    // Constructor
    public PlanModel(String id, String title, String timeRange, int colorResource, int backgroundResource, boolean completed, long startTimeMs) {
        this.id = id;
        this.title = title;
        this.timeRange = timeRange;
        this.colorResource = colorResource;
        this.backgroundResource = backgroundResource;
        this.completed = completed;
        this.startTimeMs = startTimeMs;
    }

    // Getters
    public String getId() {
        return id;
    }
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

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }
}