package kz.anon.portal.signup.login.api.mainapi

import akka.actor.Props
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import kz.anon.portal.signup.login.api.model.SignUpModel
import kz.anon.portal.signup.login.api.serializer.Json4sSerializer
import kz.anon.portal.signup.login.api.service.ParentService
import kz.anon.portal.signup.login.api.service.RegistrationActor.Registration

trait MainApi extends ParentService with Json4sSerializer{

  def registrationProps: Props

  lazy val mainRoutes: Route = {
    concat(
      pathPrefix("signup") {
        post {
          entity(as[SignUpModel]) { signUpModel =>
            complete {
              (createSomeActor(registrationProps) ! Registration(signUpModel.firstName,
                                                                 signUpModel.lastName,
                                                                 signUpModel.age,
                                                                 signUpModel.password))
              HttpResponse(StatusCodes.OK, entity = "Data received")
            }
          }
        }
      }~
      pathPrefix("healthcheck"){
        get{
          complete(
            HttpResponse(StatusCodes.OK, entity = "Got ya again!")
          )
        }
      }
    )

  }

}
