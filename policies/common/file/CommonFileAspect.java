package kware.common.file;

import cetus.annotation.CetusFile;
import cetus.bean.CetusBean;
import cetus.exception.ResponseException;
import cetus.support.Reflector;
import cetus.user.SessionUserUtil;
import kware.common.config.auth.CetusUser;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;

@RequiredArgsConstructor
@Aspect
@Order(1)
@Component
public class CommonFileAspect {

    private final CommonFileService fileService;

    @Before("@annotation(org.springframework.web.bind.annotation.GetMapping) || " + "@annotation(org.springframework.web.bind.annotation.PostMapping) || " + "@annotation(org.springframework.web.bind.annotation.PutMapping) || " + "@annotation(org.springframework.web.bind.annotation.PatchMapping) || " + "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    private void preMapping(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof CetusBean) {
                List<Field> fields = Reflector.getAnnotationFields(arg, CetusFile.class);
                for (Field field : fields) {
                    setFileUid((CetusBean) arg, field.getName());
                }
            }
        }
    }

    private void setFileUid(CetusBean bean, String fieldName) {
        if (bean.getFileAdd() == null && bean.getFileDel() == null) {
            return;
        }

        Long uid = Reflector.getValue(bean, fieldName);
        if (uid == null) {
            uid = fileService.generateUid();
            Reflector.setValue(bean, fieldName, uid);
        }

        CommonFile file = new CommonFile();
        file.setUid(uid);

        try {
            CetusUser user = (CetusUser) SessionUserUtil.getUser();
            String regId = user.getUserId();
            file.setRegId(regId);
        } catch (Exception e) {
            throw ResponseException.UN_AUTHORIZED;
        }

        if (bean.getFileAdd() != null) {
            for (String add : bean.getFileAdd()) {
                file.setUrl(add);
                fileService.changeSaved(file);
            }
        }

        if (bean.getFileDel() != null) {
            for (String del : bean.getFileDel()) {
                file.setUrl(del);
                fileService.delete(file);
            }
        }
    }
}
