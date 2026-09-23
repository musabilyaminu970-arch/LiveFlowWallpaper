package com.example.liveflowwallpaper

import android.graphics.*
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.*
import kotlin.random.Random

class FlowWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return FlowEngine()
    }

    inner class FlowEngine : Engine() {

        private var running = false
        private var animationThread: Thread? = null

        private val random =
            Random(System.currentTimeMillis())

        private var backgroundBitmap: Bitmap? = null
        private var flowBitmap: Bitmap? = null

        private val pieces =
            mutableListOf<Piece>()

        private var animationPhase =
            Phase.ASSEMBLING

        private var phaseStartTime = 0L

        private val assembleDuration = 4200L
        private val staticDuration = 2200L
        private val breakDuration = 3600L

        private var selectedShape = "Random"

        private enum class Phase {
            ASSEMBLING,
            STATIC,
            BREAKING
        }
        override fun onVisibilityChanged(visible: Boolean) {
    super.onVisibilityChanged(visible)

    if (visible) {
        startAnimation()
    } else {
        stopAnimation()
    }
}

override fun onSurfaceChanged(
    holder: SurfaceHolder,
    format: Int,
    width: Int,
    height: Int
) {
    super.onSurfaceChanged(holder, format, width, height)

    if (width > 0 && height > 0) {
        createAnimation(width, height)
    }
}

override fun onSurfaceDestroyed(
    holder: SurfaceHolder
) {
    stopAnimation()
    super.onSurfaceDestroyed(holder)
}
private fun startAnimation() {

    if (running) return

    running = true
    phaseStartTime = System.currentTimeMillis()

    animationThread = Thread {

        while (running) {

            drawFrame()

            try {
                Thread.sleep(16)
            } catch (_: InterruptedException) {
                break
            }
        }
    }

    animationThread?.start()
}

private fun stopAnimation() {

    running = false

    animationThread?.interrupt()
    animationThread = null
}
private fun createAnimation(
    width: Int,
    height: Int
) {
    backgroundBitmap =
        createBackground(width, height)

    flowBitmap =
        createFlowImage(width, height)

    pieces.clear()

    createPieces(width, height)

    animationPhase =
        Phase.ASSEMBLING

    phaseStartTime =
        System.currentTimeMillis()
}
private fun createBackground(
    width: Int,
    height: Int
): Bitmap {

    val bitmap = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)

    canvas.drawColor(
        Color.rgb(20, 22, 28)
    )

    return bitmap
}
private fun createFlowImage(
    width: Int,
    height: Int
): Bitmap {

    val bitmap = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)

    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG)

    paint.shader = LinearGradient(
        0f,
        0f,
        width.toFloat(),
        height.toFloat(),
        Color.rgb(70, 170, 255),
        Color.rgb(180, 80, 255),
        Shader.TileMode.CLAMP
    )

    canvas.drawRect(
        width * 0.12f,
        height * 0.22f,
        width * 0.88f,
        height * 0.78f,
        paint
    )

    paint.shader = null

    return bitmap
}
private fun createPieces(
    width: Int,
    height: Int
) {
    val columns = 8
    val rows = 10

    val pieceWidth =
        width / columns.toFloat()

    val pieceHeight =
        height * 0.56f / rows

    val startTop =
        height * 0.22f

    for (row in 0 until rows) {

        for (column in 0 until columns) {

            val targetLeft =
                column * pieceWidth

            val targetTop =
                startTop +
                    row * pieceHeight

            val target = RectF(
                targetLeft,
                targetTop,
                targetLeft + pieceWidth,
                targetTop + pieceHeight
            )

            pieces.add(
                Piece(
                    row = row,
                    column = column,
                    target = target,

                    startX =
                        random.nextFloat() *
                            width * 1.4f -
                            width * 0.2f,

                    startY =
                        random.nextFloat() *
                            height * 1.4f -
                            height * 0.2f,

                    startRotation =
                        random.nextFloat() * 360f,

                    startScale =
                        random.nextFloat() *
                            0.7f + 0.3f,

                    endX =
                        random.nextFloat() *
                            width * 1.4f -
                            width * 0.2f,

                    endY =
                        random.nextFloat() *
                            height * 1.4f -
                            height * 0.2f,

                    endRotation =
                        random.nextFloat() *
                            720f - 360f,

                    shape = chooseShape()
                )
            )
        }
    }
}
private fun chooseShape(): ShapeType {

    return when (selectedShape) {

        "Circle" ->
            ShapeType.CIRCLE

        "Rectangle" ->
            ShapeType.RECTANGLE

        "Triangle" ->
            ShapeType.TRIANGLE

        "Pentagon" ->
            ShapeType.PENTAGON

        "Hexagon" ->
            ShapeType.HEXAGON

        else ->
            ShapeType.values()
                .random(random)
    }
}

