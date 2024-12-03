package com.kware.hybrid.service.dao;


import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import com.kware.hybrid.service.vo.CommonFeatureVO;

@Repository
public class CommonFeatureDao {
    private final SqlSession sqlSession;

    public CommonFeatureDao(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    public int insertCommonFeature(CommonFeatureVO commonFeature) {
        return sqlSession.insert("commonFeatureMapper.insertCommonFeature", commonFeature);
    }

    public int updateCommonFeature(CommonFeatureVO commonFeature) {
        return sqlSession.update("commonFeatureMapper.updateCommonFeature", commonFeature);
    }

    public int deleteCommonFeature(String feaName, String feaSubName) {
        return sqlSession.update("commonFeatureMapper.deleteCommonFeature", 
            new CommonFeatureVO() {{
                setFeaName(feaName);
                setFeaSubName(feaSubName);
            }}
        );
    }

    public CommonFeatureVO selectCommonFeatureByKey(String feaName, String feaSubName) {
        return sqlSession.selectOne("commonFeatureMapper.selectCommonFeatureByKey", 
            new CommonFeatureVO() {{
                setFeaName(feaName);
                setFeaSubName(feaSubName);
            }}
        );
    }

    public List<CommonFeatureVO> selectAllCommonFeatures(String feaName) {
        return sqlSession.selectList("commonFeatureMapper.selectAllCommonFeatures", feaName);
    }
}
