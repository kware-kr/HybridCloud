package kware.app.collector.domain.config;

import cetus.bean.CetusBean;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class Config extends CetusBean {
    private final String key;
    private String value;
    private OffsetDateTime createdAt;

    public Config(String key) {
        this.key = key;
    }
}
