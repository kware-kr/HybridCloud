package com.kware.policy.task.common.service.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class CommonQueueDefault {
  @JsonIgnore
  private Long      timemillisecond = 0L;
}