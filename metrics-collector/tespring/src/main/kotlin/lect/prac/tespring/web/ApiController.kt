package lect.prac.tespring.web

import lect.prac.tespring.model.Counter
import lect.prac.tespring.model.CounterRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes


@RestController
class ApiController {

    @Autowired
    lateinit var repo: CounterRepository

    @GetMapping("/count")
    fun count() {
        val req = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
        var ip = req.getHeader("X-FORWARDED-FOR")
        if (ip == null) ip = req.remoteAddr
        repo.save(Counter(ip))
    }
}