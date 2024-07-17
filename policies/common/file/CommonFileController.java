package kware.common.file;

import lombok.RequiredArgsConstructor;
import me.desair.tus.server.TusFileUploadService;
import me.desair.tus.server.exception.TusException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Controller
@RequiredArgsConstructor
@RequestMapping("cetus/files")
public class CommonFileController {
    
    private final CommonStaticFiles staticFiles;
    private final TusFileUploadService uploadService;
    private final CommonFileService fileService;
    
    @GetMapping(value="/static/{id}")
    public ResponseEntity<?> download(@PathVariable String id) throws UnsupportedEncodingException {
        
        String orgFileNm = staticFiles.get(id);
        ClassPathResource staticFile = new ClassPathResource("/static/files/" + id);
        
        String fileNameOrg = URLEncoder.encode(orgFileNm, "UTF-8").replaceAll("\\+", "%20");
        String disposition = "attachment;filename=" + fileNameOrg + ";";
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition", disposition)
            .body(staticFile);
    }
    
    @RequestMapping(value={ "upload", "upload/**" },
                    method={ RequestMethod.POST, RequestMethod.OPTIONS })
    public void process(final HttpServletRequest req, final HttpServletResponse res) throws IOException, TusException {
        uploadService.process(req, res);
    }
    
    @DeleteMapping({ "upload", "upload/**" })
    public void delete(final HttpServletRequest req, final HttpServletResponse res) throws IOException, TusException {
        uploadService.process(req, res);
        fileService.deleteFile(req);
    }
    
    @RequestMapping(value={ "upload", "upload/**" },
                    method={ RequestMethod.HEAD, RequestMethod.PATCH })
    public void upload(final HttpServletRequest req, final HttpServletResponse res) throws IOException, TusException {
        uploadService.process(req, res);
        fileService.saveFile(req);
    }
    
    @GetMapping({ "upload", "upload/**" })
    public void download(final HttpServletRequest req, final HttpServletResponse res) throws IOException, TusException {
        uploadService.process(req, res);
        fileService.download(req);
    }
}