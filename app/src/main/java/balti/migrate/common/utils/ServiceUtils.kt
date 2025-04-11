package balti.migrate.common.utils

import baltiapps.migrate.domain.BREAK_LINE
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.repository.ProgressLogRepository
import baltiapps.migrate.domain.common.sources.ContextSource
import baltiapps.migrate.domain.common.sources.fileSystem.TextWriter
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException

class ServiceUtils(
    private val contextSource: ContextSource,
    private val progressLogRepository: ProgressLogRepository,
    private val logWriter: TextWriter<String>,
    private val errorWriter: TextWriter<String>,
) {
    suspend fun collectLogs(
        progress: Progress,
    ) {
        if (progress.isFailure || progress.isFinished()) {
            progressLogRepository.pushError(progress)
            errorWriter.writeLine(progress.logs)
        }
        progressLogRepository.pushProgress(progress)
        logWriter.writeLine(progress.logs)
    }

    suspend fun emitHeadingLog(
        progressType: Progress.ProgressType,
    ) {
        val headingTitle = contextSource.getProgressTitle(progressType)
        collectLogs(
            progress = Progress(
                progressType = progressType,
                percentage = 1.0,
                logs = "\n${headingTitle}\n${BREAK_LINE}\n",
                isLogHeading = true,
            ),
        )
    }

    suspend fun runStage(
        shouldRun: () -> Boolean,
        stageBody: () -> Flow<Progress>,
        progressType: Progress.ProgressType,
        errorMessage: (Exception) -> String,
    ) {
        try {
            if (shouldRun()) {
                emitHeadingLog(progressType)
                stageBody().collect {
                    collectLogs(it)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
            Progress(
                progressType = progressType,
                percentage = 1.0,
                logs = errorMessage(e),
                isFailure = true,
            ).run {
                collectLogs(this)
            }
        }
    }
}