package com.kware.common.db.dao;


import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDao {
	Logger logger;

	/** MyBatis */
	protected SqlSessionTemplate sqlSession;
	
	/** 생성자를 통한 객체 생성 */
	public DefaultDao(SqlSessionTemplate sqlSession, Logger logger) {
		this.sqlSession = sqlSession;
		this.logger = logger;
	}
	
	/** 생성자를 통한 객체 생성 */
	public DefaultDao(SqlSessionTemplate sqlSession) {
		this.sqlSession = sqlSession;
		this.logger = LoggerFactory.getLogger(this.getClass().getName());
	}

}
