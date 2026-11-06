package my.maze.malaysia
import org.junit.Test
import org.junit.Assert.*
class MazeDataTest {
 @Test fun searchHandlesCaseWhitespaceAndAlias(){assertEquals("klcc",localSearch(" KLCC ").single().id);assertTrue(localSearch("sarawak").any{it.id=="kuching"});assertTrue(localSearch("zz-913-no-place").isEmpty())}
 @Test fun invalidCoordinatesAreRejected(){for(pair in listOf(Double.NaN to 101.0,3.0 to Double.POSITIVE_INFINITY,89.0 to 101.0)){assertTrue(runCatching{GeoPoint(pair.first,pair.second)}.isFailure)}}
 @Test fun routesRequireRoadGeometryAndServiceSuccess(){for(json in listOf("""{"code":"NoRoute","routes":[]}""","""{"code":"Ok","routes":[{"distance":100,"duration":20,"geometry":{"coordinates":[[101,3]]},"legs":[]}]}""")){assertTrue(runCatching{parseRoutes(json)}.isFailure)}}
 @Test fun actualRoadShapeAndStepsArePreserved(){val r=parseRoutes("""{"code":"Ok","routes":[{"distance":1200,"duration":180,"geometry":{"coordinates":[[101.6,3.1],[101.7,3.2],[101.8,3.3]]},"legs":[{"summary":"Jalan Example","steps":[{"distance":300,"name":"Jalan Example","maneuver":{"type":"turn","modifier":"left"}}]}]}]}""").single();assertEquals(3,r.geometry.size);assertEquals(GeoPoint(3.1,101.6),r.geometry.first());assertEquals("Left · Jalan Example",r.steps.single().text);assertEquals("1.2 km",routeDistanceLabel(r.meters))}
 @Test fun searchFiltersNonRegionalResultsAndBoundsResults(){val p=parsePlaces("""[{"place_id":1,"lat":"3.15","lon":"101.7","display_name":"KLCC, Kuala Lumpur, Malaysia"},{"place_id":2,"lat":"51.5","lon":"-0.1","display_name":"London"}]""");assertEquals(1,p.size);assertEquals("KLCC",p.single().name)}
 @Test fun nonPositiveDurationsAreNotPresentedAsValidRoutes(){assertTrue(runCatching{parseRoutes("""{"code":"Ok","routes":[{"distance":1200,"duration":0,"geometry":{"coordinates":[[101,3],[102,4]]},"legs":[]}]}""")}.isFailure)}
}
