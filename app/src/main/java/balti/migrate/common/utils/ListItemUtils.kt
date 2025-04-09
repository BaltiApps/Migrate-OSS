package balti.migrate.common.utils

import baltiapps.migrate.domain.common.model.CallLogListItem
import baltiapps.migrate.domain.common.model.ContactListItem
import baltiapps.migrate.domain.common.model.ListItem
import baltiapps.migrate.domain.common.model.SmsListItem

class ListItemUtils {

    @Suppress("UNCHECKED_CAST")
    fun <T: ListItem> toggleAllItems(list: List<T>, isChecked: Boolean): List<T> {
        return list.mapNotNull {
            when (it) {
                is ContactListItem -> it.copy(isChecked = isChecked)
                is CallLogListItem -> it.copy(isChecked = isChecked)
                is SmsListItem -> it.copy(isChecked = isChecked)
                else -> null
            }
        } as List<T>
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: ListItem> toggleSingleItem(list: List<T>, item: T): List<T> {
        val newItem = when(item) {
            is ContactListItem -> item.copy(isChecked = !item.isChecked)
            is CallLogListItem -> item.copy(isChecked = !item.isChecked)
            is SmsListItem -> item.copy(isChecked = !item.isChecked)
            else -> null
        } ?: return emptyList()
        return replaceListItem(
            list = list,
            item = item,
            newItem = newItem as T,
        )
    }

    private fun <T> replaceListItem(
        list: List<T>,
        item: T,
        newItem: T,
    ): List<T> {
        return list.toMutableList().apply {
            val index = indexOf(item)
            if (index != -1) {
                this[index] = newItem
            }
        }
    }
}