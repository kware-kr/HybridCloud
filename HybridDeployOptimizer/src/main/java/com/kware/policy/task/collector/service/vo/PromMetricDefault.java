package com.kware.policy.task.collector.service.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class PromMetricDefault {
  @JsonIgnore
  private Long      timemillisecond = 0L;
  public void clear() { }
}