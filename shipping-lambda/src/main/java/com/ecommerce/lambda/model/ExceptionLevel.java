package com.ecommerce.lambda.model;

/**
 * Enum для уровней критичности ошибок
 */
public enum ExceptionLevel {
    TRACE("TRACE", 0),
    DEBUG("DEBUG", 1),
    INFO("INFO", 2),
    WARN("WARN", 3),
    ERROR("ERROR", 4),
    FATAL("FATAL", 5);

    private final String level;
    private final int priority;

    ExceptionLevel(String level, int priority) {
        this.level = level;
        this.priority = priority;
    }

    public String getLevel() {
        return level;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * Проверяет, является ли уровень критическим (ERROR или FATAL)
     */
    public boolean isCritical() {
        return this == ERROR || this == FATAL;
    }

    /**
     * Проверяет, требует ли уровень немедленного алерта (FATAL)
     */
    public boolean requiresImmediateAlert() {
        return this == FATAL;
    }

    @Override
    public String toString() {
        return level;
    }
} 