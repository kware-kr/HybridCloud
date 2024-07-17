package kware.app;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TempController {

    @GetMapping("/")
    String index() {
        return "/index";
    }

    @GetMapping("/login")
    String login() {
        return "/login";
    }
}
