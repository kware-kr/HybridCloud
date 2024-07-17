package com.kware.common.server.h2;

import java.sql.SQLException;

import org.h2.tools.Server;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class H2NSServer {
	Server server = null;
	int port = 9093;
	String path = "mem";
	String dbname= "public";
	boolean ismemory = false;
	
	//memdb 생성 방법
	//Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9093", "-url", "jdbc:h2:mem:testdb").start();

	public boolean start(String port) {
		return start(null, null, port);
	}
	
	public boolean start(String path, String dbname) {
		return start(path, dbname, null);
	}
	
	public boolean start(String path, String dbname, String port) {		
		if(path != null && !"".equals(path) && !"mem".equals(path))
			this.path = path;
		else ismemory = true;
		
		if(dbname != null && !"".equals(dbname))
			this.dbname = dbname;
		
		if(port != null && !"".equals(port)) {
			try {
				int temp_port = Integer.parseInt(port);
				this.port = temp_port;
			}catch(NumberFormatException nfe) {
				
			}
		}
		
		try {
			if(this.ismemory)
				server = adviceRun(this.port, this.dbname);
			else server = adviceRun(this.port, this.dbname, this.path);
		} catch (SQLException e) {
			log.error("H2 Server start Error", e);
		}
		
 		if(server != null && server.isRunning(true)){
 			log.info("server run success");
 			log.info("H2 server url = {}", server.getURL());
 			return true;
 		}else { 			
 			log.info("server run fail");
 			return false;
 		}
 		
	}

	
	
	private Server adviceRun(int port, String dbname, String db_store) throws SQLException {
		String dbpath = db_store + "/storage/" + dbname;
 		return Server.createTcpServer(
 				"-tcp",
 				"-tcpAllowOthers",
 				"-ifNotExists",
 				"-tcpPort", port+"", "-key", dbname, dbpath).start();
 	}
	
	//메모리 DB 생성
	private Server adviceRun(int port, String dbname) throws SQLException {
		//String url = "mem:" + dbname;
 		return Server.createTcpServer(
 				"-tcp",
 				"-tcpAllowOthers",
 				"-ifNotExists",
 				"-tcpPort", port+"").start();
 	}

 	private Server defaultRun(int port) throws SQLException {
 		return Server.createTcpServer(
 				"-tcp",
 				"-tcpAllowOthers",
 				"-ifNotExists",
 				"-tcpPort", port+"").start();
 	}
 	
 	public void shutdown() {
 		server.stop();
 		
 		log.info("H2 server shutdown! complete!");
 	}
}
