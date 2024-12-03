package com.kware.policy.task.selector.service.vo;

import java.time.LocalDateTime;

import com.kware.policy.task.selector.service.vo.WorkloadRequest.Container;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * 이 클래스는 노드셀렉터가 예상시간을 포함한 노드를 선태할 수 있도록 정보를 실시간 갱신하면서 저장 
 * WorkloadRequest.Container를 Wrapping하여 실제 노드에 배포된 파드의 실행시간, 예측실행시간, 예측 종료시간정보를 실시간 정보를 
 * 통해 저장하고, 
 */
@Getter
@Setter
public class WorkloadTaskContainerWrapper{
	
	@Setter(AccessLevel.NONE)
	private Container container; //워크로드 요청시에 제공한 Container 한개의 정보 
	private LocalDateTime estimatedStartTime;  //예상 시작시간
    private LocalDateTime estimatedEndTime;    //예상 종료시간
    private LocalDateTime actualStartTime;     //실제 시작시간
    private LocalDateTime actualEndTime;       //실제 종료시간
    
    
    private String podUid; //이건 프로메테우스 메트릭에 포함된 정보를 넣으면 어떤가?
    private String resouceName; //api에서 제공하는 이름으로
    private String pod; //pod이름//프로메테우스 resourcename + container.name + randum number고 구성됨
    
    
    public WorkloadTaskContainerWrapper(Container container) {
    	this.container = container;
    }
}
