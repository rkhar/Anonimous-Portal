import scala.collection.immutable.Stream.cons

def getStrategy(enoughEnergy: Boolean) =
    if (enoughEnergy)
      (energy: Double) => "We are going to attack with damage " + energy
    else
      (energy: Double) => "We are going to reflect damage " + energy / 2

val returnedFunction = getStrategy(true)

returnedFunction(15.0)

getStrategy(true)(1500)(0)

class C {
  var acc = 0
  val fun = () => acc + 1
}

val c = new C

c.fun
c.acc

val numbers = Set(5, 1, 2, 3, 6)

numbers

val tuple = ("aaa", 123, 2.2)

numbers.foreach(x => x * 2)

def ourMap(numbers: List[Int], fn: Int => Int): List[Int] =
    numbers.foldRight(List[Int]()) { (x: Int, xs: List[Int]) =>
      fn(x) :: xs
    }

ourMap(numbers.toList, x => x * 2)

List(1, 2).::(3)

1 +: List(2, 3) :+ 4

val one: PartialFunction[String, Int] = { case "one" => 1 }

one("one")

2 :: 1 :: 3 :: "asd" :: List()

val seq: Seq[Int] = Seq(1, 2, 3)

val days = List("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

days match {
    case firstDay :: otherDays =>
      println("The first day of the week is: " + firstDay)
    case Nil =>
      println("There don't seem to be any week days.")
}

val map = Map("1" -> 1, "2" -> 2, "3" -> 3, "4" -> 4, "5" -> 5, "6" -> 6, "7" -> 7)

val opt: Option[Int] = Some(2)
opt.fold(0)(x => x)

val lbd = (x: Int) => x + 1

def addWithSyntaxSugar(x: Int) = (y: Int) ⇒ x + y

def fiveAdder = addWithSyntaxSugar(5)

addWithSyntaxSugar(5)
fiveAdder(5)


val myMap =
  Map("MI" → "Michigan", "OH" → "Ohio", "WI" → "Wisconsin", "IA" → "Iowa")

myMap.getOrElse("MI", "missing data")

val c = 'a' //unicode for a
val d = '\141' //octal for a
val e = '\"'
val f = '\\'

println("%c".format(c))
println("%c".format(d))
println("%c".format(e))
println("%c".format(f))

val secondElement = List(1) match {
  case z :: y :: x :: xs ⇒ xs.headOption
  case _ ⇒ 0
}

case class Dog(name: String, breed: Int)
val d1 = Dog("Scooby", 2)

d1.toString

case class Person(first: String, last: String, age: Int = 0, ssn: String = "")
val p1 = Person("Fred", "Jones", 23, "111-22-3333")

val parts = Person.unapply(p1).get

val someNumbers = Range(0, 10)
val second = someNumbers(0)
val last = someNumbers.last

def multiply(x: Int, y: Int) = x * y
val multiplyCurried = (multiply _).curried
multiply(4, 5)
multiplyCurried(3)(2)
val multiplyCurriedFour = multiplyCurried(4)
multiplyCurriedFour(2)
multiplyCurriedFour(4)

val xValues = 1 to 4
val yValues = 1 to 2
val coordinates = for {
  x ← xValues
  y ← yValues
} yield (x, y)

coordinates(4)

val nums = List(List(1), List(2), List(3), List(4), List(5))

val g: Int = 31

g toHexString

val xs1 = Set(3, 2, 1, 4, 5, 6, 7)
val ys1 = Set(7, 2, 1, 4, 5, 6, 3)

val xt1 = Set(1, 2, 3, 4, 5)
val yt1 = Set(3, 2, 1, 4, 5)

yt1

def streamer(v: Int): Stream[Int] = cons(v, streamer(v + 1))
val a = streamer(1)
((a drop 1) take 3).toList

val intList = List(5, 4, 3, 2, 1)
intList.reduceRight((x, y) => x - y)
intList.reverse.reduceLeft((x, y) => x - y)
intList.reduce((x, y) => y - x)

val list2 = List(List(1), List(4))
list2.transpose

val stringBuilder = new StringBuilder()
val list = List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
stringBuilder.append("I want all numbers 6-12: ")
list.filter(it ⇒ it > 5 && it < 13).addString(stringBuilder, ",")
stringBuilder.mkString

val array = Array(87, 44, 5, 4, 200, 10, 39, 100)
val result = array splitAt 0
result._1

class WithoutClassParameters() {
  def addColors(red: Int, green: Int, blue: Int) = {
    (red, green, blue)
  }

  def addColorsWithDefaults(red: Int = 0, green: Int = 0, blue: Int = 0) = {
    (red, green, blue)
  }
}

class WithClassParameters(val defaultRed: Int, val defaultGreen: Int, val defaultBlue: Int) {
  def addColors(red: Int, green: Int, blue: Int) = {
    (red + defaultRed, green + defaultGreen, blue + defaultBlue)
  }

  def addColorsWithDefaults(red: Int = 0, green: Int = 0, blue: Int = 0) = {
    (red + defaultRed, green + defaultGreen, blue + defaultBlue)
  }
}

class WithClassParametersInClassDefinition(val defaultRed: Int = 0, val defaultGreen: Int = 255, val defaultBlue: Int = 100) {
  def addColors(red: Int, green: Int, blue: Int) = {
    (red + defaultRed, green + defaultGreen, blue + defaultBlue)
  }

  def addColorsWithDefaults(red: Int = 0, green: Int = 0, blue: Int = 0) = {
    (red + defaultRed, green + defaultGreen, blue + defaultBlue)
  }
}


val me = new WithClassParameters(40, 50, 60)
val myColor = me.addColors(green = 50, red = 60, blue = 40)

myColor

def reduce(a: Int, f: (Int, Int) ⇒ Int = _ + _): Int = f(a, a)

reduce(5)

"%c".format('a')
"%c".format('\141')
"%c".format('\"')
"%c".format('\\')

class Car(val make: String, val model: String, val year: Short, val topSpeed: Short)

object ChopShop {
  def unapply(x: Car) = Some(x.make, x.model, x.year, x.topSpeed)
}

val ChopShop(a, b, c, d) = new Car("Chevy", "Camaro", 1978, 120)
a
b
c
d

class Car(val make: String, val model: String, val year: Short, val topSpeed: Short)

object ChopShop {
  def unapply(x: Car) = Some(x.make, x.model, x.year, x.topSpeed)
}

val x = new Car("Chevy", "Camaro", 1978, 120) match {
  case ChopShop(s, t, u, v) ⇒ (s, t)
  case _ ⇒ ("Ford", "Edsel")
}

x._1

class Employee(
                val firstName: String,
                val middleName: Option[String],
                val lastName: String
              )

object Employee {
  //factory methods, extractors, apply
  //Extractor: Create tokens that represent your object
  def unapply(x: Employee) =
    Some(x.lastName, x.middleName, x.firstName)
}

val singri = new Employee("Singri", None, "Keerthi")

val Employee(a, b, c) = singri

object PigLatinizer {
  def apply(x: ⇒ String) = x.tail + x.head + "ay"
}

val result = PigLatinizer {
  val x = "pret"
  val z = "zel"
  x ++ z //concatenate the strings
}

def repeatedParameterMethod(x: Int, y: String, z: Any*) = {
  "%d %ss can give you %s".format(x, y, z.mkString(", "))
}

repeatedParameterMethod(3, "egg", List("a delicious sandwich", "protein", "high cholesterol"))

repeatedParameterMethod(
  3,
  "egg",
  List("a delicious sandwich", "protein", "high cholesterol"): _*
)

abstract class Soldier(val firstName: String, val lastName: String) {

  class Catch(val number: Long) {
    // nothing to do here.  Just observe that it compiles
  }

}
class Pilot(override val firstName: String, override val lastName: String, val squadron: Long)
  extends Soldier(firstName, lastName)

val pilot = new Pilot("John", "Yossarian", 256)
val catchNo = new pilot.Catch(2)
catchNo.number

trait Randomizer[A] {
  def draw(): A
}

class IntRandomizer extends Randomizer[Int] {
  def draw() = {
    import util.Random
    Random.nextInt()
  }
}

val intRand = new IntRandomizer
(intRand.draw <= Int.MaxValue)

classOf[String].getCanonicalName
classOf[String].getSimpleName

val a = 3.0
val b = 3.00
val c = 2.73
val d = 3f
val e = 3.22d
val f = 93e-9
val g = 93E-9
val h = 0.0
val i = 9.23E-9D

val e = '\"'
val f = '\\'
