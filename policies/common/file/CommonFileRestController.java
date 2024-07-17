package kware.common.file;

import cetus.Response;
import lombok.RequiredArgsConstructor;
import me.desair.tus.server.exception.TusException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("cetus/files")
public class CommonFileRestController {
    
    private final CommonFileService fileService;
    
    /**
     * 임시 저장 파일 목록
     * @return
     */
    @GetMapping("temp/list.json")
    public Response lastUpload(CommonFile bean) {
        bean.setSaved(CommonFileState.N.name());
        bean.setUid(0L);
        return Response.ok(fileService.lastUpload(bean));
    }
    
    /**
     * 수정 파일 목록 (임시 파일 포함)
     * @param bean
     * @return
     */
    @GetMapping("edit/list.json")
    public Response editList(CommonFile bean) {
        return Response.ok(fileService.editList(bean));
    }
    
    /**
     * 저장된 파일 목록
     * @param bean
     * @return
     */
    @GetMapping("list.json")
    public Response list(CommonFile bean) {
        return Response.ok(fileService.list(bean));
    }
    
    /**
     * 임시 파일 삭제
     * @param bean
     * @return
     * @throws TusException
     * @throws IOException
     */
    @DeleteMapping("temp/delete.json")
    public Response deleteTemp(CommonFile bean) throws TusException, IOException {
        return Response.ok(fileService.deleteFileAndUploadedData(bean));
    }
    
    /**
     * 파일 다운로드 기록
     * @param bean
     * @return
     */
    @GetMapping("log/list.json")
    public Response logList(CommonFileLog bean) {
        return Response.ok(fileService.logList(bean));
    }
}
