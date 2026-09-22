package com.example.liveflowwallpaper

import android.graphics.*
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.cos
import kotlin.math.sin

class FlowWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = FlowEngine()

    private inner class FlowEngine : Engine() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            strokeCap = Paint.Cap.ROUND
        }

        private var running = false
        private var thread: Thread? = null

        override fun onVisibilityChanged(visible: Boolean) {
            running = visible
            if (visible) {
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
            super.onSurfaceChanged(holder, format, width, height)
            if (running) {
                startDrawing()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            stopDrawing()
            super.onSurfaceDestroyed(holder)
        }

        private fun startDrawing() {
            if (thread?.isAlive == true) return

            running = true

            thread = Thread {
                var time = 0f

                while (running) {
                    val frameStart = System.nanoTime()

                    drawFrame(time)
                    time += 0.018f

                    val elapsedMs =
                        (System.nanoTime() - frameStart) / 1_000_000L

                    val sleepMs =
                        (16L - elapsedMs).coerceAtLeast(1L)

                    try {
                        Thread.sleep(sleepMs)
                    } catch (_: InterruptedException) {
                    }
                }
            }.also { it.start() }
        }

        private fun stopDrawing() {
            running = false
            thread?.interrupt()
            thread = null
        }

        private fun drawFrame(t: Float) {
            val holder = surfaceHolder

            val canvas = try {
                holder.lockCanvas()
            } catch (_: Exception) {
                null
            } ?: return

            try {
                val w = canvas.width.toFloat()
                val h = canvas.height.toFloat()
                val minDim = minOf(w, h)

                // Animated background
                val bg = LinearGradient(
                    0f, 0f, w, h,
                    intArrayOf(
                        Color.rgb(7, 11, 24),
                        Color.rgb(13, 22, 48),
                        Color.rgb(5, 10, 22)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )

                canvas.drawRect(
                    0f,
                    0f,
                    w,
                    h,
                    Paint().apply {
                        shader = bg
                    }
                )

                // Moving luminous orbs
                drawOrb(
                    canvas,
                    w * 0.23f + cos(t * 0.52f) * w * 0.16f,
                    h * 0.30f + sin(t * 0.67f) * h * 0.13f,
                    minDim * 0.34f,
                    Color.argb(90, 40, 145, 255)
                )

                drawOrb(
                    canvas,
                    w * 0.76f + sin(t * 0.43f) * w * 0.13f,
                    h * 0.58f + cos(t * 0.58f) * h * 0.16f,
                    minDim * 0.38f,
                    Color.argb(70, 110, 70, 255)
                )

                // Flowing ribbons
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = minDim * 0.010f

                paint.shader = LinearGradient(
                    0f,
                    0f,
                    w,
                    h,
                    intArrayOf(
                        Color.argb(20, 80, 190, 255),
                        Color.argb(185, 80, 190, 255),
                        Color.argb(80, 175, 110, 255),
                        Color.argb(15, 80, 190, 255)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )

                for (ribbon in 0..3) {
                    val path = Path()
                    val baseY = h * (0.28f + ribbon * 0.16f)

                    path.moveTo(-w * 0.05f, baseY)

                    var x = -w * 0.05f

                    while (x <= w * 1.05f) {
                        val nx = x / w

                        val y = baseY +
                            sin(
                                nx * 6.4f +
                                    t * (0.75f + ribbon * 0.08f)
                            ) * h * 0.045f +
                            cos(
                                nx * 3.1f -
                                    t * 0.45f
                            ) * h * 0.025f

                        path.lineTo(x, y)

                        x += w / 80f
                    }

                    canvas.drawPath(path, paint)
                }

                // Star-like particles
                paint.shader = null
                paint.style = Paint.Style.FILL

                for (i in 0 until 28) {
                    val px =
                        ((i * 83.0 +
                            t * (7 + i % 4) * 4) %
                            (w + 40)) - 20

                    val py =
                        ((i * 137.0 +
                            sin(t * 0.3f + i) * 18) %
                            (h + 40)) - 20

                    val radius =
                        minDim *
                            (0.0015f + (i % 3) * 0.001f)

                    paint.color =
                        Color.argb(
                            90 + (i % 4) * 30,
                            180,
                            215,
                            255
                        )

                    canvas.drawCircle(
                        px.toFloat(),
                        py.toFloat(),
                        radius,
                        paint
                    )
                }

            } finally {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (_: Exception) {
                }
            }
        }

        private fun drawOrb(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            radius: Float,
            color: Int
        ) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG)

            p.shader = RadialGradient(
                cx,
                cy,
                radius,
                intArrayOf(
                    color,
                    Color.argb(
                        20,
                        Color.red(color),
                        Color.green(color),
                        Color.blue(color)
                    ),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )

            canvas.drawCircle(cx, cy, radius, p)
        }
    }
}
