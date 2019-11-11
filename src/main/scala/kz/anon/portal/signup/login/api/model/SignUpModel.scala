package kz.anon.portal.signup.login.api.model

import java.time.LocalDate

case class SignUpModel(firstName: String, lastName: String, age: Int, password: String) extends CommonEntity
