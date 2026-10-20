package com.sforca.callmanager.core.designsystem.component

/**
 * Which semantic color family a [StatusPill] (or anything else coloring itself by call outcome)
 * should use. Lives in the design system, not the domain layer — it maps to a color, not a rule.
 */
enum class StatusTone {
    BLOCKED,
    ALLOWED,
    SILENCED,
    UNKNOWN,
}
