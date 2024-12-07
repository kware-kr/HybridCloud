package com.kware.policy.task.common.service.dao;


import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.kware.policy.task.common.service.vo.CommonEvent;

@Repository
public class CommonEventDao {
    private final SqlSessionTemplate sqlSessionTemplate;

    public CommonEventDao(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    public void insertEvent(CommonEvent event) {
    	sqlSessionTemplate.insert("EventMapper.insertEvent", event);
    }

    public CommonEvent getEventById(int id) {
        return sqlSessionTemplate.selectOne("EventMapper.getEventById", id);
    }

    public List<CommonEvent> getAllEvents(CommonEvent event) {
        return sqlSessionTemplate.selectList("EventMapper.getAllEvents", event);
    }
}
