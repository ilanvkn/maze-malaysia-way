package my.maze.malaysia

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

data class GeoPoint(val lat: Double, val lon: Double) {
    init { require(lat.isFinite() && lon.isFinite() && lat in -85.0..85.0 && lon in -180.0..180.0) }
}
data class Place(val id: String, val name: String, val area: String, val point: GeoPoint)
data class RoadStep(val text: String, val meters: Double)
data class RoadRoute(val geometry: List<GeoPoint>, val steps: List<RoadStep>, val meters: Double, val seconds: Double, val summary: String)
val landmarks = listOf(
    Place("sentral", "KL Sentral", "Kuala Lumpur", GeoPoint(3.1341,101.6869)),
    Place("klcc", "Petronas Twin Towers", "Kuala Lumpur · KLCC", GeoPoint(3.15785,101.71165)),
    Place("merdeka", "Dataran Merdeka", "Kuala Lumpur", GeoPoint(3.1478,101.6937)),
    Place("batu", "Batu Caves", "Selangor", GeoPoint(3.2379,101.6831)),
    Place("putra", "Putrajaya", "Federal Territory", GeoPoint(2.9264,101.6964)),
    Place("penang", "George Town", "Penang", GeoPoint(5.4141,100.3288)),
    Place("kuching", "Kuching Waterfront", "Sarawak", GeoPoint(1.5574,110.3500)),
    Place("kk", "Kota Kinabalu", "Sabah", GeoPoint(5.9804,116.0735))
)
fun inMalaysiaRegion(p: GeoPoint) = p.lat in 0.5..8.0 && p.lon in 99.0..120.0
fun localSearch(query: String): List<Place> {
    val q=query.trim()
    return landmarks.filter { (it.name + " " + it.area).contains(q, ignoreCase=true) || (it.id=="klcc" && q.equals("klcc",true)) }
}
fun parseRoutes(json: String): List<RoadRoute> {
    val root=JSONObject(json)
    require(root.optString("code")=="Ok") { "The road service found no route." }
    val routes=root.getJSONArray("routes")
    require(routes.length() in 1..5) { "No usable road route returned." }
    return (0 until routes.length()).map { index ->
        val r=routes.getJSONObject(index)
        val coords=r.getJSONObject("geometry").getJSONArray("coordinates")
        require(coords.length() in 2..50000) { "Invalid road geometry." }
        val geometry=(0 until coords.length()).map { i ->
            val c=coords.getJSONArray(i); require(c.length()>=2)
            GeoPoint(c.getDouble(1),c.getDouble(0))
        }
        val meters=r.getDouble("distance"); val seconds=r.getDouble("duration")
        require(meters.isFinite() && seconds.isFinite() && meters>0 && seconds>0)
        val steps=mutableListOf<RoadStep>(); val summaries=mutableListOf<String>()
        val legs=r.getJSONArray("legs")
        for(l in 0 until legs.length()) {
            val leg=legs.getJSONObject(l); summaries+=leg.optString("summary").take(180)
            val rows=leg.getJSONArray("steps")
            require(rows.length()<=2000)
            for(s in 0 until rows.length()) {
                val row=rows.getJSONObject(s); val m=row.getJSONObject("maneuver")
                val action=when(m.optString("type")) {
                    "depart" -> "Depart"; "arrive" -> "Arrive at destination"
                    "roundabout", "rotary" -> "At the roundabout, take exit " + m.optInt("exit",1)
                    else -> (m.optString("modifier").replaceFirstChar { it.uppercase() }.ifBlank { "Continue" })
                }
                val road=row.optString("name").take(180)
                val distance=row.optDouble("distance",0.0)
                require(distance.isFinite() && distance>=0)
                steps+=RoadStep(action + if(road.isBlank()) "" else " · " + road,distance)
            }
        }
        RoadRoute(geometry,steps.take(500),meters,seconds,summaries.filter{it.isNotBlank()}.joinToString(" · ").ifBlank { "Road route" })
    }
}
fun parsePlaces(json: String): List<Place> {
    val rows=JSONArray(json); require(rows.length()<=20)
    return (0 until rows.length()).mapNotNull { i ->
        runCatching {
            val r=rows.getJSONObject(i); val p=GeoPoint(r.getDouble("lat"),r.getDouble("lon"))
            if(!inMalaysiaRegion(p)) return@runCatching null
            val display=r.optString("display_name",r.optString("name")).take(300)
            Place("online-"+r.optString("place_id",r.optString("id")),display.substringBefore(',').take(100),r.optString("area",display.substringAfter(',',"Malaysia").trim()),p)
        }.getOrNull()
    }.take(5)
}
fun routeDistanceLabel(meters: Double) = if(meters>=1000) String.format(java.util.Locale.ROOT,"%.1f km",meters/1000) else meters.roundToInt().toString()+" m"
fun routeMinutes(seconds: Double) = max(1,ceil(seconds/60).toInt())
