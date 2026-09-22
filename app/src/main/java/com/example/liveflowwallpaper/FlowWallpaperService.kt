package com.example.liveflowwallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class FlowWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return FlowEngine()
    }

    private inner class FlowEngine : Engine() {

        private var running = false
        private var thread: Thread? = null

        private var bitmap: Bitmap? = null

        private val paint =
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        private val random =
            Random(System.currentTimeMillis())

        private data class PolygonPiece(
            val x: Float,
            val y: Float,
            val radius: Float,
            val sides: Int,
            val rotation: Float,
            val phase: Float,
            val speed: Float,
            val distance: Float
        )

        private var pieces =
            emptyList<PolygonPiece>()

        override fun onVisibilityChanged(visible: Boolean) {

            running = visible

            if (visible) {
                loadPhoto()
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

            loadPhoto()

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

        private fun loadPhoto() {

            val saved =
                getSharedPreferences(
                    "LiveFlow",
                    MODE_PRIVATE
                )
                    .getString("photo_uri", null)

            if (saved == null) {
                bitmap = null
                return
            }

            try {

                val uri = Uri.parse(saved)

                val stream =
                    contentResolver.openInputStream(uri)

                val original =
                    BitmapFactory.decodeStream(stream)

                stream?.close()

                bitmap = original

            } catch (_: Exception) {

                bitmap = null
            }
        }

        private fun startDrawing() {

            if (thread?.isAlive == true) {
                return
            }

            running = true

            thread = Thread {

                var time = 0f

                while (running) {

                    val frameStart =
                        System.nanoTime()

                    drawFrame(time)

                    time += 0.018f

                    val elapsed =
                        (
                            System.nanoTime() -
                                frameStart
                            ) / 1_000_000L

                    val sleep =
                        (16L - elapsed)
                            .coerceAtLeast(1L)

                    try {
                        Thread.sleep(sleep)
                    } catch (_: InterruptedException) {
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

        private fun createPieces(
            width: Float,
            height: Float
        ) {

            val list =
                ArrayList<PolygonPiece>()

            val minDimension =
                minOf(width, height)

            val columns = 4
            val rows = 6

            val cellWidth =
                width / columns

            val cellHeight =
                height / rows

            for (row in 0 until rows) {

                for (column in 0 until columns) {

                    val centerX =
                        column * cellWidth +
                            cellWidth / 2f

                    val centerY =
                        row * cellHeight +
                            cellHeight / 2f

                    val sides =
                        random.nextInt(4, 8)

                    val radius =
                        minOf(
                            cellWidth,
                            cellHeight
                        ) * 0.48f

                    val angle =
                        random.nextFloat() * 360f

                    val phase =
                        random.nextFloat() *
                            6.28f

                    val speed =
                        0.5f +
                            random.nextFloat() * 0.8f

                    val distance =
                        minDimension *
                            (0.15f +
                                random.nextFloat() * 0.55f)

                    list.add(
                        PolygonPiece(
                            centerX,
                            centerY,
                            radius,
                            sides,
                            angle,
                            phase,
                            speed,
                            distance
                        )
                    )
                }
            }

            pieces = list
        }

        private fun drawFrame(time: Float) {

            val holder =
                surfaceHolder

            val canvas =
                try {
                    holder.lockCanvas()
                } catch (_: Exception) {
                    null
                } ?: return

            try {

                val width =
                    canvas.width.toFloat()

                val height =
                    canvas.height.toFloat()

                val image =
                    bitmap

                if (image == null) {

                    canvas.drawColor(
                        Color.rgb(
                            7,
                            11,
                            24
                        )
                    )

                    drawWaitingMessage(
                        canvas,
                        width,
                        height
                    )

                    return
                }

                if (
                    pieces.size != 24
                ) {
                    createPieces(
                        width,
                        height
                    )
                }

                /*
                 * One complete animation cycle.
                 *
                 * 0.0 -> pieces scattered
                 * 0.5 -> pieces return
                 * 1.0 -> full picture revealed
                 */

                val cycle =
                    (time / 7.0f) % 1.0f

                val reveal =
                    smoothStep(
                        cycle
                    )

                drawPolygonPhoto(
                    canvas,
                    image,
                    width,
                    height,
                    reveal,
                    time
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

        private fun drawPolygonPhoto(
            canvas: Canvas,
            image: Bitmap,
            width: Float,
            height: Float,
            reveal: Float,
            time: Float
        ) {

            /*
             * First draw a very dark background.
             */
            canvas.drawColor(
                Color.rgb(
                    3,
                    5,
                    12
                )
            )

            /*
             * Draw each polygon as a moving
             * window containing its correct
             * section of the photograph.
             */

            for (piece in pieces) {

                val wave =
                    sin(
                        time * piece.speed +
                            piece.phase
                    ).toFloat()

                /*
                 * At the beginning, pieces are
                 * pushed away from their final
                 * positions.
                 *
                 * As reveal approaches 1,
                 * they return to the correct
                 * position.
                 */
                val scatter =
                    (1f - reveal) *
                        piece.distance

                val direction =
                    piece.rotation *
                        0.017453292f

                val dx =
                    cos(direction) *
                        scatter

                val dy =
                    sin(direction) *
                        scatter

                val centerX =
                    piece.x + dx

                val centerY =
                    piece.y + dy

                /*
                 * Zoom effect.
                 */
                val zoom =
                    0.55f +
                        reveal * 0.45f +
                        wave * 0.08f

                /*
                 * Rotation effect.
                 */
                val rotation =
                    piece.rotation +
                        wave * 35f +
                        (1f - reveal) * 180f

                val path =
                    createPolygonPath(
                        centerX,
                        centerY,
                        piece.radius * zoom,
                        piece.sides,
                        rotation
                    )

                canvas.save()

                /*
                 * The polygon becomes a mask.
                 */
                canvas.clipPath(path)

                /*
                 * Calculate how the original
                 * photograph fills the screen.
                 */
                val matrix =
                    createImageMatrix(
                        image,
                        width,
                        height
                    )

                /*
                 * Move the image together with
                 * its polygon piece.
                 *
                 * This is what makes each polygon
                 * contain the correct part of the
                 * original photograph.
                 */
                matrix.postTranslate(
                    centerX - piece.x,
                    centerY - piece.y
                )

                matrix.postScale(
                    zoom,
                    zoom,
                    centerX,
                    centerY
                )

                paint.alpha = 255

                canvas.drawBitmap(
                    image,
                    matrix,
                    paint
                )

                canvas.restore()

                /*
                 * Soft outline around every piece.
                 */
                paint.style =
                    Paint.Style.STROKE

                paint.strokeWidth = 2.5f

                paint.color =
                    Color.argb(
                        100,
                        255,
                        255,
                        255
                    )

                canvas.drawPath(
                    path,
                    paint
                )

                paint.style =
                    Paint.Style.FILL
            }

            /*
             * During the final part of the cycle,
             * gently reveal the entire photograph.
             *
             * This creates the "everything becomes
             * one full picture" moment.
             */
            if (reveal > 0.82f) {

                val alpha =
                    (
                        (reveal - 0.82f) /
                            0.18f *
                            255f
                        )
                        .toInt()
                        .coerceIn(
                            0,
                            255
                        )

                paint.alpha = alpha

                val matrix =
                    createImageMatrix(
                        image,
                        width,
                        height
                    )

                canvas.drawBitmap(
                    image,
                    matrix,
                    paint
                )

                paint.alpha = 255
            }
        }

        private fun createPolygonPath(
            cx: Float,
            cy: Float,
            radius: Float,
            sides: Int,
            rotation: Float
        ): Path {

            val path =
                Path()

            val rotationRadians =
                Math.toRadians(
                    rotation.toDouble()
                )

            for (i in 0 until sides) {

                val angle =
                    rotationRadians +
                        i.toDouble() *
                        Math.PI * 2.0 /
                        sides.toDouble()

                val x =
                    cx +
                        cos(angle)
                            .toFloat() *
                        radius

                val y =
                    cy +
                        sin(angle)
                            .toFloat() *
                        radius

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

        private fun createImageMatrix(
            bitmap: Bitmap,
            width: Float,
            height: Float
        ): Matrix {

            val matrix =
                Matrix()

            val bitmapWidth =
                bitmap.width.toFloat()

            val bitmapHeight =
                bitmap.height.toFloat()

            /*
             * Center-crop the photograph so
             * it completely covers the screen.
             */
            val scale =
                maxOf(
                    width / bitmapWidth,
                    height / bitmapHeight
                )

            val scaledWidth =
                bitmapWidth * scale

            val scaledHeight =
                bitmapHeight * scale

            val left =
                (width - scaledWidth) / 2f

            val top =
                (height - scaledHeight) / 2f

            matrix.setScale(
                scale,
                scale
            )

            matrix.postTranslate(
                left,
                top
            )

            return matrix
        }

        private fun smoothStep(
            value: Float
        ): Float {

            val x =
                value.coerceIn(
                    0f,
                    1f
                )

            return x * x *
                (3f - 2f * x)
        }

        private fun drawWaitingMessage(
            canvas: Canvas,
            width: Float,
            height: Float
        ) {

            paint.color =
                Color.WHITE

            paint.textSize =
                42f

            paint.textAlign =
                Paint.Align.CENTER

            canvas.drawText(
                "Choose a photo",
                width / 2f,
                height / 2f,
                paint
            )
        }
    }
}
