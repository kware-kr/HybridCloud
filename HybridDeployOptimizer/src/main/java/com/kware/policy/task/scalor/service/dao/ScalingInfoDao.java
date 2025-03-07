package com.kware.policy.task.scalor.service.dao;


import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.kware.policy.task.scalor.service.vo.ScalingInfo;

@Repository
public class ScalingInfoDao {

    private final SqlSessionTemplate sqlSessionTemplate;
    
    public ScalingInfoDao(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }
    
    public int insertScalingInfo(ScalingInfo vo) {
        int result = sqlSessionTemplate.insert("insertScalingInfo", vo);
        return result;
    }

    public ScalingInfo selectScalingInfoByUid(Long uid) {
        return sqlSessionTemplate.selectOne("selectScalingInfoByUid", uid);
    }
    
    public ScalingInfo selectAllScalingInfo(ScalingInfo vo) {
        return sqlSessionTemplate.selectOne("selectAllScalingInfo", vo);
    }

    public List<ScalingInfo> selectAllScalingInfos() {
        return sqlSessionTemplate.selectList("selectAllScalingInfos");
    }

    public int updateScalingInfo(ScalingInfo vo) {
        int result = sqlSessionTemplate.update("updateScalingInfo", vo);
        return result;
    }

    public int deleteScalingInfo(Long uid) {
        int result = sqlSessionTemplate.delete("deleteScalingInfo", uid);
        return result;
    }
}
