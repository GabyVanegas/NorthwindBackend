package com.example

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
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