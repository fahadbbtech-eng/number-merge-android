package com.bluebell.numbermerge

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.abs
import kotlin.random.Random

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

  private var gameThread: Thread? = null
  private var isPlaying = false

  private val size = 4
  private var grid = Array(size) { IntArray(size) }
  private var score = 0
  private var best = 0
  private var gameOver = false
  private var won = false

  private var startX = 0f
  private var startY = 0f

  private var animRow = -1
  private var animCol = -1
  private var animTicks = 0
  private val animMaxTicks = 6

  private val paint = Paint().apply { isAntiAlias = true }

  init {
    holder.addCallback(this)
    spawnTile()
    spawnTile()
  }

  override fun surfaceCreated(holder: SurfaceHolder) {
    isPlaying = true
    gameThread = Thread(this)
    gameThread!!.start()
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

  override fun surfaceDestroyed(holder: SurfaceHolder) {
    isPlaying = false
    gameThread?.join()
  }

  override fun run() {
    while (isPlaying) {
      if (animTicks > 0) animTicks--
      draw()
      Thread.sleep(30)
    }
  }

  private fun spawnTile() {
    val empty = mutableListOf<Pair<Int, Int>>()
    for (r in 0 until size) {
      for (c in 0 until size) {
        if (grid[r][c] == 0) empty.add(Pair(r, c))
      }
    }
    if (empty.isEmpty()) return
    val (r, c) = empty[Random.nextInt(empty.size)]
    grid[r][c] = if (Random.nextFloat() < 0.9f) 2 else 4
    animRow = r
    animCol = c
    animTicks = animMaxTicks
  }

  private fun canMove(): Boolean {
    for (r in 0 until size) {
      for (c in 0 until size) {
        if (grid[r][c] == 0) return true
        if (c < size - 1 && grid[r][c] == grid[r][c + 1]) return true
        if (r < size - 1 && grid[r][c] == grid[r + 1][c]) return true
      }
    }
    return false
  }

  private fun moveLeft(): Boolean {
    var moved = false
    for (r in 0 until size) {
      val row = grid[r].filter { it != 0 }.toMutableList()
      var i = 0
      while (i < row.size - 1) {
        if (row[i] == row[i + 1]) {
          row[i] = row[i] * 2
          score += row[i]
          if (row[i] == 2048) won = true
          row.removeAt(i + 1)
        }
        i++
      }
      while (row.size < size) row.add(0)
      for (c in 0 until size) {
        if (grid[r][c] != row[c]) moved = true
        grid[r][c] = row[c]
      }
    }
    return moved
  }

  private fun rotateGrid() {
    val newGrid = Array(size) { IntArray(size) }
    for (r in 0 until size) {
      for (c in 0 until size) {
        newGrid[c][size - 1 - r] = grid[r][c]
      }
    }
    grid = newGrid
  }

  private fun swipe(direction: String) {
    if (gameOver) return
    var rotations = 0
    when (direction) {
      "up" -> rotations = 3
      "right" -> rotations = 2
      "down" -> rotations = 1
      "left" -> rotations = 0
    }
    repeat(rotations) { rotateGrid() }
    val moved = moveLeft()
    repeat((4 - rotations) % 4) { rotateGrid() }

    if (moved) {
      spawnTile()
      if (score > best) best = score
      if (!canMove()) gameOver = true
    }
  }

  private fun tileColor(value: Int): Int {
    return when (value) {
      0 -> Color.rgb(60, 58, 50)
      2 -> Color.rgb(238, 228, 218)
      4 -> Color.rgb(237, 224, 200)
      8 -> Color.rgb(242, 177, 121)
      16 -> Color.rgb(245, 149, 99)
      32 -> Color.rgb(246, 124, 95)
      64 -> Color.rgb(246, 94, 59)
      128 -> Color.rgb(237, 207, 114)
      256 -> Color.rgb(237, 204, 97)
      512 -> Color.rgb(237, 200, 80)
      1024 -> Color.rgb(237, 197, 63)
      2048 -> Color.rgb(237, 194, 46)
      else -> Color.rgb(60, 58, 50)
    }
  }

  private fun draw() {
    if (!holder.surface.isValid) return
    val canvas: Canvas = holder.lockCanvas()

    canvas.drawColor(Color.rgb(35, 33, 28))

    paint.color = Color.WHITE
    paint.textSize = 55f
    canvas.drawText("Number Merge", 40f, 100f, paint)
    paint.textSize = 40f
    canvas.drawText("Score: " + score, 40f, 155f, paint)
    canvas.drawText("Best: " + best, 40f, 200f, paint)

    val boardTop = 240f
    val boardSize = width - 80f
    val cell = boardSize / size
    val left = 40f

    paint.color = Color.rgb(50, 48, 42)
    canvas.drawRect(left, boardTop, left + boardSize, boardTop + boardSize, paint)

    for (r in 0 until size) {
      for (c in 0 until size) {
        val value = grid[r][c]
        val x = left + c * cell + 8
        val y = boardTop + r * cell + 8
        var w = cell - 16
        var h = cell - 16
        var dx = x
        var dy = y
        if (r == animRow && c == animCol && animTicks > 0) {
          val progress = 1f - (animTicks.toFloat() / animMaxTicks.toFloat())
          val scale = 0.5f + 0.5f * progress
          val newW = w * scale
          val newH = h * scale
          dx = x + (w - newW) / 2
          dy = y + (h - newH) / 2
          w = newW
          h = newH
        }
        paint.color = tileColor(value)
        canvas.drawRect(dx, dy, dx + w, dy + h, paint)
        if (value != 0) {
          paint.color = if (value <= 4) Color.rgb(90, 85, 75) else Color.WHITE
          paint.textSize = 50f
          val text = value.toString()
          val textWidth = paint.measureText(text)
          canvas.drawText(text, x + (cell - 16) / 2 - textWidth / 2, y + (cell - 16) / 2 + 18, paint)
        }
      }
    }

    if (gameOver) {
      paint.color = Color.argb(200, 0, 0, 0)
      canvas.drawRect(left, boardTop, left + boardSize, boardTop + boardSize, paint)
      paint.color = Color.WHITE
      paint.textSize = 60f
      val msg = if (won) "Aap Jeet Gaye!" else "Game Over"
      val msgWidth = paint.measureText(msg)
      canvas.drawText(msg, left + boardSize / 2 - msgWidth / 2, boardTop + boardSize / 2, paint)
      paint.textSize = 40f
      val restart = "Phir Se Try Karo"
      val restartWidth = paint.measureText(restart)
      canvas.drawText(restart, left + boardSize / 2 - restartWidth / 2, boardTop + boardSize / 2 + 60f, paint)
    }

    holder.unlockCanvasAndPost(canvas)
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        startX = event.x
        startY = event.y
      }
      MotionEvent.ACTION_UP -> {
        if (gameOver) {
          grid = Array(size) { IntArray(size) }
          score = 0
          gameOver = false
          won = false
          spawnTile()
          spawnTile()
          return true
        }
        val dx = event.x - startX
        val dy = event.y - startY
        if (abs(dx) > abs(dy)) {
          if (abs(dx) > 60) {
            if (dx > 0) swipe("right") else swipe("left")
          }
        } else {
          if (abs(dy) > 60) {
            if (dy > 0) swipe("down") else swipe("up")
          }
        }
      }
    }
    return true
  }
}
