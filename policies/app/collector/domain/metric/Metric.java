package kware.app.collector.domain.metric;

import cetus.bean.CetusBean;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class Metric extends CetusBean {
    private LocalDateTime createdAt;
    private String instance;
    private String nodeName;
    private String data;
}
