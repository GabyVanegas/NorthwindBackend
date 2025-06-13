package com.example

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

//Mapeo de tabla Customers
object Customers : Table("Customers") {
    val customerId  : Column<String> = varchar("CustomerID", 5)
    val companyName : Column<String> = varchar("CompanyName", 40)
    val contactName : Column<String?> = varchar("ContactName", 30).nullable()
    val country     : Column<String?> = varchar("Country", 15).nullable()

    // Definimos la PK de forma explícita:
    override val primaryKey = PrimaryKey(customerId, name = "PK_Customers")
}
//Mapeo de tabla Orders
object Orders: Table("Orders"){
    val orderId     = integer("OrderID")
    val customerId  = reference("CustomerID", Customers.customerId)
    val orderDate   = datetime("OrderDate").nullable()
    val shippedDate = datetime("ShippedDate").nullable()

    override val primaryKey = PrimaryKey(orderId, name = "PK_Orders")
}

//Tabla para rgistrar las peticiones
object WebTracker : Table("WebTracker"){
    val trackerid          = integer("id")
    val path                 = varchar("path", 255)
    val method               = varchar("method", 10)
    val timestamp            = datetime("timestamp")
    val ip                   = varchar("ip", 45)

    override val primaryKey = PrimaryKey(trackerid, name = "PK_WebTracker")
}