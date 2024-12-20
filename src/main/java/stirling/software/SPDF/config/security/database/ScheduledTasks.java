package stirling.software.SPDF.config.security.database;

import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import stirling.software.SPDF.model.provider.UnsupportedProviderException;

@Component
public class ScheduledTasks {

    @Autowired private DatabaseService databaseService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void performBackup() throws SQLException, UnsupportedProviderException {
        databaseService.exportDatabase();
    }
}
