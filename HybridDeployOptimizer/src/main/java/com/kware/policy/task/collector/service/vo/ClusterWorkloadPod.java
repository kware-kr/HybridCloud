package com.kware.policy.task.collector.service.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ClusterWorkloadPod extends ClusterDefault{
  private String uid;  //pod uid
  private String kind; //kind 이름
  private String pod;  //pod이름
  private String node; //노드이름
  private String mlId; //ml이름
}