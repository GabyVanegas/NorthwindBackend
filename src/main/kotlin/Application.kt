package com.example

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {

    // 1) Leemos la configuración de la BD
    val dbConfig = environment.config.config("ktor.database")
    val hikariConfig = HikariConfig().apply {
        jdbcUrl            = dbConfig.property("jdbcUrl").getString()
        driverClassName    = dbConfig.property("driver").getString()
        username           = dbConfig.property("user").getString()
        password           = dbConfig.property("password").getString()
        maximumPoolSize    = dbConfig.property("maximumPoolSize").getString().toInt()
    }
    val dataSource = HikariDataSource(hikariConfig)

    // 2) Conectamos Exposed al dataSource
    Database.connect(dataSource)

    // 3) Ahora sí podemos crear tablas
    //transaction {
        //SchemaUtils.createMissingTablesAndColumns(
            //Customers,
            //Orders,
            //WebTracker
        //)
    //}


    configureHTTP()
    configureSerialization()
    configureMonitoring()
    configureRouting()


}
