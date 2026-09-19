package greetings

import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.ktor.client.plugins.resources.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

class GreetingTest {
  @Test
  fun bye() = appTest { client ->
    val response = client.get(Greeting.Bye(Greeting("ada")))
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals("Bye, ada", response.bodyAsText())
  }

  @Test
  fun hello() = appTest { client ->
    val response = client.get(Greeting.Hello(Greeting("linus"), hour = 9))
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals("Hello, linus, it's 9 o'clock", response.bodyAsText())
  }

  @Test
  fun tooEarly() = appTest { client ->
    assertEquals(HttpStatusCode.TooEarly, client.get(Greeting.Hello(Greeting("grace"), hour = 3)).status)
  }

  // Exercise 3 / 5: `Arb.string()` finds the empty name within a few dozen
  // attempts (`/greet//bye` matches no route: 404). Widen the generator to
  // `Arb.string()` and decide for yourself whether that is a bug in `bye`.
  @Test
  fun anyName() = appTest { client ->
    checkAll(Arb.string(minSize = 1)) { name ->
      assertEquals(HttpStatusCode.OK, client.get(Greeting.Bye(Greeting(name))).status, "name: '$name'")
    }
  }
}
