package kware.common.file;

import cetus.bean.CetusBean;
import cetus.user.SessionUser;
import kware.common.config.auth.CetusUser;
import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.type.Alias;

@Alias("file")
@Getter
@Setter
public class CommonFile extends CetusBean {
    private Long uid;
    private String id;
    private String saved;
    private String name;
    private String url;
    private String type;
    private String extension;
    private Long size;
    private String regId;
    private String regDt;

    @Override
    public void setDataWithUser(SessionUser user) {
        regId = ((CetusUser) user).getUserId();
    }
}
