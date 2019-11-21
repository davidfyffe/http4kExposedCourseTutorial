package com.lm

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

data class Vehicle(val make : String, val model :String, val id : String = UUID.randomUUID().toString()) {
    fun insert() {
        transaction {
            CarVehicle.insert {
                it[MAKE] = make
                it[MODEL] = model
                it[ID] = id
            }
        }

    }
}

object CarVehicle : Table() {

    fun stageRecords() {
        val vehicles = listOf(
            Vehicle("Nissan", "350z"),
            Vehicle("BMW", "i8"),
            Vehicle("Chevrolet", "Trax"),
            Vehicle("Hyundai", "Sante Fe")
        )

        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(CarVehicle) // This creates the Car table
            vehicles.forEach { it.insert() }  // Inserts from a list of cars we have from http4k
            selectAllVehicles().forEach(::println) // Selects all cars from the DB
        }
    }

    val ID = CarVehicle.varchar("id", length = 36).primaryKey()
    val MAKE = CarVehicle.varchar("make", length = 50)
    val MODEL = CarVehicle.varchar("model", length = 50)

    fun selectAllVehicles(): List<Vehicle> {
        return transaction {
            CarVehicle.selectAll().map {
                Vehicle(make = it[MAKE], model = it[MODEL], id = it[ID])
            }
        }
    }

    fun selectSingle(id: String): Vehicle {
        return transaction {
            CarVehicle.select { ID eq id }
                .map { Vehicle(make = it[MAKE], model = it[MODEL], id = it[ID]) }
                .get(0)
        }

    }

    fun deleteCar(id: String) {
        return transaction {
            CarVehicle.deleteWhere { ID eq id }
        }
    }

}