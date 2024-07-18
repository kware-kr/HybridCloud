package kware.common.config.auth;

import cetus.user.SessionUser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CetusUser implements SessionUser {
    private String userId;
    private String password;
    private String role;
}
