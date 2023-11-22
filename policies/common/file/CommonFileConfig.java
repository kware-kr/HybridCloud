package kware.common.file;

import kware.common.file.extension.DownloadExtension;
import kware.common.file.extension.LazyDiskLockingService;
import me.desair.tus.server.TusFileUploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonFileConfig {
    
    @Value("${tus.server.storage_path:../file}")
    protected String tusDataPath;
    
    @Value("${tus.server.upload_uri:/cetus/files/upload}")
    protected String uploadURI;
    
    @Bean
    public TusFileUploadService TusFileUploadService() {
        return new TusFileUploadService()
            .withUploadURI(uploadURI)
            .withThreadLocalCache(true)
            .withUploadLockingService(new LazyDiskLockingService(tusDataPath))
            .addTusExtension(new DownloadExtension());
    }
}