package my.maze.malaysia

import android.os.Bundle
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

@Composable fun MazeMap(state:MazeState,modifier:Modifier=Modifier,topInset:Int=0,bottomInset:Int=0,rightInset:Int=0,styleUri:String=DAY_STYLE,onReady:(Boolean)->Unit) {
    val context=LocalContext.current; val lifecycle=LocalLifecycleOwner.current.lifecycle
    val mapView=remember(styleUri) { require(java.net.URI(styleUri).host=="127.0.0.1"){"Offline map requires the local pack"};MapLibre.getInstance(context);MapLibre.setConnected(true);MapView(context).apply { onCreate(Bundle()) } }
    var map by remember(styleUri) { mutableStateOf<MapLibreMap?>(null) }
    var ready by remember(styleUri) { mutableStateOf(false) }
    DisposableEffect(mapView,lifecycle) {
        var started=false;var resumed=false
        val observer=LifecycleEventObserver { _,event -> when(event) {
            Lifecycle.Event.ON_START->{if(!started){mapView.onStart();started=true}};Lifecycle.Event.ON_RESUME->{if(!resumed){mapView.onResume();resumed=true}}
            Lifecycle.Event.ON_PAUSE->{if(resumed){mapView.onPause();resumed=false}};Lifecycle.Event.ON_STOP->{if(started){mapView.onStop();started=false}}
            else -> Unit
        } }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer);if(resumed)mapView.onPause();if(started)mapView.onStop();mapView.onDestroy() }
    }
    key(styleUri) { AndroidView(factory={mapView.apply { getMapAsync { m ->
        map=m;m.uiSettings.isLogoEnabled=false
        m.cameraPosition=CameraPosition.Builder().target(LatLng(3.1465,101.699)).zoom(12.5).build()
        m.setStyle(Style.Builder().fromUri(styleUri)) { style ->
            style.addSource(GeoJsonSource("maze-road"))
            val lastRoad=style.layers.indexOfLast{it is LineLayer || it is FillLayer}
            val firstLabel=style.layers.drop(lastRoad+1).firstOrNull{it is SymbolLayer}?.id
            val casing=LineLayer("maze-road-casing","maze-road").withProperties(lineColor("#FFFFFF"),lineWidth(10f),lineCap("round"),lineJoin("round"))
            val line=LineLayer("maze-road-line","maze-road").withProperties(lineColor("#0B715F"),lineWidth(6f),lineCap("round"),lineJoin("round"))
            if(firstLabel!=null){style.addLayerBelow(casing,firstLabel);style.addLayerBelow(line,firstLabel)}else{style.addLayer(casing);style.addLayer(line)}
            style.addSource(GeoJsonSource("maze-access"));style.addLayer(CircleLayer("maze-access-layer","maze-access").withProperties(circleColor("#E6B75D"),circleRadius(4f),circleStrokeColor("#FFFFFF"),circleStrokeWidth(1.5f)))
            style.addSource(GeoJsonSource("maze-points"))
            style.addLayer(CircleLayer("maze-points-layer","maze-points").withProperties(circleColor("#A45A13"),circleRadius(7f),circleStrokeColor("#FFFFFF"),circleStrokeWidth(3f)))
            style.addSource(GeoJsonSource("maze-origin"))
            style.addLayer(CircleLayer("maze-origin-layer","maze-origin").withProperties(circleColor("#0B715F"),circleRadius(7f),circleStrokeColor("#FFFFFF"),circleStrokeWidth(3f)))
            ready=true;onReady(true)
        }
    } }},modifier=modifier) }
    LaunchedEffect(map,styleUri,ready,state.accessPoints){if(ready)map?.style?.getSourceAs<GeoJsonSource>("maze-access")?.setGeoJson(FeatureCollection.fromFeatures(state.accessPoints.map{Feature.fromGeometry(Point.fromLngLat(it.point.lon,it.point.lat))}))}
    val route=state.routes.getOrNull(state.selectedRoute)
    LaunchedEffect(map,styleUri,ready,route,state.origin,state.destination,topInset,bottomInset,rightInset) {
        val m=map ?: return@LaunchedEffect;if(!ready)return@LaunchedEffect
        val style=m.style ?: return@LaunchedEffect
        val coordinates=route?.geometry?.map{Point.fromLngLat(it.lon,it.lat)} ?: emptyList()
        style.getSourceAs<GeoJsonSource>("maze-road")?.setGeoJson(if(coordinates.size>=2)FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(coordinates))) else FeatureCollection.fromFeatures(emptyArray<Feature>()))
        style.getSourceAs<GeoJsonSource>("maze-points")?.setGeoJson(FeatureCollection.fromFeatures(listOf(state.destination).map { Feature.fromGeometry(Point.fromLngLat(it.point.lon,it.point.lat)) }))
        style.getSourceAs<GeoJsonSource>("maze-origin")?.setGeoJson(Feature.fromGeometry(Point.fromLngLat(state.origin.point.lon,state.origin.point.lat)))
        val points=route?.geometry ?: listOf(state.origin.point,state.destination.point)
        if(points.distinct().size>=2) {
            val bounds=LatLngBounds.Builder().includes(points.map{LatLng(it.lat,it.lon)}).build()
            m.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds,40,topInset.coerceAtLeast(40),rightInset.coerceAtLeast(40),bottomInset.coerceAtLeast(40)))
        }
    }
}
