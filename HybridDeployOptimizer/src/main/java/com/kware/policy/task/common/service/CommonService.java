package com.kware.policy.task.common.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kware.policy.task.common.service.dao.CommonDao;
import com.kware.policy.task.common.service.dao.CommonEventDao;
import com.kware.policy.task.common.service.vo.CommonConfigGroup;
import com.kware.policy.task.common.service.vo.CommonEvent;


//@Slf4j
@Service
public class CommonService {
	
	//{{db관련 서비스
	@Autowired
	protected CommonDao dao;

	public List<?> selectCommonConfigGroupList() {
		return dao.selectCommonConfigGroupList();
	}

	public CommonConfigGroup selectCommonConfigGroup(CommonConfigGroup.ConfigName cfgName) {
		return dao.selectCommonConfigGroup(cfgName);
	}
	//}}db 관련 서비스
	
	
	@Autowired
	protected CommonEventDao eventDAO;
	
	public void createEvent(String name, String type, String desc ){
		CommonEvent event = new CommonEvent(name, type, desc);
		this.createEvent(event);
	}

    public void createEvent(CommonEvent event) {
        eventDAO.insertEvent(event);
    }

    public CommonEvent getEventById(int id) {
        return eventDAO.getEventById(id);
    }

    public List<CommonEvent> getAllEvents(CommonEvent event) {
        return eventDAO.getAllEvents(event);
    }
}