package kware.common.file;

import cetus.service.CetusService;
import cetus.user.SessionUserUtil;
import cetus.util.FileUtil;
import cetus.util.WebUtil;
import kware.common.config.auth.CetusUser;
import lombok.RequiredArgsConstructor;
import me.desair.tus.server.TusFileUploadService;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.upload.UploadInfo;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CommonFileServiceImpl extends CetusService<CommonFile, CommonFileDao> implements CommonFileService {

    private static final String IN_PROGRESS_ID = "InProgress";

    private final TusFileUploadService uploadService;

    public Long generateUid() {
        return dao.key();
    }

    @Override
    public List<CommonFile> list(CommonFile bean) {
        bean.setSaved(CommonFileState.Y.name());
        return super.list(bean);
    }

    public List<CommonFile> editList(CommonFile bean) {
        return super.list(bean);
    }

    public List<CommonFileLog> logList(CommonFileLog bean) {
        return dao.selectLog(bean);
    }

    public List<CommonFile> lastUpload(CommonFile bean) {
        return super.list(bean);
    }

    public int download(final HttpServletRequest req) {
        CetusUser user = (CetusUser) SessionUserUtil.getUser(req);
        String url = req.getRequestURI();

        CommonFileLog log = new CommonFileLog();
        log.setFileUrl(url);
        log.setRegId(user != null ? user.getUserId() : WebUtil.getIpAddress(req));

        return dao.insertLog(log);
    }

    public void saveFile(final HttpServletRequest req) throws TusException, IOException {
        CetusUser user = (CetusUser) SessionUserUtil.getUser(req);
        String uploadURI = req.getRequestURI();
        UploadInfo info = uploadService.getUploadInfo(uploadURI);

        if (info == null) return;

        CommonFile file = new CommonFile();

        file.setUid(0L);
        file.setName(info.getFileName());
        file.setSize(info.getLength());
        file.setType(info.getFileMimeType());
        file.setExtension(FileUtil.extension(info.getFileName()));
        file.setUrl(uploadURI);
        file.setRegId(user.getUserId());

        final String fileUidName = "uid";
        if (info.getMetadata().containsKey(fileUidName)) {
            Long uid = Long.parseLong(info.getMetadata().get(fileUidName));
            CommonFile temp = new CommonFile();
            temp.setRegId(user.getUserId());
            temp.setUid(uid);
            if (dao.isRegister(temp)) file.setUid(uid);
        }

        if (info.isUploadInProgress()) {
            file.setId(IN_PROGRESS_ID);
            file.setSaved(CommonFileState.P.name());
        } else {
            file.setId(info.getId().toString());
            file.setSaved(CommonFileState.N.name());
        }
        dao.merge(file);
    }

    public int deleteFile(final HttpServletRequest req) throws TusException, IOException {
        String requestURI = req.getRequestURI();

        CommonFile file = new CommonFile();
        file.setUrl(requestURI);

        return dao.deleteReal(file);
    }

    public int deleteFileAndUploadedData(CommonFile bean) throws TusException, IOException {
        uploadService.deleteUpload(bean.getUrl());
        return dao.deleteReal(bean);
    }

    public int changeSaved(CommonFile bean) {
        return dao.changeSaved(bean);
    }

    @Override
    public int delete(CommonFile bean) {
        try {
            uploadService.deleteUpload(bean.getUrl());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return super.delete(bean);
    }
}