private fun drawFrame() {

    val holder = surfaceHolder

    if (!holder.surface.isValid) {
        return
    }

    val now =
        System.currentTimeMillis()

    val elapsed =
        now - phaseStartTime

    when (animationPhase) {

        Phase.ASSEMBLING -> {
            if (elapsed >= assembleDuration) {
                animationPhase = Phase.STATIC
                phaseStartTime = now
            }
        }

        Phase.STATIC -> {
            if (elapsed >= staticDuration) {
                animationPhase = Phase.BREAKING
                phaseStartTime = now
            }
        }

        Phase.BREAKING -> {
            if (elapsed >= breakDuration) {
                randomizePieces()
                animationPhase = Phase.ASSEMBLING
                phaseStartTime = now
            }
        }
    }
        val canvas =
        try {
            holder.lockCanvas()
        } catch (_: Exception) {
            null
        }

    if (canvas == null) {
        return
    }

    try {

        drawBackground(canvas)

        when (animationPhase) {

            Phase.ASSEMBLING ->
                drawAssembling(
                    canvas,
                    elapsed
                )

            Phase.STATIC ->
                drawStatic(canvas)

            Phase.BREAKING ->
                drawBreaking(
                    canvas,
                    elapsed
                )
        }

    } finally {

        holder.unlockCanvasAndPost(
            canvas
        )
    }
}

private fun drawBackground(
    canvas: Canvas
) {

    val bitmap =
        backgroundBitmap
            ?: return

    val source = Rect(
        0,
        0,
        bitmap.width,
        bitmap.height
    )

    val destination = Rect(
        0,
        0,
        canvas.width,
        canvas.height
    )

    canvas.drawBitmap(
        bitmap,
        source,
        destination,
        null
    )
}
private fun drawAssembling(
    canvas: Canvas,
    elapsed: Long
) {

    val progress =
        (elapsed.toFloat() /
            assembleDuration)
            .coerceIn(0f, 1f)

    val smoothProgress =
        smoothStep(progress)

    for (piece in pieces) {

        val x =
            lerp(
                piece.startX,
                piece.target.centerX(),
                smoothProgress
            )

        val y =
            lerp(
                piece.startY,
                piece.target.centerY(),
                smoothProgress
            )

        val scale =
            lerp(
                piece.startScale,
                1f,
                smoothProgress
            )

        val rotation =
            lerp(
                piece.startRotation,
                0f,
                smoothProgress
            )

        drawPiece(
            canvas,
            piece,
            x,
            y,
            scale,
            rotation,
            1f
        )
    }
}

