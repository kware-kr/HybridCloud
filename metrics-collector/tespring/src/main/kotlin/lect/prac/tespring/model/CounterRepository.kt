package lect.prac.tespring.model

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CounterRepository : CrudRepository<Counter, Long> {
}