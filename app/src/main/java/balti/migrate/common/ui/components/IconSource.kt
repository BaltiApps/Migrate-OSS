package balti.migrate.common.ui.components

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector

sealed interface IconSource {
    data class Vector(val imageVector: ImageVector) : IconSource
    data class Drawable(val painter: Painter) : IconSource
}
