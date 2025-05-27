package balti.migrate.restore.ui.screens.restoreSummary

enum class RestoreSummaryItemState {
    REQUEST_USER_INPUT,
    ON_USER_INPUT_POSITIVE,
    ON_USER_INPUT_NEGATIVE,
    WAITING,
    PROCESSING,
    DONE,
    CANCELLED,
    ERROR,
    UNKNOWN,
}