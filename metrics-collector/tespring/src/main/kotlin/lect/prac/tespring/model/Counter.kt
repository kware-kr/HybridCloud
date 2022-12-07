package lect.prac.tespring.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.sql.Date

@Entity
class Counter(
    val ip: String,
    val dateCreated: Date = Date(System.currentTimeMillis()),
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = -1
) {
    constructor() : this("") {}
}