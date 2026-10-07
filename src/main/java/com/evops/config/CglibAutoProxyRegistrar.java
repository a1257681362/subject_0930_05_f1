package com.evops.config;

import org.springframework.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 以标准 RootBeanDefinition 注册 CGLIB 版自动代理创建器（bean 名固定为
 * internalAutoProxyCreator）：
 * - Shiro 的代理创建器带 @ConditionalOnMissingBean(internalAutoProxyCreator)，检测到后让位；
 * - 必须用注册器而非 @Bean：@Bean 工厂方法产生的定义 beanClassName 为 null，事务自动配置
 *   升级代理时会抛 “Class name [null] is not a known auto-proxy creator class”；
 * - 类型必须在 Spring 的代理优先级表内（如 InfrastructureAdvisorAutoProxyCreator），
 *   DefaultAdvisorAutoProxyCreator 不在表内会被拒绝升级；
 * - proxyTargetClass=true 后，@Transactional 的具体类 Service 走 CGLIB 代理，可按类型注入；
 * - Shiro 的 URL 过滤链由 ShiroFilterFactoryBean 独立承担，不受影响。
 */
public class CglibAutoProxyRegistrar implements ImportBeanDefinitionRegistrar {

    private static final String AUTO_PROXY_CREATOR_BEAN_NAME =
            "org.springframework.aop.config.internalAutoProxyCreator";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        if (!registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
            RootBeanDefinition definition = new RootBeanDefinition(InfrastructureAdvisorAutoProxyCreator.class);
            definition.getPropertyValues().add("proxyTargetClass", Boolean.TRUE);
            definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, definition);
        }
    }
}
