package chat

import io.ktor.http.ContentType
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.openapi.hide
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Route.docs() {
  swaggerUI("/swagger") {
    info = OpenApiInfo("Greeting API", "1.0.0")
    source = OpenApiDocSource.Routing(ContentType.Application.Json)
    remotePath = "openapi.json"
  }
  get("/health") { call.respondText("ok") }.hide()
}
