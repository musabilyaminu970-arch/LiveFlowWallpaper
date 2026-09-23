package com.example.liveflowwallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
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
        private var thread: Thread? = null

        private var backgroundBitmap: Bitmap? = null
        private var flowBitmap: Bitmap? = null

        private val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG or
                    Paint.FILTER_BITMAP_FLAG
            )

        private val random =
            Random(System.currentTimeMillis())

        private val pieceCount = 36

        private val startX =
            FloatArray(pieceCount)

        private val startY =
            FloatArray(pieceCount)

        private val targetX =
            FloatArray(pieceCount)

        private val targetY =
            FloatArray(pieceCount)

        private val rotation =
            FloatArray(pieceCount)

        private val phase =
            FloatArray(pieceCount)

        private val size =
            FloatArray(pieceCount)

        private var width = 0
        private var height = 0

        private var animationStart = 0L

        /*
         * Animation length:
         *
         * 0.0 - 0.35  pieces come together
         * 0.35 - 0.55 photo is fully revealed
         * 0.55 - 0.70 zoom out
         * 0.70 - 0.85 zoom in
         * 0.85 - 1.00 pieces scatter again
         */

        private val animationLength = 12000L

        override fun onVisibilityChanged(
            visible: Boolean
        ) {
            running = visible

            if (visible) {
                loadPhotos()
                animationStart = System.currentTimeMillis()
                startDrawing()
            } else {
                stopDrawing()
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(
                holder,
                format,
                width,
                height
            )

            this.width = width
            this.height = height

            loadPhotos()
            createPieces()

            if (running) {
                startDrawing()
            }
        }

        override fun onSurfaceDestroyed(
            holder: SurfaceHolder
        ) {
            stopDrawing()
            super.onSurfaceDestroyed(holder)
        }

        private fun loadPhotos() {

            val prefs =
                getSharedPreferences(
                    "LiveFlow",
                    MODE_PRIVATE
                )

            val backgroundUri =
                prefs.getString(
                    "background_uri",
                    null
                )

            val flowUri =
                prefs.getString(
                    "flow_uri",
                    null
                )

            backgroundBitmap =
                decodePhoto(backgroundUri)

            flowBitmap =
                decodePhoto(flowUri)
        }

        private fun decodePhoto(
            value: String?
        ): Bitmap? {

            if (value == null) {
                return null
            }

            return try {

                val uri =
                    Uri.parse(value)

                val stream =
                    contentResolver
                        .openInputStream(uri)

                val bitmap =
                    BitmapFactory
                        .decodeStream(stream)

                stream?.close()

                bitmap

            } catch (_: Exception) {
                null
            }
        }

        private fun createPieces() {

            if (width <= 0 || height <= 0) {
                return
            }

            val columns = 6
            val rows = 6

            val cellWidth =
                width.toFloat() / columns

            val cellHeight =
                height.toFloat() / rows

            var index = 0

            for (row in 0 until rows) {

                for (column in 0 until columns) {

                    if (index >= pieceCount) {
                        break
                    }

                    val tx =
                        column * cellWidth +
                            cellWidth / 2f

                    val ty =
                        row * cellHeight +
                            cellHeight / 2f

                    targetX[index] = tx
                    targetY[index] = ty

                    /*
                     * Start positions are deliberately
                     * scattered outside their final location.
                     */
                    startX[index] =
                        random.nextFloat() *
                            width

                    startY[index] =
                        random.nextFloat() *
                            height

                    rotation[index] =
                        random.nextFloat() *
                            360f - 180f

                    phase[index] =
                        random.nextFloat() *
                            6.28f

                    size[index] =
                        min(
                            cellWidth,
                            cellHeight
                        ) * 0.82f

                    index++
                }
            }
        }

        private fun startDrawing() {

            if (thread?.isAlive == true) {
                return
            }

            thread =
                Thread {

                    while (running) {

                        drawFrame()

                        try {
                            Thread.sleep(16L)
                        } catch (_: InterruptedException) {
                            break
                        }
                    }
                }

            thread?.start()
        }

        private fun stopDrawing() {

            running = false

            thread?.interrupt()
            thread = null
        }

        private fun drawFrame() {

            val holder =
                surfaceHolder

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

                canvas.drawColor(
                    Color.BLACK
                )

                drawBackground(canvas)

                val elapsed =
                    (
                        System.currentTimeMillis() -
                            animationStart
                        )

                val raw =
                    (
                        elapsed %
                            animationLength
                        ).toFloat() /
                        animationLength

                drawFlowAnimation(
                    canvas,
                    raw
                )

            } finally {

                try {
                    holder.unlockCanvasAndPost(
                        canvas
                    )
                } catch (_: Exception) {
                }
            }
        }

        private fun drawBackground(
            canvas: Canvas
        ) {

            val bitmap =
                backgroundBitmap

            if (bitmap == null) {
                return
            }

            drawCoverBitmap(
                canvas,
                bitmap,
                1f
            )
        }

        private fun drawFlowAnimation(
            canvas: Canvas,
            time: Float
        ) {

            val bitmap =
                flowBitmap
                    ?: return

            /*
             * First part:
             * scattered pieces move toward
             * their correct positions.
             */
            if (time < 0.35f) {

                val p =
                    smoothStep(
                        time / 0.35f
                    )

                drawPieces(
                    canvas,
                    bitmap,
                    p,
                    false
                )

                return
            }

            /*
             * Full photo reveal.
             */
            if (time < 0.55f) {

                drawFullPhoto(
                    canvas,
                    bitmap,
                    1f
                )

                return
            }

            /*
             * Zoom OUT.
             */
            if (time < 0.70f) {

                val p =
                    smoothStep(
                        (time - 0.55f) /
                            0.15f
                    )

                val zoom =
                    1f - 0.12f * p

                drawFullPhoto(
                    canvas,
                    bitmap,
                    zoom
                )

                return
            }

            /*
             * Zoom IN again.
             */
            if (time < 0.85f) {

                val p =
                    smoothStep(
                        (time - 0.70f) /
                            0.15f
                    )

                val zoom =
                    0.88f +
                        0.12f * p

                drawFullPhoto(
                    canvas,
                    bitmap,
                    zoom
                )

                return
            }

            /*
             * Final part:
             * photo breaks back into pieces.
             */
            val p =
                smoothStep(
                    (time - 0.85f) /
                        0.15f
                )

            drawPieces(
                canvas,
                bitmap,
                1f - p,
                true
            )
        }

        private fun drawPieces(
            canvas: Canvas,
            bitmap: Bitmap,
            progress: Float,
            scattered: Boolean
        ) {

            val prefs =
                getSharedPreferences(
                    "LiveFlow",
                    MODE_PRIVATE
                )

            val shape =
                prefs.getInt(
                    "shape_mode",
                    0
                )

            val columns = 6
            val rows = 6

            val cellWidth =
                width.toFloat() /
                    columns

            val cellHeight =
                height.toFloat() /
                    rows

            var index = 0

            for (row in 0 until rows) {

                for (column in 0 until columns) {

                    if (index >= pieceCount) {
                        break
                    }

                    val targetCX =
                        targetX[index]

                    val targetCY =
                        targetY[index]

                    val x =
                        lerp(
                            startX[index],
                            targetCX,
                            progress
                        )

                    val y =
                        lerp(
                            startY[index],
                            targetCY,
                            progress
                        )

                    val movingX =
                        x +
                            sin(
                                phase[index] +
                                    progress * 8f
                            ) *
                            cellWidth *
                            0.16f

                    val movingY =
                        y +
                            cos(
                                phase[index] +
                                    progress * 7f
                            ) *
                            cellHeight *
                            0.16f

                    val currentRotation =
                        rotation[index] *
                            (1f - progress)

                    val left =
                        column * cellWidth

                    val top =
                        row * cellHeight

                    val right =
                        left + cellWidth

                    val bottom =
                        top + cellHeight

                    val source =
                        Rect(
                            (
                                bitmap.width *
                                    left /
                                    width
                                ).toInt(),

                            (
                                bitmap.height *
                                    top /
                                    height
                                ).toInt(),

                            (
                                bitmap.width *
                                    right /
                                    width
                                ).toInt(),

                            (
                                bitmap.height *
                                    bottom /
                                    height
                                ).toInt()
                        )

                    val halfWidth =
                        cellWidth * 0.47f

                    val halfHeight =
                        cellHeight * 0.47f

                    val destination =
                        RectF(
                            movingX -
                                halfWidth,

                            movingY -
                                halfHeight,

                            movingX +
                                halfWidth,

                            movingY +
                                halfHeight
                        )

                    canvas.save()

                    canvas.rotate(
                        currentRotation,
                        movingX,
                        movingY
                    )

                    val path =
                        createShapePath(
                            shape,
                            movingX,
                            movingY,
                            halfWidth,
                            halfHeight,
                            index
                        )

                    canvas.clipPath(path)

                    canvas.drawBitmap(
                        bitmap,
                        source,
                        destination,
                        paint
                    )

                    canvas.restore()

                    index++
                }
            }
        }

        private fun createShapePath(
            shapeMode: Int,
            cx: Float,
            cy: Float,
            halfWidth: Float,
            halfHeight: Float,
            index: Int
        ): Path {

            val path = Path()

            var actualShape =
                shapeMode

            /*
             * Random mode:
             * randomly choose a shape for
             * every piece.
             */
            if (shapeMode == 5) {

                actualShape =
                    index % 5
            }

            when (actualShape) {

                /*
                 * Circle
                 */
                0 -> {

                    path.addOval(
                        RectF(
                            cx - halfWidth,
                            cy - halfHeight,
                            cx + halfWidth,
                            cy + halfHeight
                        ),
                        Path.Direction.CW
                    )
                }

                /*
                 * Rectangle
                 */
                1 -> {

                    path.addRect(
                        RectF(
                            cx - halfWidth,
                            cy - halfHeight,
                            cx + halfWidth,
                            cy + halfHeight
                        ),
                        Path.Direction.CW
                    )
                }

                /*
                 * Triangle
                 */
                2 -> {

                    addPolygon(
                        path,
                        cx,
                        cy,
                        halfWidth,
                        3
                    )
                }

                /*
                 * Pentagon
                 */
                3 -> {

                    addPolygon(
                        path,
                        cx,
                        cy,
                        halfWidth,
                        5
                    )
                }

                /*
                 * Hexagon
                 */
                else -> {

                    addPolygon(
                        path,
                        cx,
                        cy,
                        halfWidth,
                        6
                    )
                }
            }

            return path
        }

        private fun addPolygon(
            path: Path,
            cx: Float,
            cy: Float,
            radius: Float,
            sides: Int
        ) {

            for (i in 0 until sides) {

                val angle =
                    -Math.PI / 2.0 +
                        i *
                        2.0 *
                        Math.PI /
                        sides

                val x =
                    cx +
                        cos(angle).toFloat() *
                        radius

                val y =
                    cy +
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

        private fun drawFullPhoto(
            canvas: Canvas,
            bitmap: Bitmap,
            zoom: Float
        ) {

            val centerX =
                width / 2f

            val centerY =
                height / 2f

            val drawWidth =
                width * zoom

            val drawHeight =
                height * zoom

            val destination =
                RectF(
                    centerX -
                        drawWidth / 2f,

                    centerY -
                        drawHeight / 2f,

                    centerX +
                        drawWidth / 2f,

                    centerY +
                        drawHeight / 2f
                )

            canvas.drawBitmap(
                bitmap,
                null,
                destination,
                paint
            )
        }

        private fun drawCoverBitmap(
            canvas: Canvas,
            bitmap: Bitmap,
            zoom: Float
        ) {

            val scale =
                maxOf(
                    width.toFloat() /
                        bitmap.width,

                    height.toFloat() /
                        bitmap.height
                ) * zoom

            val drawWidth =
                bitmap.width * scale

            val drawHeight =
                bitmap.height * scale

            val left =
                (width - drawWidth) / 2f

            val top =
                (height - drawHeight) / 2f

            canvas.drawBitmap(
                bitmap,
                null,
                RectF(
                    left,
                    top,
                    left + drawWidth,
                    top + drawHeight
                ),
                paint
            )
        }

        private fun lerp(
            a: Float,
            b: Float,
            t: Float
        ): Float {

            return a +
                (b - a) * t
        }

        private fun smoothStep(
            value: Float
        ): Float {

            val t =
                value.coerceIn(
                    0f,
                    1f
                )

            return t * t *
                (3f - 2f * t)
        }
    }
}