private fun drawStatic(
    canvas: Canvas
) {

    for (piece in pieces) {

        drawPiece(
            canvas,
            piece,
            piece.target.centerX(),
            piece.target.centerY(),
            1f,
            0f,
            1f
        )
    }
}
private fun drawBreaking(
    canvas: Canvas,
    elapsed: Long
) {

    val progress =
        (elapsed.toFloat() /
            breakDuration)
            .coerceIn(0f, 1f)

    val smoothProgress =
        smoothStep(progress)

    for (piece in pieces) {

        val x =
            lerp(
                piece.target.centerX(),
                piece.endX,
                smoothProgress
            )

        val y =
            lerp(
                piece.target.centerY(),
                piece.endY,
                smoothProgress
            )

        val scale =
            lerp(
                1f,
                0f,
                smoothProgress
            )

        val rotation =
            lerp(
                0f,
                piece.endRotation,
                smoothProgress
            )

        val alpha =
            (1f - smoothProgress)
                .coerceIn(0f, 1f)

        drawPiece(
            canvas,
            piece,
            x,
            y,
            scale,
            rotation,
            alpha
        )
    }
}
private fun drawPiece(
    canvas: Canvas,
    piece: Piece,
    centerX: Float,
    centerY: Float,
    scale: Float,
    rotation: Float,
    alpha: Float
) {

    val bitmap =
        flowBitmap ?: return

    val save = canvas.save()

    canvas.translate(
        centerX,
        centerY
    )

    canvas.rotate(rotation)

    canvas.scale(
        scale,
        scale
    )

    val sourceLeft =
        (piece.column *
            bitmap.width / 8f).toInt()

    val sourceTop =
        (piece.row *
            bitmap.height / 10f).toInt()

    val sourceRight =
        ((piece.column + 1) *
            bitmap.width / 8f).toInt()

    val sourceBottom =
        ((piece.row + 1) *
            bitmap.height / 10f).toInt()

    val source = Rect(
        sourceLeft,
        sourceTop,
        sourceRight,
        sourceBottom
    )

    val destination = RectF(
        -piece.target.width() / 2f,
        -piece.target.height() / 2f,
        piece.target.width() / 2f,
        piece.target.height() / 2f
    )

    val path =
        createShapePath(
            destination,
            piece.shape
        )

    canvas.save()

    canvas.clipPath(path)

    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG)

    paint.alpha =
        (alpha * 255f)
            .toInt()
            .coerceIn(0, 255)

    canvas.drawBitmap(
        bitmap,
        source,
        destination,
        paint
    )

    canvas.restore()

    canvas.restoreToCount(save)
}
private fun createShapePath(
    rect: RectF,
    shape: ShapeType
): Path {

    val path = Path()

    when (shape) {

        ShapeType.CIRCLE ->
            path.addOval(
                rect,
                Path.Direction.CW
            )

        ShapeType.RECTANGLE ->
            path.addRect(
                rect,
                Path.Direction.CW
            )

        ShapeType.TRIANGLE -> {

            path.moveTo(
                rect.centerX(),
                rect.top
            )

            path.lineTo(
                rect.right,
                rect.bottom
            )

            path.lineTo(
                rect.left,
                rect.bottom
            )

            path.close()
        }

        ShapeType.PENTAGON ->
            createPolygonPath(
                path,
                rect,
                5
            )

        ShapeType.HEXAGON ->
            createPolygonPath(
                path,
                rect,
                6
            )
    }

    return path
}

private fun createPolygonPath(
    path: Path,
    rect: RectF,
    sides: Int
) {

    val centerX = rect.centerX()
    val centerY = rect.centerY()

    val radius =
        min(
            rect.width(),
            rect.height()
        ) / 2f

    for (i in 0 until sides) {

        val angle =
            -PI / 2 +
                i *
                (2.0 * PI / sides)

        val x =
            centerX +
                cos(angle).toFloat() *
                radius

        val y =
            centerY +
                sin(angle).toFloat() *
                radius

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    path.close()
}
private fun randomizePieces() {

    val width =
        surfaceHolder
            .surfaceFrame
            .width()

    val height =
        surfaceHolder
            .surfaceFrame
            .height()

    for (piece in pieces) {

        piece.startX =
            random.nextFloat() *
                width * 1.4f -
                width * 0.2f

        piece.startY =
            random.nextFloat() *
                height * 1.4f -
                height * 0.2f

        piece.startRotation =
            random.nextFloat() * 360f

        piece.startScale =
            random.nextFloat() *
                0.7f + 0.3f

        piece.endX =
            random.nextFloat() *
                width * 1.4f -
                width * 0.2f

        piece.endY =
            random.nextFloat() *
                height * 1.4f -
                height * 0.2f

        piece.endRotation =
            random.nextFloat() *
                720f - 360f
    }
}
private fun smoothStep(
    value: Float
): Float {

    return value * value *
        (3f - 2f * value)
}

private fun lerp(
    start: Float,
    end: Float,
    progress: Float
): Float {

    return start +
        (end - start) *
        progress
}

private enum class ShapeType {
    CIRCLE,
    RECTANGLE,
    TRIANGLE,
    PENTAGON,
    HEXAGON
}
private data class Piece(
    val row: Int,
    val column: Int,
    val target: RectF,

    var startX: Float,
    var startY: Float,
    var startRotation: Float,
    var startScale: Float,

    var endX: Float,
    var endY: Float,
    var endRotation: Float,

    val shape: ShapeType
)
    }
}
