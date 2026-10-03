package com.mparsict.vosolmaliat.domain.model;

/** The reserved local user 0 represents background/system work. */
public final class AuditContext {
    private static final ThreadLocal<Long> USER = new ThreadLocal<>();
    private AuditContext() {}
    public static long userId() { return USER.get() == null ? 0L : USER.get(); }
    public static void bind(Long id) { USER.set(id); }
    public static void clear() { USER.remove(); }
}
