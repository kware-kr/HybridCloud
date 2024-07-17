package kware.app.collector.domain.config;

import cetus.dao.CetusDao;
import org.springframework.stereotype.Component;

@Component
public class ConfigDao extends CetusDao<Config> {
    public ConfigDao() {
        super("config");
    }
}
