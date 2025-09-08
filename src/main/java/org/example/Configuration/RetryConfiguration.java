package org.example.Configuration;

/**
 * 重试模板配置
 */
@Configuration
public class RetryConfiguration {

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate template = new RetryTemplate();

        // 固定延迟重试策略
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(2000); // 2秒间隔
        template.setBackOffPolicy(backOffPolicy);

        // 重试策略：最多3次
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        template.setRetryPolicy(retryPolicy);

        return template;
    }
}

