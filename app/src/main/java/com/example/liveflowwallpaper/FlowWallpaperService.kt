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

        private var thread: Thread? = null
        private var running = false

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val random = Random(System.currentTimeMillis())

        private var background: Bitmap? = null
        private var flow: Bitmap? = null

        private var pieces = mutableListOf<Piece>()

        private var phase = 0
        private var phaseStart = 0L
        private var lastFrame = 0L

        private val assembleTime = 4200L
        private val staticTime = 2200L
        private val breakTime = 3600L

        private var shapeMode = "Random"

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
            createAnimation()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            stopAnimation()
            super.onSurfaceDestroyed(holder)
        }

        private fun startAnimation() {
            if (running) return

            running = true
            phaseStart = System.currentTimeMillis()

            thread = Thread {
                while (running) {
                    drawFrame()
                    try {
                        Thread.sleep(16)
                    } catch (_: InterruptedException) {
                    }
                }
            }

            thread?.start()
        }

        private fun stopAnimation() {
            running = false
            thread?.interrupt()
            thread = null
        }

        private fun createAnimation() {
            val holder = surfaceHolder
            if (!holder.surface.isValid) return

            val width = holder.surfaceFrame.width()
            val height = holder.surfaceFrame.height()

            if (width <= 0 || height <= 0) return

            /*
             * Temporary generated image.
             *
             * In the next part we will connect this to the
             * actual Flow Photo selected by the user.
             */
            background = createBackground(width, height)
            flow = createFlowPhoto(width, height)

            pieces.clear()
            createPieces(width, height)

            phase = 0
            phaseStart = System.currentTimeMillis()
        }

        private fun createBackground(width: Int, height: Int): Bitmap {
            val bitmap = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(bitmap)

            canvas.drawColor(Color.rgb(20, 22, 28))

            return bitmap
        }

        private fun createFlowPhoto(width: Int, height: Int): Bitmap {
            val bitmap = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(bitmap)

            val p = Paint(Paint.ANTI_ALIAS_FLAG)

            p.shader = LinearGradient(
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
                p
            )

            p.shader = null

            return bitmap
        }

        private fun createPieces(width: Int, height: Int) {

            val cols = 8
            val rows = 10

            val pieceWidth = width / cols.toFloat()
            val pieceHeight = height * 0.56f / rows

            val startY = height * 0.22f

            for (row in 0 until rows) {
                for (col in 0 until cols) {

                    val targetLeft = col * pieceWidth
                    val targetTop = startY + row * pieceHeight

                    val target = RectF(
                        targetLeft,
                        targetTop,
                        targetLeft + pieceWidth,
                        targetTop + pieceHeight
                    )

                    val angle = random.nextFloat() * 360f

                    val startX =
                        random.nextFloat() * width * 1.4f - width * 0.2f

                    val startYRandom =
                        random.nextFloat() * height * 1.4f - height * 0.2f

                    pieces.add(
                        Piece(
                            row = row,
                            col = col,
                            target = target,
                            startX = startX,
                            startY = startYRandom,
                            startRotation = angle,
                            startScale = random.nextFloat() * 0.7f + 0.3f,
                            rotationSpeed =
                                random.nextFloat() * 100f - 50f
                        )
                    )
                }
            }
        }

        private fun drawFrame() {

            val holder = surfaceHolder

            if (!holder.surface.isValid) return

            val now = System.currentTimeMillis()

            if (lastFrame == 0L) {
                lastFrame = now
            }

            val elapsed = now - phaseStart

            when (phase) {

                0 -> {
                    if (elapsed >= assembleTime) {
                        phase = 1
                        phaseStart = now
                    }
                }

                1 -> {
                    if (elapsed >= staticTime) {
                        phase = 2
                        phaseStart = now
                    }
                }

                2 -> {
                    if (elapsed >= breakTime) {
                        randomizePieces()
                        phase = 0
                        phaseStart = now
                    }
                }
            }

            val canvas = try {
                holder.lockCanvas()
            } catch (_: Exception) {
                null
            }

            if (canvas == null) return

            try {

                canvas.drawColor(Color.BLACK)

                drawBackground(canvas)

                when (phase) {
                    0 -> drawAssembling(canvas, elapsed)
                    1 -> drawStatic(canvas)
                    2 -> drawBreaking(canvas, elapsed)
                }

            } finally {
                holder.unlockCanvasAndPost(canvas)
            }

            lastFrame = now
        }

        private fun drawBackground(canvas: Canvas) {

            val bitmap = background ?: return

            val src = Rect(
                0,
                0,
                bitmap.width,
                bitmap.height
            )

            val dst = Rect(
                0,
                0,
                canvas.width,
                canvas.height
            )

            paint.alpha = 255
            paint.shader = null

            canvas.drawBitmap(
                bitmap,
                src,
                dst,
                paint
            )
        }

        private fun drawAssembling(
            canvas: Canvas,
            elapsed: Long
        ) {

            val progress =
                (elapsed.toFloat() / assembleTime)
                    .coerceIn(0f, 1f)

            val smooth = smoothStep(progress)

            pieces.forEach { piece ->

                val x = lerp(
                    piece.startX,
                    piece.target.centerX(),
                    smooth
                )

                val y = lerp(
                    piece.startY,
                    piece.target.centerY(),
                    smooth
                )

                val scale = lerp(
                    piece.startScale,
                    1f,
                    smooth
                )

                val rotation = lerp(
                    piece.startRotation,
                    0f,
                    smooth
                )

                drawPiece(
                    canvas,
                    piece,
                    x,
                    y,
                    scale,
                    rotation
                )
            }
        }

        private fun drawStatic(canvas: Canvas) {

            pieces.forEach { piece ->

                drawPiece(
                    canvas,
                    piece,
                    piece.target.centerX(),
                    piece.target.centerY(),
                    1f,
                    0f
                )
            }
        }

        private fun drawBreaking(
            canvas: Canvas,
            elapsed: Long
        ) {

            val progress =
                (elapsed.toFloat() / breakTime)
                    .coerceIn(0f, 1f)

            val smooth = smoothStep(progress)

            pieces.forEach { piece ->

                val x = lerp(
                    piece.target.centerX(),
                    piece.endX,
                    smooth
                )

                val y = lerp(
                    piece.target.centerY(),
                    piece.endY,
                    smooth
                )

                val scale = lerp(
                    1f,
                    0f,
                    smooth
                )

                val rotation =
                    piece.endRotation * smooth

                val alpha =
                    ((1f - smooth) * 255f)
                        .toInt()
                        .coerceIn(0, 255)

                paint.alpha = alpha

                drawPiece(
                    canvas,
                    piece,
                    x,
                    y,
                    scale,
                    rotation
                )
            }

            paint.alpha = 255
        }

        private fun drawPiece(
            canvas: Canvas,
            piece: Piece,
            centerX: Float,
            centerY: Float,
            scale: Float,
            rotation: Float
        ) {

            val bitmap = flow ?: return

            val save = canvas.save()

            canvas.translate(centerX, centerY)
            canvas.rotate(rotation)
            canvas.scale(scale, scale)

            val srcLeft =
                (piece.col * bitmap.width / 8f).toInt()

            val srcTop =
                (piece.row * bitmap.height / 10f).toInt()

            val srcRight =
                ((piece.col + 1) * bitmap.width / 8f).toInt()

            val srcBottom =
                ((piece.row + 1) * bitmap.height / 10f).toInt()

            val src = Rect(
                srcLeft,
                srcTop,
                srcRight,
                srcBottom
            )

            val dst = RectF(
                -piece.target.width() / 2f,
                -piece.target.height() / 2f,
                piece.target.width() / 2f,
                piece.target.height() / 2f
            )

            val path = createShapePath(
                dst,
                piece.shape
            )

            canvas.save()

            canvas.clipPath(path)

            paint.alpha =
                ((piece.alpha) * 255).toInt()

            canvas.drawBitmap(
                bitmap,
                src,
                dst,
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

                ShapeType.CIRCLE -> {
                    path.addOval(
                        rect,
                        Path.Direction.CW
                    )
                }

                ShapeType.RECTANGLE -> {
                    path.addRect(
                        rect,
                        Path.Direction.CW
                    )
                }

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

                ShapeType.PENTAGON -> {
                    polygonPath(
                        path,
                        rect,
                        5
                    )
                }

                ShapeType.HEXAGON -> {
                    polygonPath(
                        path,
                        rect,
                        6
                    )
                }
            }

            return path
        }

        private fun polygonPath(
            path: Path,
            rect: RectF,
            sides: Int
        ) {

            val cx = rect.centerX()
            val cy = rect.centerY()

            val radius =
                min(rect.width(), rect.height()) / 2f

            for (i in 0 until sides) {

                val angle =
                    -PI / 2 +
                            i * (2.0 * PI / sides)

                val x =
                    cx + cos(angle).toFloat() * radius

                val y =
                    cy + sin(angle).toFloat() * radius

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            path.close()
        }

        private fun randomizePieces() {

            pieces.forEach { piece ->

                piece.startX =
                    random.nextFloat() *
                            surfaceHolder.surfaceFrame.width() *
                            1.4f -
                            surfaceHolder.surfaceFrame.width() *
                            0.2f

                piece.startY =
                    random.nextFloat() *
                            surfaceHolder.surfaceFrame.height() *
                            1.4f -
                            surfaceHolder.surfaceFrame.height() *
                            0.2f

                piece.startRotation =
                    random.nextFloat() * 360f

                piece.startScale =
                    random.nextFloat() * 0.7f + 0.3f

                piece.endX =
                    random.nextFloat() *
                            surfaceHolder.surfaceFrame.width() *
                            1.4f -
                            surfaceHolder.surfaceFrame.width() *
                            0.2f

                piece.endY =
                    random.nextFloat() *
                            surfaceHolder.surfaceFrame.height() *
                            1.4f -
                            surfaceHolder.surfaceFrame.height() *
                            0.2f

                piece.endRotation =
                    random.nextFloat() * 720f - 360f

                piece.alpha = 1f
            }
        }

        private fun smoothStep(value: Float): Float {
            return value * value * (3f - 2f * value)
        }

        private fun lerp(
            a: Float,
            b: Float,
            t: Float
        ): Float {
            return a + (b - a) * t
        }

        private fun ShapeForPiece(): ShapeType {
            return when (shapeMode) {
                "Circle" -> ShapeType.CIRCLE
                "Rectangle" -> ShapeType.RECTANGLE
                "Triangle" -> ShapeType.TRIANGLE
                "Pentagon" -> ShapeType.PENTAGON
                "Hexagon" -> ShapeType.HEXAGON
                else -> ShapeType.values()
                    .random(random)
            }
        }

        private enum class ShapeType {
            CIRCLE,
            RECTANGLE,
            TRIANGLE,
            PENTAGON,
            HEXAGON
        }

        private class Piece(
            val row: Int,
            val col: Int,
            val target: RectF,
            var startX: Float,
            var startY: Float,
            var startRotation: Float,
            var startScale: Float,
            var rotationSpeed: Float
        ) {

            var endX = startX
            var endY = startY
            var endRotation = 0f

            var alpha = 1f

            val shape: ShapeType =
                ShapeType.values().random()

        }
    }
}
