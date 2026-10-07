package com.evops.plant.support;

/**
 * 操作者上下文。优先级：显式设置（定时任务用） > Shiro 当前登录人 > SYSTEM 兜底。
 * 任何业务写入都能解析到操作者 ID，保证 create_by/update_by 不丢失。
 */
public final class OperatorContext {

    /** 系统兜底操作员（schema.sql 种子数据固定 id=1） */
    public static final long SYSTEM_OPERATOR_ID = 1L;

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private OperatorContext() {
    }

    /** 以指定操作员身份执行（如定时任务以 SYSTEM 身份建档） */
    public static void setOperatorId(Long operatorId) {
        CURRENT.set(operatorId);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static Long currentOperatorId() {
        Long explicit = CURRENT.get();
        if (explicit != null) {
            return explicit;
        }
        Long shiroUser = ShiroOperatorResolver.currentOperatorId();
        if (shiroUser != null) {
            return shiroUser;
        }
        return SYSTEM_OPERATOR_ID;
    }
}
