package stirling.software.SPDF.EE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ResourceLoader;
import org.thymeleaf.spring6.SpringTemplateEngine;

import stirling.software.SPDF.config.FileFallbackTemplateResolver;
import stirling.software.SPDF.model.ApplicationProperties;

@Configuration
@Lazy
public class EEAppConfig {

    private static final Logger logger = LoggerFactory.getLogger(EEAppConfig.class);

    @Autowired ApplicationProperties applicationProperties;

    @Bean(name = "RunningEE")
    public boolean runningEnterpriseEdition() {
        // TODO: Implement EE detection
        return true;
    }
}
