package lect.prac.tespring.web

import lect.prac.tespring.model.CounterRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class MainController {

    @Autowired
    lateinit var repo: CounterRepository

    @GetMapping("/")
    fun index(model: Model): String {
        model.addAttribute("count", repo.count())
        return "index"
    }
}