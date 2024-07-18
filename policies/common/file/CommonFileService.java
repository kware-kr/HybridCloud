package kware.common.file;

import me.desair.tus.server.exception.TusException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

/**
 * {@link CommonFileState}
 */
public interface CommonFileService {
    
    /**
     * 파일 UID 생성
     * @return
     */
    Long generateUid();
    
    /**
     * 저장된 파일 리스트
     * @param bean
     * @return
     */
    List<CommonFile> list(CommonFile bean);
    
    /**
     * 수정 파일 목록 (임시 파일 포함)
     * @param bean
     * @return
     */
    List<CommonFile> editList(CommonFile bean);
    
    /**
     * 파일 다운로드 기록
     * @param bean
     * @return
     */
    List<CommonFileLog> logList(CommonFileLog bean);
    
    /**
     * 임시 파일 목록
     * @return
     */
    List<CommonFile> lastUpload(CommonFile bean);
    
    /**
     * 파일 다운로드
     * @param req
     * @return
     */
    int download(final HttpServletRequest req);
    
    /**
     * 업로드 파일 저장
     * @param req
     * @throws TusException
     * @throws IOException
     */
    void saveFile(final HttpServletRequest req) throws TusException, IOException;
    
    /**
     * 실제 파일 제거
     * @param req
     * @return
     * @throws TusException
     * @throws IOException
     */
    int deleteFile(final HttpServletRequest req) throws TusException, IOException;
    
    /**
     * 1. 실제 파일 제거
     * 2. delete k_cetus.file by user_id and url
     * @param bean
     * @return
     * @throws TusException
     * @throws IOException
     */
    int deleteFileAndUploadedData(CommonFile bean) throws TusException, IOException;
    
    /**
     * 파일 상태 저장으로 변경
     * @param bean
     * @return
     */
    int changeSaved(CommonFile bean);
    
    /**
     * 실제 파일 제거
     * update k_cetus.file.useAt to 'N' by url
     * @param bean
     * @return
     */
    int delete(CommonFile bean);
}
