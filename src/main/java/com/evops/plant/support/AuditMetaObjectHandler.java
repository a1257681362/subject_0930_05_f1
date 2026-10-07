package com.evops.plant.support;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 审计字段自动填充：
 * 任何 INSERT/UPDATE 都写入当前操作者与时间，操作者绝不允许为空（SYSTEM 兜底）。
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Long operatorId = OperatorContext.currentOperatorId();
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createBy", Long.class, operatorId);
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateBy", Long.class, operatorId);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long operatorId = OperatorContext.currentOperatorId();
        this.strictUpdateFill(metaObject, "updateBy", Long.class, operatorId);
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
