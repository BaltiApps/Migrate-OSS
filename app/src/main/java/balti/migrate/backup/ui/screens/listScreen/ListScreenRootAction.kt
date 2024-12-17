package balti.migrate.backup.ui.screens.listScreen

interface ListScreenRootAction {
    object OnNextClicked : ListScreenRootAction

    class SetLoading(val isLoading: Boolean): ListScreenRootAction

    object SelectAll : ListScreenRootAction
    object DeselectAll : ListScreenRootAction
}
