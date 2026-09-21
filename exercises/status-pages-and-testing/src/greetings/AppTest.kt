package greetings

import io.ktor.client.HttpClient

/**
 * Exercise 2 / 5: a helper owns the set-up.
 *
 * `appTest` is `testApplication { }` with the set-up every test repeats:
 * `application { module() }` and a client from `createClient { }` with
 * `ContentNegotiation` (`json()`) installed, which it hands to [test]. The tests in `GreetingTest` are then only the test.
 *
 * Exercise 3 / 5: generated input finds the corner cases.
 *
 * `GreetingTest.anyName` greets every name `checkAll(Arb.string())` comes up
 * with. Run it, look at what it finds, then decide: is the failing input a
 * bug in the server or a name that cannot be a path segment? Narrow the
 * generator or fix the route accordingly; see README.md.
 */
fun appTest(test: suspend (HttpClient) -> Unit): Unit = TODO()
