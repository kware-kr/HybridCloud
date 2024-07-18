package kware.common.file.extension;

import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.exception.UploadAlreadyLockedException;
import me.desair.tus.server.upload.UploadId;
import me.desair.tus.server.upload.UploadIdFactory;
import me.desair.tus.server.upload.UploadLock;
import me.desair.tus.server.upload.UploadLockingService;
import me.desair.tus.server.upload.disk.AbstractDiskBasedService;
import me.desair.tus.server.upload.disk.FileBasedLock;
import me.desair.tus.server.util.Utils;
import org.apache.commons.lang3.Validate;

import java.io.File;
import java.io.IOException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * 빠르게 재요청했을 때 lock이 풀리지 않았을 경우를 대비해 1.5초 sleep함
 */
public class LazyDiskLockingService extends AbstractDiskBasedService implements UploadLockingService {
    
    private static final String LOCK_SUB_DIRECTORY = "locks";
    private static final long LOCK_FILE_WAIT_SLEEP_TIME = 1500;
    
    private UploadIdFactory idFactory;
    
    public LazyDiskLockingService(String storagePath) {
        super(storagePath + File.separator + LOCK_SUB_DIRECTORY);
    }
    
    @Override
    public UploadLock lockUploadByUri(String requestURI) throws TusException, IOException {
        
        UploadId id = idFactory.readUploadId(requestURI);
        
        UploadLock lock = null;
        
        Path lockPath = getLockPath(id);
        
        //If lockPath is not null, we know this is a valid Upload URI
        if ( lockPath != null ) {
            lock = getLockLazy(requestURI, lockPath);
        }
        return lock;
    }
    
    @Override
    public void cleanupStaleLocks() throws IOException {
        try (DirectoryStream<Path> locksStream = Files.newDirectoryStream(getStoragePath())) {
            for ( Path path : locksStream ) {
                
                FileTime lastModifiedTime = Files.getLastModifiedTime(path);
                if ( lastModifiedTime.toMillis() < System.currentTimeMillis() - 10000L ) {
                    UploadId id = new UploadId(path.getFileName().toString());
                    
                    if ( !isLocked(id) ) {
                        Files.deleteIfExists(path);
                    }
                }
                
            }
        }
    }
    
    @Override
    public boolean isLocked(UploadId id) {
        boolean locked = false;
        Path lockPath = getLockPath(id);
        
        if ( lockPath != null ) {
            //Try to obtain a lock to see if the upload is currently locked
            try (UploadLock lock = getLockLazy(id.toString(), lockPath)) {
                
                //We got the lock, so it means no one else is locking it.
                locked = false;
                
            }
            catch (UploadAlreadyLockedException | IOException e) {
                //There was already a lock
                locked = true;
            }
        }
        
        return locked;
    }
    
    @Override
    public void setIdFactory(UploadIdFactory idFactory) {
        Validate.notNull(idFactory, "The IdFactory cannot be null");
        this.idFactory = idFactory;
    }
    
    private Path getLockPath(UploadId id) {
        return getPathInStorageDirectory(id);
    }
    
    
    private FileBasedLock getLockLazy(String requestURI, Path lockPath) throws UploadAlreadyLockedException, IOException {
        try {
            return new FileBasedLock(requestURI, lockPath);
        }
        catch (OverlappingFileLockException | UploadAlreadyLockedException e) {
            Utils.sleep(LOCK_FILE_WAIT_SLEEP_TIME);
            return new FileBasedLock(requestURI, lockPath);
        }
    }
}
