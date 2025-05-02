class CoordinateConverter {
    fun convertToBoardCoordinates(screenX: Float, screenY: Float, boardSize: Int): Pair<Int, Int> {
        val cellSize = screenX / boardSize
        val x = (screenX / cellSize).toInt()
        val y = (screenY / cellSize).toInt()
        return Pair(x, y)
    }

    fun convertToScreenCoordinates(boardX: Int, boardY: Int, boardSize: Int): Pair<Float, Float> {
        val cellSize = boardSize.toFloat()
        val screenX = boardX * cellSize
        val screenY = boardY * cellSize
        return Pair(screenX, screenY)
    }
}