package com.lm

import org.http4k.core.*
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.lens.int
import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.http4k.lens.Path
import org.http4k.lens.string
import org.http4k.routing.RoutingHttpHandler


fun main() {

    CarVehicle.stageRecords()

    data class CarLocal(val id: Int, val make: String, val model: String)

    val carsLocalList = mutableListOf(
        CarLocal(1, "Nissan", "350z"),
        CarLocal(2, "BMW", "i8"),
        CarLocal(3, "Chevrolet", "Trax"),
        CarLocal(4, "Hyundai", "Sante Fe")
    )

    fun localListRoute(): RoutingHttpHandler {
        val listCarLens = Body.auto<List<CarLocal>>().toLens()
        val carLens = Body.auto<CarLocal>().toLens()
        val idLens = Path.int().of("id")

        return routes(
            "/cars" bind Method.GET to { request: Request -> listCarLens(carsLocalList, Response(Status.OK)) },
            "/car" bind Method.POST to { request: Request ->
                val car = carLens(request)
                carsLocalList.add(car)
                Response(Status.ACCEPTED).header("location", carsLocalList.size.toString()).with(carLens of car)
            },
            "/car/{id}" bind Method.GET to { request ->
                val id = idLens(request)
                carLens(carsLocalList.filter { it.id == id }.get(0), Response(Status.OK))
            },
            "/car/{id}" bind Method.DELETE to { request ->
                val id = idLens(request)
                carsLocalList.remove(carsLocalList.filter { it.id == id }.get(0))
                Response(Status.OK)
            }
        )
    }

    fun routesUsingVehicleDb(): RoutingHttpHandler {
        val listCarLens = Body.auto<List<Vehicle>>().toLens()
        val carLens = Body.auto<Vehicle>().toLens()
        val idLens = Path.string().of("id")

        return routes(
            "/cars" bind Method.GET to { request: Request ->
                try {
                    listCarLens(CarVehicle.selectAllVehicles(), Response(Status.OK))
                } catch (exc: Exception) {
                    Response(Status.NOT_ACCEPTABLE).body(exc.toString())
                }
            },
            "/car" bind Method.POST to { request: Request ->
                try {
                    val car: Vehicle = carLens(request)
                    car.insert()
                    Response(Status.ACCEPTED).header("location", carsLocalList.size.toString()).with(carLens of car)
                } catch(exc :Exception) {
                    Response(Status.NOT_ACCEPTABLE).body(exc.toString())
                }

            },
            "/car/{id}" bind Method.GET to { request ->
                try {
                    val id = idLens(request)
                    carLens(CarVehicle.selectSingle(id), Response(Status.OK))
                } catch (exc: Exception) {
                    Response(Status.NOT_ACCEPTABLE).body(exc.toString())
                }
            }
            ,
            "/car/{id}" bind Method.DELETE to { request ->
                try {
                    val id = idLens(request)
                    CarVehicle.deleteCar(id)
                    Response(Status.OK)
                } catch (exc: Exception) {
                    Response(Status.NOT_ACCEPTABLE).body(exc.toString())
                }
            }
        )
    }

    val routeCombined = routes(
        localListRoute(),
        "/db" bind routesUsingVehicleDb()
    )

    routeCombined.asServer(SunHttp(8080)).with()
        .start() // This makes it into a SunHttp Server bound to port 8080 and starts it

    println(routeCombined(Request(Method.GET,"/db/cars")))

}

