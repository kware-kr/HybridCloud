package kware.common.file;

import cetus.dao.CetusDao;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommonFileDao extends CetusDao<CommonFile> {
    
    public CommonFileDao() {
        super("file");
    }
    
    public Long key() {
        return selectOne("key");
    }
    
    public int changeSaved(CommonFile bean) {
        return update("changeSaved", bean);
    }
    
    public int deleteReal(CommonFile bean) {
        return delete("deleteReal", bean);
    }
    
    public int insertLog(CommonFileLog bean) {
        return insert("insertLog", bean);
    }
    
    public List<CommonFileLog> selectLog(CommonFileLog bean) {
        return selectList("selectLog", bean);
    }
    
    public int merge(CommonFile bean) {
        return update("merge", bean);
    }
    
    public boolean isRegister(CommonFile bean) {
        return selectOne("isRegister", bean);
    }
}
