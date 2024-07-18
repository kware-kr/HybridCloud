package kware.common.file.extension;

import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.RequestHandler;
import me.desair.tus.server.RequestValidator;
import me.desair.tus.server.download.DownloadOptionsRequestHandler;
import me.desair.tus.server.util.AbstractTusExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 기존 다운로드 확장에서 파일명 한글 깨짐 수정
 */
public class DownloadExtension extends AbstractTusExtension {

    @Override
    public String getName() {
        return "download";
    }

    @Override
    public Collection<HttpMethod> getMinimalSupportedHttpMethods() {
        return Arrays.asList(HttpMethod.OPTIONS, HttpMethod.GET);
    }

    @Override
    protected void initValidators(List<RequestValidator> requestValidators) {
        //All validation is all read done by the Core protocol
    }

    @Override
    protected void initRequestHandlers(List<RequestHandler> requestHandlers) {
        requestHandlers.add(new DownloadGetRequestHandler());
        requestHandlers.add(new DownloadOptionsRequestHandler());
    }
}
