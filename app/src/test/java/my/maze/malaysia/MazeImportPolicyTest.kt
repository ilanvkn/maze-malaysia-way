package my.maze.malaysia
import org.junit.Assert.*
import org.junit.Test
class MazeImportPolicyTest {
    @Test fun importReservesStagingSpaceAndRejectsInvalidTotals() {
        assertEquals(1_314_856_960L, importSpaceRequired(1_247_748_096L))
        assertThrows(IllegalArgumentException::class.java) { importSpaceRequired(0) }
        assertThrows(ArithmeticException::class.java) { importSpaceRequired(Long.MAX_VALUE) }
    }
    @Test fun failuresGiveRecoveryWithoutLeakingFilesystemOrProviderDetails() {
        assertTrue(importFailureMessage(PackImportProblem(PackIssue.SPACE)).contains("Free some space"))
        assertTrue(importFailureMessage(java.util.zip.ZipException("private/location.zip")).contains("Download it again"))
        val generic = importFailureMessage(java.io.IOException("private/provider/token"))
        assertFalse(generic.contains("private")); assertFalse(generic.contains("token"))
        assertTrue(importFailureMessage(PackImportProblem(PackIssue.CONTENT)).contains("matching Malaysia pack"))
    }
}
