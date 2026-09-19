package routes

/**
 * Exercise 1 / 4: a route is a class.
 *
 * Declare `Greeting`, a `@Serializable @Resource("/greet/{name}")` class with
 * a `name: String` and a `lang: String? = "en"`, and inside it the nested
 * `@Serializable @Resource("bye") class Bye(val parent: Greeting)`.
 *
 * Exercise 2 / 4: nest the hour.
 *
 * Add the nested `@Serializable @Resource("hello/{hour}") class Hello` with
 * a `parent: Greeting` and an `hour: Int`: `/greet/ada/hello/9` matches,
 * `/greet/ada/hello/nine` does not.
 */
