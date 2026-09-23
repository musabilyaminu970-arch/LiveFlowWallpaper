package com.example.liveflowwallpaper

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class FlowWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return FlowEngine()
    }

    private inner class FlowEngine : Engine() {

        private var running = false
        private var animationThread: Thread? = null

        private var canvasWidth = 0
        private var canvasHeight = 0

        private var backgroundBitmap: Bitmap? = null
        private var flowBitmap: Bitmap? = null

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        private val random = Random(System.currentTimeMillis())
        private val pieces = mutableListOf<Piece>()

        private var state = AnimationState.ASSEMBLING
        private var animationStart = System.currentTimeMillis()

        private val assembleDuration = 6500L
        private val staticDuration = 2500L
        private val breakDuration = 5000L

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                running = true
                startAnimation()
            } else {
                running = false
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

            canvasWidth = width
            canvasHeight = height

            createBitmaps()
            createPieces()

            state = AnimationState.ASSEMBLING
            animationStart = System.currentTimeMillis()

            if (running) {
                startAnimation()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            running = false
            stopAnimation()
            super.onSurfaceDestroyed(holder)
        }

        private fun startAnimation() {
            if (animationThread?.isAlive == true) {
                return
            }

            animationThread = Thread {
                while (running) {
                    drawFrame()

                    try {
                        Thread.sleep(16L)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }

            animationThread?.start()
        }

        private fun stopAnimation() {
            animationThread?.interrupt()
            animationThread = null
        }

        private fun createBitmaps() {
            if (canvasWidth <= 0 || canvasHeight <= 0) {
                return
            }

            backgroundBitmap = Bitmap.createBitmap(
                canvasWidth,
                canvasHeight,
                Bitmap.Config.ARGB_8888
            )

            flowBitmap = Bitmap.createBitmap(
                canvasWidth,
                canvasHeight,
                Bitmap.Config.ARGB_8888
            )

            val backgroundCanvas = Canvas(backgroundBitmap!!)
            backgroundCanvas.drawColor(Color.rgb(10, 13, 20))

            val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

            for (y in 0 until canvasHeight step 50) {
                for (x in 0 until canvasWidth step 50) {
                    val shade = 18 + ((x + y) % 25)

                    backgroundPaint.color = Color.rgb(
                        shade,
                        shade + 3,
                        shade + 9
                    )

                    backgroundCanvas.drawRect(
                        x.toFloat(),
                        y.toFloat(),
                        (x + 50).toFloat(),
                        (y + 50).toFloat(),
                        backgroundPaint
                    )
                }
            }

            val flowCanvas = Canvas(flowBitmap!!)

            flowCanvas.drawColor(Color.TRANSPARENT)

            val left = canvasWidth * 0.12f
            val top = canvasHeight * 0.22f
            val right = canvasWidth * 0.88f
            val bottom = canvasHeight * 0.78f

            val flowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

            flowPaint.shader = LinearGradient(
                left,
                top,
                right,
                bottom,
                Color.rgb(35, 175, 255),
                Color.rgb(170, 65, 245),
                Shader.TileMode.CLAMP
            )

            flowCanvas.drawRoundRect(
                left,
                top,
                right,
                bottom,
                45f,
                45f,
                flowPaint
            )

            val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG)

            shinePaint.color = Color.argb(
                85,
                255,
                255,
                255
            )

            flowCanvas.drawCircle(
                canvasWidth * 0.30f,
                canvasHeight * 0.35f,
                min(canvasWidth, canvasHeight) * 0.10f,
                shinePaint
            )

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

            textPaint.color = Color.WHITE
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize =
                min(canvasWidth, canvasHeight) * 0.075f

            textPaint.typeface = Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

            flowCanvas.drawText(
                "LIVE FLOW",
                canvasWidth / 2f,
                canvasHeight * 0.53f,
                textPaint
            )
        }

        private fun createPieces() {
            pieces.clear()

            if (canvasWidth <= 0 || canvasHeight <= 0) {
                return
            }

            val columns = 8
            val rows = 10

            val pieceWidth =
                canvasWidth.toFloat() / columns

            val pieceHeight =
                canvasHeight.toFloat() / rows

            for (row in 0 until rows) {
                for (column in 0 until columns) {

                    val sourceX =
                        column * pieceWidth

                    val sourceY =
                        row * pieceHeight

                    val targetX =
                        sourceX + pieceWidth / 2f

                    val targetY =
                        sourceY + pieceHeight / 2f

                    pieces.add(
                        Piece(
                            sourceX = sourceX,
                            sourceY = sourceY,
                            targetX = targetX,
                            targetY = targetY,
                            scatterX =
                                random.nextFloat() *
                                    canvasWidth,
                            scatterY =
                                random.nextFloat() *
                                    canvasHeight,
                            width = pieceWidth,
                            height = pieceHeight,
                            startRotation =
                                random.nextFloat() *
                                    360f - 180f,
                            breakRotation =
                                random.nextFloat() *
                                    720f - 360f,
                            startScale =
                                0.20f +
                                    random.nextFloat() * 0.65f,
                            delay =
                                random.nextFloat() * 0.35f,
                            shape =
                                ShapeType.values()
                                    .filter {
                                        it != ShapeType.RANDOM
                                    }
                                    .random(random),
                            randomSides =
                                3 + random.nextInt(4)
                        )
                    )
                }
            }
        }

        private fun drawFrame() {
            val holder = surfaceHolder

            if (!holder.surface.isValid) {
                return
            }

            val canvas = try {
                holder.lockCanvas()
            } catch (_: Exception) {
                null
            } ?: return

            try {
                val now = System.currentTimeMillis()
                val elapsed = now - animationStart

                when (state) {
                    AnimationState.ASSEMBLING -> {
                        val progress =
                            (elapsed.toFloat() /
                                assembleDuration)
                                .coerceIn(0f, 1f)

                        drawBackground(canvas)
                        drawAssembling(canvas, progress)

                        if (progress >= 1f) {
                            state = AnimationState.STATIC
                            animationStart = now
                        }
                    }

                    AnimationState.STATIC -> {
                        drawBackground(canvas)
                        drawStatic(canvas)

                        if (elapsed >= staticDuration) {
                            state = AnimationState.BREAKING
                            animationStart = now
                        }
                    }

                    AnimationState.BREAKING -> {
                        val progress =
                            (elapsed.toFloat() /
                                breakDuration)
                                .coerceIn(0f, 1f)

                        drawBackground(canvas)
                        drawBreaking(canvas, progress)

                        if (progress >= 1f) {
                            createPieces()
                            state = AnimationState.ASSEMBLING
                            animationStart = now
                        }
                    }
                }
            } finally {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (_: Exception) {
                }
            }
        }
                private fun drawBackground(canvas: Canvas) {
            backgroundBitmap?.let { bitmap ->
                canvas.drawBitmap(
                    bitmap,
                    null,
                    Rect(
                        0,
                        0,
                        canvas.width,
                        canvas.height
                    ),
                    bitmapPaint
                )
            }
        }

        private fun drawAssembling(
            canvas: Canvas,
            progress: Float
        ) {
            for (piece in pieces) {
                val pieceProgress =
                    ((progress - piece.delay) /
                        (1f - piece.delay))
                        .coerceIn(0f, 1f)

                val eased =
                    smoothStep(pieceProgress)

                drawPiece(
                    canvas,
                    piece,
                    eased,
                    false
                )
            }
        }

        private fun drawStatic(canvas: Canvas) {
            for (piece in pieces) {
                drawPiece(
                    canvas,
                    piece,
                    1f,
                    false
                )
            }
        }

        private fun drawBreaking(
            canvas: Canvas,
            progress: Float
        ) {
            for (piece in pieces) {
                val pieceProgress =
                    ((progress - piece.delay) /
                        (1f - piece.delay))
                        .coerceIn(0f, 1f)

                val eased =
                    smoothStep(pieceProgress)

                drawPiece(
                    canvas,
                    piece,
                    eased,
                    true
                )
            }
        }

        private fun drawPiece(
            canvas: Canvas,
            piece: Piece,
            progress: Float,
            breaking: Boolean
        ) {
            val startX =
                if (breaking) {
                    piece.targetX
                } else {
                    piece.scatterX
                }

            val startY =
                if (breaking) {
                    piece.targetY
                } else {
                    piece.scatterY
                }

            val endX =
                if (breaking) {
                    piece.scatterX
                } else {
                    piece.targetX
                }

            val endY =
                if (breaking) {
                    piece.scatterY
                } else {
                    piece.targetY
                }

            val x =
                lerp(
                    startX,
                    endX,
                    progress
                )

            val y =
                lerp(
                    startY,
                    endY,
                    progress
                )

            val rotation =
                if (breaking) {
                    lerp(
                        0f,
                        piece.breakRotation,
                        progress
                    )
                } else {
                    lerp(
                        piece.startRotation,
                        0f,
                        progress
                    )
                }

            val scale =
                if (breaking) {
                    lerp(
                        1f,
                        0.05f,
                        progress
                    )
                } else {
                    lerp(
                        piece.startScale,
                        1f,
                        progress
                    )
                }

            val alpha =
                if (breaking) {
                    ((1f - progress) * 255f)
                        .toInt()
                        .coerceIn(0, 255)
                } else {
                    255
                }

            val path =
                createShapePath(
                    piece,
                    scale
                )

            val bitmap =
                flowBitmap ?: return

            val shader = BitmapShader(
                bitmap,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP
            )

            val matrix = Matrix()

            matrix.setTranslate(
                -piece.sourceX,
                -piece.sourceY
            )

            shader.setLocalMatrix(matrix)

            paint.shader = shader
            paint.alpha = alpha

            canvas.save()

            canvas.translate(x, y)
            canvas.rotate(rotation)

            canvas.drawPath(
                path,
                paint
            )

            canvas.restore()

            paint.shader = null
            paint.alpha = 255
        }

        private fun createShapePath(
            piece: Piece,
            scale: Float
        ): Path {
            val width =
                piece.width * scale

            val height =
                piece.height * scale

            return when (piece.shape) {
                ShapeType.CIRCLE -> {
                    Path().apply {
                        addOval(
                            -width / 2f,
                            -height / 2f,
                            width / 2f,
                            height / 2f,
                            Path.Direction.CW
                        )
                    }
                }

                ShapeType.RECTANGLE -> {
                    Path().apply {
                        addRect(
                            -width / 2f,
                            -height / 2f,
                            width / 2f,
                            height / 2f,
                            Path.Direction.CW
                        )
                    }
                }

                ShapeType.TRIANGLE -> {
                    createPolygonPath(
                        3,
                        width / 2f,
                        height / 2f
                    )
                }

                ShapeType.PENTAGON -> {
                    createPolygonPath(
                        5,
                        width / 2f,
                        height / 2f
                    )
                }

                ShapeType.HEXAGON -> {
                    createPolygonPath(
                        6,
                        width / 2f,
                        height / 2f
                    )
                }

                ShapeType.RANDOM -> {
                    createPolygonPath(
                        piece.randomSides,
                        width / 2f,
                        height / 2f
                    )
                }
            }
        }

        private fun createPolygonPath(
            sides: Int,
            radiusX: Float,
            radiusY: Float
        ): Path {
            val path = Path()

            for (i in 0 until sides) {
                val angle =
                    -Math.PI / 2.0 +
                        (2.0 * Math.PI * i / sides)

                val x =
                    cos(angle).toFloat() *
                        radiusX

                val y =
                    sin(angle).toFloat() *
                        radiusY

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            path.close()

            return path
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
            amount: Float
        ): Float {
            return start +
                (end - start) * amount
        }
    }

    private enum class AnimationState {
        ASSEMBLING,
        STATIC,
        BREAKING
    }

    private enum class ShapeType {
        CIRCLE,
        RECTANGLE,
        TRIANGLE,
        PENTAGON,
        HEXAGON,
        RANDOM
    }

    private data class Piece(
        val sourceX: Float,
        val sourceY: Float,
        val targetX: Float,
        val targetY: Float,
        val scatterX: Float,
        val scatterY: Float,
        val width: Float,
        val height: Float,
        val startRotation: Float,
        val breakRotation: Float,
        val startScale: Float,
        val delay: Float,
        val shape: ShapeType,
        val randomSides: Int
    )
}
