package my.maze.malaysia

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class MazeCartographyTest {
 @Test fun bothThemesKeepResourcesLocalAndLayerIdsUnique(){
  for(night in listOf(false,true)){
   val base="http://127.0.0.1:12345/test-token"
   val style=JSONObject(mazeMapStyle(base,night))
   assertTrue(style.getString("glyphs").startsWith(base))
   val source=style.getJSONObject("sources").getJSONObject("region")
   assertEquals(14,source.getInt("maxzoom"))
   assertTrue(source.getJSONArray("tiles").getString(0).startsWith(base))
   val layers=style.getJSONArray("layers")
   val ids=(0 until layers.length()).map{layers.getJSONObject(it).getString("id")}
   assertEquals(ids.size,ids.toSet().size)
   // Roads must paint before labels; the runtime inserts the route below labels.
   val firstSymbol=(0 until layers.length()).first{layers.getJSONObject(it).getString("type")=="symbol"}
   assertTrue((firstSymbol until layers.length()).all{layers.getJSONObject(it).getString("type")=="symbol"})
   val pois=(0 until layers.length()).map{layers.getJSONObject(it)}.first{it.getString("id")=="local-places"}
   assertTrue(pois.getInt("minzoom")>=17)
  }
 }
}
