package com.kware.policy.task.collector.service.vo;

import lombok.Getter;
import lombok.Setter;
import net.minidev.json.annotate.JsonIgnore;

@Getter
@Setter
public class PromMetricDefault {
  @JsonIgnore
  private Long      timemillisecond = 0L;
  public void clear() { }
}