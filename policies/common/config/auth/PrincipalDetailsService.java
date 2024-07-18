package kware.common.config.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PrincipalDetailsService implements UserDetailsService {

//    private final CetusUserService cetusUserService;
//    private final CetusAuthorUserInfoService authUserService;
//    private final CetusMenuInfoService cetusMenuInfoService;

    @Override
    public UserDetails loadUserByUsername(String userId) {
//        CetusUser user = cetusUserService.findPassword(new CetusUser(userId));
        CetusUser user = new CetusUser(userId, "1234", "ADMIN");
        if (user != null) {
//            CetusAuthorUserInfo authorUserInfo = authUserService.view(new CetusAuthorUserInfo(user.getUid()));
//            List<CetusMenuInfo> menuInfoList = cetusMenuInfoService.list(new CetusMenuInfo(authorUserInfo.getAuthorCd()));
//
//            List<String> urls = menuInfoList.stream().map(CetusMenuInfo::getUrl).collect(Collectors.toList());
//
//            Optional<CetusMenuInfo> rootMenu = menuInfoList.stream().filter(menuInfo -> menuInfo.getUpperMenuNo() == null).findFirst();
//            Long rootMenuNo = rootMenu.isPresent() ? rootMenu.get().getMenuNo() : 1L;
//
//            List<CetusMenu> menus = menuInfoList.stream().filter(menuInfo -> menuInfo.getUpperMenuNo() != null && menuInfo.getUpperMenuNo().equals(rootMenuNo)).map(CetusMenu::new).sorted(Comparator.comparingInt(CetusMenu::getSortNo)).collect(Collectors.toList());
//
//            menuInfoList.stream().sorted(Comparator.comparingInt(CetusMenuInfo::getDepth)).filter(menuInfo -> menuInfo.getUpperMenuNo() != null && !menuInfo.getUpperMenuNo().equals(rootMenuNo)).forEach(menuInfo -> {
//                Optional<CetusMenu> parent = menus.stream().filter(maybeParent -> maybeParent.getMenuNo().equals(menuInfo.getUpperMenuNo())).findFirst();
//                parent.ifPresent(cetusMenu -> {
//                    cetusMenu.getChildren().add(new CetusMenu(menuInfo));
//                    cetusMenu.getChildren().sort(Comparator.comparingInt(CetusMenu::getSortNo));
//                });
//            });
//
//            user.setRole(authorUserInfo.getAuthorCd());
//            user.setAuthorizedMenuUrls(urls);
//            user.setMenus(menus);

            return new PrincipalDetails(user);
        } else {
            throw new UsernameNotFoundException(userId);
        }
    }
}
