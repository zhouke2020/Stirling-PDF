package stirling.software.SPDF.config.security.database;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import stirling.software.SPDF.model.ApplicationProperties;
import stirling.software.SPDF.model.provider.UnsupportedProviderException;

@Getter
@Slf4j
@Configuration
public class DatabaseConfig {

    public static final String DATASOURCE_URL_TEMPLATE = "jdbc:%s://%s:%4d/%s";

    private final ApplicationProperties applicationProperties;

    public DatabaseConfig(@Autowired ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    /**
     * Creates the <code>DataSource</code> for the connection to the DB. If <code>useDefault</code>
     * is set to <code>true</code>, it will use the default H2 DB. If it is set to <code>false
     * </code>, it will use the user's custom configuration set in the settings.yml.
     *
     * @return a <code>DataSource</code> using the configuration settings in the settings.yml
     * @throws UnsupportedProviderException if the type of database selected is not supported
     */
    @Bean
    public DataSource dataSource() throws UnsupportedProviderException {
        ApplicationProperties.System system = applicationProperties.getSystem();
        ApplicationProperties.Datasource datasource = system.getDatasource();
        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();

        if (datasource.isUseDefault()) {
            log.debug("Using default H2 database");

            dataSourceBuilder.driverClassName("org.h2.Driver");
            dataSourceBuilder.url(datasource.getDefaultUrl());
            dataSourceBuilder.username("sa");

            return dataSourceBuilder.build();
        }

        dataSourceBuilder.driverClassName(getDriverClassName(datasource.getType()));
        dataSourceBuilder.url(
                getDataSourceUrl(
                        datasource.getType(),
                        datasource.getHostName(),
                        datasource.getPort(),
                        datasource.getName()));
        dataSourceBuilder.username(datasource.getUsername());
        dataSourceBuilder.password(datasource.getPassword());

        return dataSourceBuilder.build();
    }

    /**
     * Generate the URL the <code>DataSource</code> will use to connect to the database
     *
     * @param dataSourceType the type of the database
     * @param hostname the host name
     * @param port the port number to use for the database
     * @param dataSourceName the name the database to connect to
     * @return the <code>DataSource</code> URL
     */
    private String getDataSourceUrl(
            String dataSourceType, String hostname, Integer port, String dataSourceName) {
        return DATASOURCE_URL_TEMPLATE.formatted(dataSourceType, hostname, port, dataSourceName);
    }

    /**
     * @return a <code>Connection</code> using the configured <code>DataSource</code>
     * @throws SQLException if a database access error occurs
     * @throws UnsupportedProviderException when an unsupported database is selected
     */
    public Connection connection() throws SQLException, UnsupportedProviderException {
        return dataSource().getConnection();
    }

    /**
     * Selects the database driver based on the type of database chosen.
     *
     * @param driverName the type of the driver (e.g. 'h2', 'postgresql')
     * @return the fully qualified driver for the database chosen
     * @throws UnsupportedProviderException when an unsupported database is selected
     */
    private String getDriverClassName(String driverName) throws UnsupportedProviderException {
        try {
            ApplicationProperties.Driver driver =
                    ApplicationProperties.Driver.valueOf(driverName.toUpperCase());

            switch (driver) {
                case H2 -> {
                    log.debug("H2 driver selected");
                    return "org.h2.Driver";
                }
                case POSTGRESQL -> {
                    log.debug("Postgres driver selected");
                    return "org.postgresql.Driver";
                }
                default -> {
                    log.warn("{} driver selected", driverName);
                    throw new UnsupportedProviderException(
                            driverName + " is not currently supported");
                }
            }
        } catch (IllegalArgumentException e) {
            log.warn("Unknown driver: {}", driverName);
            throw new UnsupportedProviderException(driverName + " is not currently supported");
        }
    }
}
