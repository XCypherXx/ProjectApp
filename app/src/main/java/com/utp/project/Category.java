package com.utp.project;

public class Category {
    private String name;
    private int iconResId;
    private int taskCount;
    private int completedCount;
    public Category(String name, int iconResId, int taskCount) {
        this.name = name;
        this.iconResId = iconResId;
        this.taskCount = taskCount;
        this.completedCount = 0;
    }

    public Category(String name, int iconResId, int taskCount, int completedCount) {
        this.name = name;
        this.iconResId = iconResId;
        this.taskCount = taskCount;
        this.completedCount = completedCount;
    }

    public String getName() { return name; }
    public int getIconResId() { return iconResId; }
    public int getTaskCount() { return taskCount; }
    public int getCompletedCount() { return completedCount; }

    public void setTaskCount(int count) { this.taskCount = count; }
    public void setCompletedCount(int count) { this.completedCount = count; }

    public int getProgressPercent() {
        if (taskCount == 0) return 0;
        return (int) ((completedCount * 100.0f) / taskCount);
    }}
