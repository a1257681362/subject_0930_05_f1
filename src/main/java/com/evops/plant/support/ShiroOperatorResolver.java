package com.evops.plant.support;

import com.evops.plant.domain.PfOperator;
import com.evops.plant.mapper.PfOperatorMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 从 Shiro 会话解析操作员：登录名即 pf_operator.operator_code。
 * 骨架自带 bootstrap 账户（种子数据 id=2）。
 */
@Component
public class ShiroOperatorResolver {

    private static PfOperatorMapper operatorMapper;

    @Autowired
    public void setOperatorMapper(PfOperatorMapper operatorMapper) {
        ShiroOperatorResolver.operatorMapper = operatorMapper;
    }

    public static Long currentOperatorId() {
        try {
            Subject subject = SecurityUtils.getSubject();
            Object principal = subject != null ? subject.getPrincipal() : null;
            if (principal == null || operatorMapper == null) {
                return null;
            }
            PfOperator operator = operatorMapper.selectOne(
                    new QueryWrapper<PfOperator>().eq("operator_code", principal.toString()));
            return operator != null ? operator.getId() : null;
        } catch (Exception e) {
            // 非 Web 线程（如定时任务）没有 Shiro 上下文，交回 OperatorContext 兜底。
            return null;
        }
    }
}
