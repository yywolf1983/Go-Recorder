data class GameRecord(
    val boardState: List<List<Int>>, // 0 for empty, 1 for black, 2 for white
    val blackPlayer: String,
    val whitePlayer: String,
    val moves: List<Pair<Int, Int>>, // List of moves represented as (x, y) coordinates
    val gameResult: String // e.g., "Black wins", "White wins", "Draw"
)