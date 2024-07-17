package kware.common.file;

import cetus.bean.CetusBean;
import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.type.Alias;

@Getter
@Setter
@Alias("fileLog")
public class CommonFileLog extends CetusBean {
    private String uid;
    private String fileUrl;
    private String regId;
    private String regDt;
}
