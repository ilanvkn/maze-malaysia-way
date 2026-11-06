package my.maze.malaysia

internal enum class PackIssue(val guidance: String) {
    SPACE("Not enough storage to import the map. Free some space, then choose the ZIP again."),
    CONTENT("This ZIP does not match this Maze version. Download the matching Malaysia pack, then try again."),
    INCOMPLETE("The map ZIP is incomplete or damaged. Download it again and keep it zipped."),
    READ("The file could not be read. Check that it is available on this device, then choose it again.")
}
internal class PackImportProblem(val issue: PackIssue) : IllegalStateException(issue.guidance)
internal fun importSpaceRequired(datasetBytes: Long): Long {
    require(datasetBytes > 0)
    return Math.addExact(datasetBytes, 64L * 1024 * 1024)
}
internal fun importFailureMessage(error: Exception): String = when (error) {
    is PackImportProblem -> error.issue.guidance
    is java.util.zip.ZipException -> PackIssue.INCOMPLETE.guidance
    else -> PackIssue.READ.guidance
}
