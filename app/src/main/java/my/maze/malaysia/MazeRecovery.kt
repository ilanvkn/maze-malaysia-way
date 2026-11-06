package my.maze.malaysia

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException

enum class ProblemKind { NOT_CONFIGURED, OFFLINE, TIMEOUT, RATE_LIMITED, UNAVAILABLE, INVALID_DATA }
class ServiceProblem(val kind:ProblemKind,val waitSeconds:Int=0):IOException(kind.name)
class ServiceGate(private val clock:()->Long={System.nanoTime()/1_000_000}) {
    private var failures=0
    private var openUntil=0L
    @Synchronized fun permit(){val remaining=openUntil-clock();if(remaining>0)throw ServiceProblem(ProblemKind.RATE_LIMITED,((remaining+999)/1000).toInt())}
    @Synchronized fun success(){failures=0;openUntil=0}
    @Synchronized fun failure(){failures++;if(failures>=3)openUntil=clock()+30_000}
    @Synchronized fun limited(seconds:Int){openUntil=clock()+seconds.coerceIn(1,300)*1000L}
}
fun validateGateway(value:String,isLab:Boolean):HttpUrl? {
    if(value.isBlank())return null
    val url=value.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid gateway URL")
    require(url.username.isEmpty()&&url.password.isEmpty()&&url.query==null&&url.fragment==null)
    require(url.isHttps || (isLab&&url.scheme=="http"&&url.host in listOf("10.0.2.2","127.0.0.1","localhost")))
    return url
}
fun recoveryMessage(problem:Throwable,hasPreview:Boolean=false):String {
    val p=problem as? ServiceProblem
    val detail=when(p?.kind){
        ProblemKind.NOT_CONFIGURED->"Online service is not connected. You can explore the map and open saved trips."
        ProblemKind.RATE_LIMITED->"The service is taking a pause. Try again in "+p.waitSeconds+" seconds."
        ProblemKind.INVALID_DATA->"The service returned an unusable route. Try again later."
        ProblemKind.TIMEOUT->"The service took too long. Try again when your connection improves."
        else->"The service is unavailable. Check your connection and try again."
    }
    return detail+if(hasPreview)" Your existing preview is still available below." else " No new road route has been drawn."
}
