package com.kware.policy.task.common.service.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	public List<CommonConfigGroup> selectCommonConfigGroup(String feaName) {
		return sqlSessionTemplate.selectList("ptCommonMapper.selectCommonConfigGroup", feaName);
	}
	
	public CommonConfigGroup selectCommonConfigGroupSub(String feaName, String feaSubName) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("feaName"   , feaName);
		map.put("feaSubName", feaSubName);
		
		CommonConfigGroup ccGroup = sqlSessionTemplate.selectOne("ptCommonMapper.selectCommonConfigGroupSub", map);
		
		map.clear();
		return ccGroup;
	}
	
	
	public Double selectCommonGpuScore(String product) {
		return sqlSessionTemplate.selectOne("ptCommonMapper.selectCommonGpuScore", product);
	}

	public Map selectCommonGpuMinMaxScore() {
		return sqlSessionTemplate.selectOne("ptCommonMapper.selectCommonGpuMinMaxScore");
	}
	
}
