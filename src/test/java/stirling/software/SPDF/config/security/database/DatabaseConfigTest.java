package stirling.software.SPDF.config.security.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import stirling.software.SPDF.model.ApplicationProperties;
import stirling.software.SPDF.model.provider.UnsupportedProviderException;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseConfigTest {

    @Mock
    private ApplicationProperties applicationProperties;

    @InjectMocks
    private DatabaseConfig databaseConfig;

    @Test
    void testDefaultConfigurationForDataSource() throws UnsupportedProviderException {
        var system = mock(ApplicationProperties.System.class);
        var datasource = mock(ApplicationProperties.Datasource.class);
        var testUrl = "jdbc:h2:mem:test";

        when(applicationProperties.getSystem()).thenReturn(system);
        when(system.getDatasource()).thenReturn(datasource);
        when(datasource.isUseDefault()).thenReturn(true);
        when(datasource.getDefaultUrl()).thenReturn(testUrl);

        var result = databaseConfig.dataSource();

        assertInstanceOf(DataSource.class, result);
    }

    @Test
    void testCustomConfigurationForDataSource() throws UnsupportedProviderException {
        var system = mock(ApplicationProperties.System.class);
        var datasource = mock(ApplicationProperties.Datasource.class);

        when(applicationProperties.getSystem()).thenReturn(system);
        when(system.getDatasource()).thenReturn(datasource);
        when(datasource.isUseDefault()).thenReturn(false);
        when(datasource.getType()).thenReturn("postgresql");
        when(datasource.getHostName()).thenReturn("localhost");
        when(datasource.getPort()).thenReturn(5432);
        when(datasource.getName()).thenReturn("postgres");
        when(datasource.getUsername()).thenReturn("postgres");
        when(datasource.getPassword()).thenReturn("postgres");

        var result = databaseConfig.dataSource();

        assertInstanceOf(DataSource.class, result);
    }

    @ParameterizedTest(name = "Exception thrown when the DB type [{arguments}] is not supported")
    @ValueSource(strings = {"oracle", "mysql", "mongoDb"})
    void exceptionThrownWhenDBTypeIsUnsupported(String datasourceType) {
        var system = mock(ApplicationProperties.System.class);
        var datasource = mock(ApplicationProperties.Datasource.class);

        when(applicationProperties.getSystem()).thenReturn(system);
        when(system.getDatasource()).thenReturn(datasource);
        when(datasource.isUseDefault()).thenReturn(false);
        when(datasource.getType()).thenReturn(datasourceType);

        assertThrows(UnsupportedProviderException.class, () -> databaseConfig.dataSource());
    }
}