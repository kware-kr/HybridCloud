package kware.app.collector.domain.metric;

import cetus.dao.CetusDao;
import org.springframework.stereotype.Component;

@Component
public class MetricDao extends CetusDao<Metric> {
    public MetricDao() {
        super("metric");
    }
}
