import pdi.jwt.{Jwt, JwtAlgorithm}
import org.json4s._
import org.json4s.native.JsonMethods._
implicit val formats = DefaultFormats

//import java.security.KeyPairGenerator
//import org.bouncycastle.jce.provider.BouncyCastleProvider
//import java.security.spec.{ECGenParameterSpec, ECParameterSpec, ECPoint, ECPrivateKeySpec, ECPublicKeySpec}
//import java.security.{KeyFactory, KeyPairGenerator, SecureRandom, Security}
//import pdi.jwt.{Jwt, JwtAlgorithm}
//import scala.util.{Failure, Success}
//
//val token: String =
//    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJwaG9uZU51bWJlciI6IjMxMjMxMjMxMjMiLCJwYXNzd29yZCI6InNlY3JldHBhc3N3b3JkIn0.Ut0AJGB2GBrChVvrhmBxiM7GNkv-v2Cr5QFbOKM9ssg"
//val phoneNumber: String = "3123123123"
//
//val key = "secretKey"
//
//val ecGenSpec = new ECGenParameterSpec("P-521")
//if(Security.getProvider("BC") == null) {
//    Security.addProvider(new BouncyCastleProvider())
//  }
//val generatorEC = KeyPairGenerator.getInstance("ECDSA", "BC")
//val ecKey = generatorEC.generateKeyPair()

case class User(user: String)

val enc = Jwt.encode("""{"user":1}""", "secretKey", JwtAlgorithm.HS256)

val mp = Map("Authorization" -> "Bearer asd")

val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJwaG9uZU51bWJlciI6IjMxMjMxMjMxMjMiLCJwYXNzd29yZCI6InNlY3JldHBhc3N3b3JkIn0.Ut0AJGB2GBrChVvrhmBxiM7GNkv-v2Cr5QFbOKM9ssgeyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJwaG9uZU51bWJlciI6IjMxMjMxMjMxMjMiLCJwYXNzd29yZCI6InNlY3JldHBhc3N3b3JkIn0.Ut0AJGB2GBrChVvrhmBxiM7GNkv-v2Cr5QFbOKM9ssg"

//val token = mp.getOrElse("Authorization2", "")
//val dec = Jwt.decode(token,"secretKey", Seq(JwtAlgorithm.HS256)).map(x => parse(x.content).extract[User])
val dec = Jwt.decode(token,"secretKey", Seq(JwtAlgorithm.HS256)).isSuccess
