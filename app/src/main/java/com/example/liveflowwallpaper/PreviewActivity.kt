package com.example.liveflowwallpaper

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class PreviewActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(PreviewView())
    }

    private inner class PreviewView : View(this) {

        private var backgroundBitmap: Bitmap? = null
        private var flowBitmap: Bitmap? = null

        private val bitmapPaint =
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        private val random =
            Random(System.currentTimeMillis())

        private val pieces =
            mutableListOf<Piece>()

        private var startTime =
            System.currentTimeMillis()

        private var state =
            AnimationState.ASSEMBLING

        private val assembleDuration = 6500L
        private val staticDuration = 2500L
        private val breakDuration = 5000L

        init {
            loadPhotos()
        }

        private fun loadPhotos() {

            val preferences =
                getSharedPreferences("LiveFlow", MODE_PRIVATE)

            backgroundBitmap =
                loadBitmap(
                    preferences.getString(
                        "background_photo",
                        null
                    )
                )

            flowBitmap =
                loadBitmap(
                    preferences.getString(
                        "flow_photo",
                        null
                    )
                )

            post {
                createPieces()

                startTime =
                    System.currentTimeMillis()

                invalidate()
            }
        }

        private fun loadBitmap(
            uriString: String?
        ): Bitmap? {

            if (uriString.isNullOrEmpty()) {
                return null
            }

            return try {

                val input =
                    contentResolver.openInputStream(
                        Uri.parse(uriString)
                    )

                val bitmap =
                    BitmapFactory.decodeStream(input)

                input?.close()

                bitmap

            } catch (_: Exception) {

                null
            }
        }

        private fun createPieces() {

            pieces.clear()

            val bitmap =
                flowBitmap ?: return

            if (width <= 0 || height <= 0) {
                return
            }

            val columns = 8
            val rows = 10

            val left =
                width * 0.10f

            val top =
                height * 0.18f

            val right =
                width * 0.90f

            val bottom =
                height * 0.82f

            val regionWidth =
                right - left

            val regionHeight =
                bottom - top

            val pieceWidth =
                regionWidth / columns

            val pieceHeight =
                regionHeight / rows

            val preferences =
                getSharedPreferences(
                    "LiveFlow",
                    MODE_PRIVATE
                )

            val selectedShape =
                preferences.getString(
                    "piece_shape",
                    "Random"
                ) ?: "Random"

            for (row in 0 until rows) {

                for (column in 0 until columns) {

                    val targetLeft =
                        left +
                            column * pieceWidth

                    val targetTop =
                        top +
                            row * pieceHeight

                    val sourceLeft =
                        (
                            column *
                                bitmap.width /
                                columns
                        ).coerceIn(
                            0,
                            bitmap.width - 1
                        )

                    val sourceTop =
                        (
                            row *
                                bitmap.height /
                                rows
                        ).coerceIn(
                            0,
                            bitmap.height - 1
                        )

                    val sourceRight =
                        (
                            (column + 1) *
                                bitmap.width /
                                columns
                        ).coerceIn(
                            sourceLeft + 1,
                            bitmap.width
                        )

                    val sourceBottom =
                        (
                            (row + 1) *
                                bitmap.height /
                                rows
                        ).coerceIn(
                            sourceTop + 1,
                            bitmap.height
                        )

                    val pieceBitmap =
                        Bitmap.createBitmap(
                            bitmap,
                            sourceLeft,
                            sourceTop,
                            sourceRight - sourceLeft,
                            sourceBottom - sourceTop
                        )

                    pieces.add(
                        Piece(
                            bitmap = pieceBitmap,
                            targetX =
                                targetLeft +
                                    pieceWidth / 2f,
                            targetY =
                                targetTop +
                                    pieceHeight / 2f,
                            width = pieceWidth,
                            height = pieceHeight,
                            scatterX =
                                random.nextFloat() *
                                    width,
                            scatterY =
                                random.nextFloat() *
                                    height,
                            startRotation =
                                random.nextFloat() *
                                    720f -
                                    360f,
                            breakRotation =
                                random.nextFloat() *
                                    1080f -
                                    540f,
                            startScale =
                                0.20f +
                                    random.nextFloat() *
                                    0.55f,
                            delay =
                                random.nextFloat() *
                                    0.30f,
                            shape =
                                chooseShape(
                                    selectedShape
                                ),
                            randomSides =
                                3 +
                                    random.nextInt(4)
                        )
                    )
                }
            }
        }

        private fun chooseShape(
            selectedShape: String
        ): PieceShape {

            return when (
                selectedShape.lowercase()
            ) {

                "circle" ->
                    PieceShape.CIRCLE

                "rectangle" ->
                    PieceShape.RECTANGLE

                "triangle" ->
                    PieceShape.TRIANGLE

                "pentagon" ->
                    PieceShape.PENTAGON

                "hexagon" ->
                    PieceShape.HEXAGON

                else ->
                    PieceShape.values()
                        .filter {
                            it != PieceShape.RANDOM
                        }
                        .random(random)
            }
        }

        override fun onDraw(
            canvas: Canvas
        ) {

            super.onDraw(canvas)

            drawBackground(canvas)

            val elapsed =
                System.currentTimeMillis() -
                    startTime

            when (state) {

                AnimationState.ASSEMBLING -> {

                    val progress =
                        (
                            elapsed.toFloat() /
                                assembleDuration
                        ).coerceIn(
                            0f,
                            1f
                        )

                    drawAssembling(
                        canvas,
                        progress
                    )

                    if (progress >= 1f) {

                        state =
                            AnimationState.STATIC

                        startTime =
                            System.currentTimeMillis()
                    }
                }

                AnimationState.STATIC -> {

                    drawCompletePhoto(canvas)

                    if (elapsed >= staticDuration) {

                        state =
                            AnimationState.BREAKING

                        startTime =
                            System.currentTimeMillis()
                    }
                }

                AnimationState.BREAKING -> {

                    val progress =
                        (
                            elapsed.toFloat() /
                                breakDuration
                        ).coerceIn(
                            0f,
                            1f
                        )

                    drawBreaking(
                        canvas,
                        progress
                    )

                    if (progress >= 1f) {

                        createPieces()

                        state =
                            AnimationState.ASSEMBLING

                        startTime =
                            System.currentTimeMillis()
                    }
                }
            }

            postInvalidateDelayed(16L)
        }

        private fun drawBackground(
            canvas: Canvas
        ) {

            val bitmap =
                backgroundBitmap

            if (bitmap == null) {

                canvas.drawColor(
                    Color.rgb(
                        8,
                        12,
                        20
                    )
                )

                return
            }

            canvas.drawBitmap(
                bitmap,
                null,
                Rect(
                    0,
                    0,
                    width,
                    height
                ),
                bitmapPaint
            )
        }

        private fun drawCompletePhoto(
            canvas: Canvas
        ) {

            val bitmap =
                flowBitmap ?: return

            val left =
                width * 0.10f

            val top =
                height * 0.18f

            val right =
                width * 0.90f

            val bottom =
                height * 0.82f

            canvas.drawBitmap(
                bitmap,
                null,
                RectF(
                    left,
                    top,
                    right,
                    bottom
                ),
                bitmapPaint
            )
        }

        private fun drawAssembling(
            canvas: Canvas,
            progress: Float
        ) {

            for (piece in pieces) {

                val local =
                    (
                        (progress -
                            piece.delay) /
                            (1f -
                                piece.delay)
                    ).coerceIn(
                        0f,
                        1f
                    )

                drawPiece(
                    canvas,
                    piece,
                    easeOutCubic(local),
                    false
                )
            }
        }

        private fun drawBreaking(
            canvas: Canvas,
            progress: Float
        ) {

            for (piece in pieces) {

                val local =
                    (
                        (progress -
                            piece.delay) /
                            (1f -
                                piece.delay)
                    ).coerceIn(
                        0f,
                        1f
                    )

                drawPiece(
                    canvas,
                    piece,
                    easeInCubic(local),
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
                if (breaking)
                    piece.targetX
                else
                    piece.scatterX

            val startY =
                if (breaking)
                    piece.targetY
                else
                    piece.scatterY

            val endX =
                if (breaking)
                    piece.scatterX
                else
                    piece.targetX

            val endY =
                if (breaking)
                    piece.scatterY
                else
                    piece.targetY

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
                        0.02f,
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

                    (
                        (1f - progress) *
                            255f
                    ).toInt()

                } else {

                    255
                }

            val path =
                createShapePath(
                    piece,
                    scale
                )

            canvas.save()

            canvas.translate(
                x,
                y
            )

            canvas.rotate(
                rotation
            )

            canvas.clipPath(path)

            val halfWidth =
                piece.width *
                    scale /
                    2f

            val halfHeight =
                piece.height *
                    scale /
                    2f

            bitmapPaint.alpha =
                alpha.coerceIn(
                    0,
                    255
                )

            canvas.drawBitmap(
                piece.bitmap,
                null,
                RectF(
                    -halfWidth,
                    -halfHeight,
                    halfWidth,
                    halfHeight
                ),
                bitmapPaint
            )

            bitmapPaint.alpha = 255

            canvas.restore()
        }

        private fun createShapePath(
            piece: Piece,
            scale: Float
        ): Path {

            val pieceWidth =
                piece.width * scale

            val pieceHeight =
                piece.height * scale

            return when (piece.shape) {

                PieceShape.CIRCLE -> {

                    Path().apply {

                        addOval(
                            -pieceWidth / 2f,
                            -pieceHeight / 2f,
                            pieceWidth / 2f,
                            pieceHeight / 2f,
                            Path.Direction.CW
                        )
                    }
                }

                PieceShape.RECTANGLE -> {

                    Path().apply {

                        addRect(
                            -pieceWidth / 2f,
                            -pieceHeight / 2f,
                            pieceWidth / 2f,
                            pieceHeight / 2f,
                            Path.Direction.CW
                        )
                    }
                }

                PieceShape.TRIANGLE ->
                    polygon(
                        3,
                        pieceWidth / 2f,
                        pieceHeight / 2f
                    )

                PieceShape.PENTAGON ->
                    polygon(
                        5,
                        pieceWidth / 2f,
                        pieceHeight / 2f
                    )

                PieceShape.HEXAGON ->
                    polygon(
                        6,
                        pieceWidth / 2f,
                        pieceHeight / 2f
                    )

                PieceShape.RANDOM ->
                    polygon(
                        piece.randomSides,
                        pieceWidth / 2f,
                        pieceHeight / 2f
                    )
            }
        }

        private fun polygon(
            sides: Int,
            radiusX: Float,
            radiusY: Float
        ): Path {

            val path =
                Path()

            for (i in 0 until sides) {

                val angle =
                    -Math.PI / 2.0 +
                        2.0 *
                        Math.PI *
                        i /
                        sides

                val x =
                    cos(angle).toFloat() *
                        radiusX

                val y =
                    sin(angle).toFloat() *
                        radiusY

                if (i == 0) {

                    path.moveTo(
                        x,
                        y
                    )

                } else {

                    path.lineTo(
                        x,
                        y
                    )
                }
            }

            path.close()

            return path
        }

        private fun easeOutCubic(
            value: Float
        ): Float {

            val v =
                1f - value

            return 1f -
                v * v * v
        }

        private fun easeInCubic(
            value: Float
        ): Float {

            return value *
                value *
                value
        }

        private fun lerp(
            start: Float,
            end: Float,
            amount: Float
        ): Float {

            return start +
                (end - start) *
                amount
        }
    }
}

/*
 * These types are deliberately outside
  * PreviewView so Kotlin accepts them.
 */

private enum class AnimationState {
    ASSEMBLING,
    STATIC,
    BREAKING
}

private enum class PieceShape {
    CIRCLE,
    RECTANGLE,
    TRIANGLE,
    PENTAGON,
    HEXAGON,
    RANDOM
}

private data class Piece(
    val bitmap: Bitmap,
    val targetX: Float,
    val targetY: Float,
    val width: Float,
    val height: Float,
    val scatterX: Float,
    val scatterY: Float,
    val startRotation: Float,
    val breakRotation: Float,
    val startScale: Float,
    val delay: Float,
    val shape: PieceShape,
    val randomSides: Int
)
