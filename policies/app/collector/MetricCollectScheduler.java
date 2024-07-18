package kware.app.collector;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 메트릭 수집 스케줄러
 */
@RequiredArgsConstructor
@Component
public class MetricCollectScheduler {

    private final TaskScheduler taskScheduler;
    private final MetricCollectTask collectTask;

    private List<String> cronEvery5Seconds() {
        final String cron4Seconds = "%d * * * * *";
        final int everySeconds = 5;
        final int minutes = 60;

        List<String> cronList = new ArrayList<>();

        for (int i = 0; i < minutes / everySeconds; i++) {
            String cron = String.format(cron4Seconds, i * everySeconds);
            cronList.add(cron);
        }

        return cronList;
    }

    @PostConstruct
    private void postConstruct() {
        cronEvery5Seconds().forEach(cron -> taskScheduler.schedule(collectTask, new CronTrigger(cron)));
    }
}
