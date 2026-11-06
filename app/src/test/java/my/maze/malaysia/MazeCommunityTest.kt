package my.maze.malaysia
import java.net.URLDecoder
import org.junit.Assert.*
import org.junit.Test
class MazeCommunityTest {
    @Test fun onlyPublicNodeAndWayIdentitiesCanLeaveTheApp() {
        assertEquals("https://www.openstreetmap.org/node/123", publicOsmLink("osm-node/123"))
        assertEquals("https://www.openstreetmap.org/way/42", publicOsmLink("osm-way/42"))
        for (bad in listOf("current", "sentral", "osm-node/0", "osm-node/-1", "osm-relation/12", "osm-node/9223372036854775808", "osm-node/12?lat=3.14", "../../private"))
            assertNull(publicOsmLink(bad))
    }
    @Test fun reportPrefillContainsNoPrivatePointOrArbitraryVersionText() {
        val version = "0.3.2-offline-beta-lab"
        val valid = URLDecoder.decode(placeReportUrl("osm-way/42", version), "UTF-8")
        assertTrue(valid.startsWith("$MAZE_HOME/issues/new?"));assertTrue(valid.contains("template=data.yml"));assertTrue(valid.contains("/way/42"))
        val private = URLDecoder.decode(placeReportUrl("current", "private GPS 3.14,101.6"), "UTF-8")
        assertFalse(private.contains("GPS"));assertFalse(private.contains("3.14"));assertFalse(private.contains("place="));assertFalse(private.contains("environment="))
    }
}
