package com.kware.policy.task.common.service.dao;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import com.kware.policy.task.common.service.vo.CommonConfigGroup;

@Repository //@Repository("이름")으로 처리하고 @Resource(name="이름") 주입한다.
public class CommonDao {
	@Autowired
	@Qualifier("sqlSessionTemplate")
	SqlSessionTemplate sqlSessionTemplate;

	/************************ mo_user_request ***********************************/
	
	
	public List<?> selectCommonConfigGroupList() {
		return sqlSessionTemplate.selectList("ptCommonMapper.selectCommonConfigGroupList");
	}

	public CommonConfigGroup selectCommonConfigGroup(CommonConfigGroup.ConfigName cfgName) {
		return sqlSessionTemplate.selectOne("ptCommonMapper.selectCommonConfigGroup", cfgName.toString());
	}
}
