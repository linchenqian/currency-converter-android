package com.jojo.currencyconverter.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.Xml
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.PathParser
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

private data class FlagShape(
    val path: Path,
    val color: Int,
)

private data class FlagGraphic(
    val shapes: List<FlagShape>,
)

private object FlagGraphicStore {
    private val cache = ConcurrentHashMap<String, FlagGraphic>()

    fun load(context: Context, requestedCode: String): FlagGraphic {
        return cache.getOrPut(requestedCode) {
            val assetName = runCatching {
                context.assets.open("flags/$requestedCode.svg").close()
                requestedCode
            }.getOrElse { "un" }
            context.assets.open("flags/$assetName.svg").use { input ->
                val parser = Xml.newPullParser().apply {
                    setInput(input, "UTF-8")
                }
                val shapes = mutableListOf<FlagShape>()
                var maskDepth = 0
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            if (parser.name == "mask") {
                                maskDepth = 1
                            } else if (maskDepth > 0) {
                                maskDepth += 1
                            } else {
                                parseShape(parser)?.let(shapes::add)
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (maskDepth > 0) maskDepth -= 1
                        }
                    }
                    event = parser.next()
                }
                FlagGraphic(shapes)
            }
        }
    }

    private fun parseShape(parser: XmlPullParser): FlagShape? {
        val rawFill = parser.attribute("fill") ?: return null
        if (rawFill == "none") return null
        val color = parseColor(rawFill) ?: return null
        val path = when (parser.name) {
            "path" -> {
                val pathData = parser.attribute("d") ?: return null
                PathParser.createPathFromPathData(pathData)
            }
            "rect" -> {
                val x = parser.floatAttribute("x")
                val y = parser.floatAttribute("y")
                val width = parser.floatAttribute("width")
                val height = parser.floatAttribute("height")
                Path().apply { addRect(x, y, x + width, y + height, Path.Direction.CW) }
            }
            "circle" -> {
                val cx = parser.floatAttribute("cx")
                val cy = parser.floatAttribute("cy")
                val radius = parser.floatAttribute("r")
                Path().apply { addCircle(cx, cy, radius, Path.Direction.CW) }
            }
            "ellipse" -> {
                val cx = parser.floatAttribute("cx")
                val cy = parser.floatAttribute("cy")
                val radiusX = parser.floatAttribute("rx")
                val radiusY = parser.floatAttribute("ry")
                Path().apply {
                    addOval(
                        RectF(cx - radiusX, cy - radiusY, cx + radiusX, cy + radiusY),
                        Path.Direction.CW,
                    )
                }
            }
            else -> return null
        }
        return FlagShape(path, color)
    }

    private fun XmlPullParser.attribute(name: String): String? = getAttributeValue(null, name)

    private fun XmlPullParser.floatAttribute(name: String): Float =
        attribute(name)?.toFloatOrNull() ?: 0f

    private fun parseColor(raw: String): Int? = runCatching {
        val normalized = when {
            raw.length == 4 && raw.startsWith("#") -> buildString {
                append('#')
                raw.drop(1).forEach {
                    append(it)
                    append(it)
                }
            }
            raw.length == 5 && raw.startsWith("#") -> buildString {
                append('#')
                raw.drop(1).forEach {
                    append(it)
                    append(it)
                }
            }
            else -> raw
        }
        Color.parseColor(normalized)
    }.getOrNull()
}

@Composable
fun SvgFlag(
    flagCode: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val graphic by produceState<FlagGraphic?>(null, flagCode) {
        value = withContext(Dispatchers.IO) {
            FlagGraphicStore.load(context.applicationContext, flagCode)
        }
    }

    Canvas(
        modifier = modifier
            .clip(CircleShape)
            .background(ComposeColor(0xFFE3EAF0)),
    ) {
        val loadedGraphic = graphic ?: return@Canvas
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val scale = min(size.width, size.height) / 512f
        val offsetX = (size.width - 512f * scale) / 2f
        val offsetY = (size.height - 512f * scale) / 2f
        val clipPath = Path().apply {
            addCircle(256f, 256f, 256f, Path.Direction.CW)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        nativeCanvas.save()
        nativeCanvas.translate(offsetX, offsetY)
        nativeCanvas.scale(scale, scale)
        nativeCanvas.clipPath(clipPath)
        loadedGraphic.shapes.forEach { shape ->
            paint.color = shape.color
            nativeCanvas.drawPath(shape.path, paint)
        }
        nativeCanvas.restore()
    }
}
