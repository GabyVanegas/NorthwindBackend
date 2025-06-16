package com.example

import com.example.Orders.orderId
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.event.*

fun Application.configureRouting() {
    routing {
        //Get /customer?
        get("/customers"){

            val countryFilter = call.request.queryParameters["country"]
            //ejecucion de consulta
            val custumers = transaction {
                Customers
                    .select { countryFilter?.let { Customers.country eq it } ?: Op.TRUE }
                    .orderBy(Customers.contactName, SortOrder.ASC)
                    .map { row ->
                        CustomerDto(
                            id  = row[Customers.customerId],
                            companyName  = row[Customers.companyName],
                            contactName  = row[Customers.contactName],
                            country      = row[Customers.country]
                        )
                    }
            }
            call.respond(HttpStatusCode.OK, custumers)
        }

        //Get /customers/id/orders
        get("/customer/{id}/orders"){
            val id = call.parameters["id"]
            if(id.isNullOrBlank()) {
                return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")
            }

            val orders = transaction {
                Orders
                    .join(Customers, JoinType.INNER, additionalConstraint = { Orders.customerId eq Customers.customerId })
                    .slice(Orders.orderId, Orders.orderDate, Orders.shippedDate)
                    .select { Customers.customerId eq id }
                    .orderBy(Orders.shippedDate, SortOrder.ASC)
                    .map { row ->
                        OrderDto(
                            orderId     = row[Orders.orderId],
                            orderDate   = row[Orders.orderDate]?.toString(),
                            shippedDate = row[Orders.shippedDate]?.toString()
                        )

                    }

            }
            call.respond(HttpStatusCode.OK, orders)
        }

        // GET /orders/{id} → verificar si una orden existe
        get("/orders/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) return@get call.respond(HttpStatusCode.BadRequest, "ID inválido")

            val order = transaction {
                Orders.select { Orders.orderId eq id }.singleOrNull()
            }

            if (order == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(HttpStatusCode.OK, mapOf("exists" to true))
            }
        }
        get("/customers/{id}") {
            val id = call.parameters["id"]
            if (id.isNullOrBlank()) return@get call.respond(HttpStatusCode.BadRequest, "ID inválido")

            val customer = transaction {
                Customers.select { Customers.customerId eq id }.singleOrNull()
            }

            if (customer == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(HttpStatusCode.OK, mapOf("exists" to true))
            }
        }
        // Crear cliente
        post("/customers") {
            val customer = call.receive<CustomerDto>()

            transaction {
                Customers.insert {
                    it[customerId] = customer.id
                    it[companyName] = customer.companyName
                    it[contactName] = customer.contactName
                    it[country] = customer.country
                }
            }

            call.respond(HttpStatusCode.Created, "Cliente creado exitosamente")
        }

        // Editar cliente
        put("/customers/{id}") {
            val id = call.parameters["id"]
            if (id.isNullOrBlank()) return@put call.respond(HttpStatusCode.BadRequest, "ID inválido")

            val customer = call.receive<CustomerDto>()

            transaction {
                Customers.update({ Customers.customerId eq id }) {
                    it[companyName] = customer.companyName
                    it[contactName] = customer.contactName
                    it[country] = customer.country
                }
            }

            call.respond(HttpStatusCode.OK, "Cliente actualizado exitosamente")
        }

        // Eliminar cliente
        delete("/customers/{id}") {
            val id = call.parameters["id"]
            if (id.isNullOrBlank()) return@delete call.respond(HttpStatusCode.BadRequest, "ID inválido")

            // Verificar si tiene órdenes
            val hasOrders = transaction {
                Orders.select { Orders.customerId eq id }.any()
            }

            if (hasOrders) {
                return@delete call.respond(HttpStatusCode.Conflict, "No se puede eliminar el cliente: tiene órdenes asociadas")
            }

            transaction {
                Customers.deleteWhere { customerId eq id }
            }

            call.respond(HttpStatusCode.OK, "Cliente eliminado exitosamente")
        }

        post("/orders") {
            val newOrder = call.receive<OrderPostDto>()

            val exists = transaction {
                Customers.select { Customers.customerId eq newOrder.customerId }.count() > 0
            }

            if (!exists) return@post call.respond(HttpStatusCode.NotFound, "Customer not found")

            transaction {
                Orders.insert {
                    it[orderId] = newOrder.orderId
                    it[customerId] = newOrder.customerId
                    it[orderDate] = newOrder.orderDate?.toLocalDateTime()
                    it[shippedDate] = newOrder.shippedDate?.toLocalDateTime()
                }
            }
            call.respond(HttpStatusCode.Created, "Order created successfully")
        }

        put("/orders/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) return@put call.respond(HttpStatusCode.BadRequest, "Invalid ID")

            val updated = call.receive<OrderPostDto>()

            transaction {
                Orders.update({ Orders.orderId eq id }) {
                    it[customerId] = updated.customerId
                    it[orderDate] = updated.orderDate?.toLocalDateTime()
                    it[shippedDate] = updated.shippedDate?.toLocalDateTime()
                }
            }
            call.respond(HttpStatusCode.OK, "Order updated successfully")
        }

        delete("/orders/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) return@delete call.respond(HttpStatusCode.BadRequest, "Invalid ID")

            transaction {
                Orders.deleteWhere { Orders.orderId eq id }
            }
            call.respond(HttpStatusCode.OK, "Order deleted successfully")
        }

    }


}

// DTOs para serializar cómodamente a JSON
@kotlinx.serialization.Serializable
data class CustomerDto(
    val id : String,
    val companyName : String,
    val contactName : String?,
    val country : String?,
)
@kotlinx.serialization.Serializable
data class OrderDto(
    val orderId : Int,
    val orderDate : String?,
    val shippedDate : String?
)

@kotlinx.serialization.Serializable
data class OrderPostDto(
    val orderId: Int,
    val customerId: String,
    val orderDate: String?,
    val shippedDate: String?
)