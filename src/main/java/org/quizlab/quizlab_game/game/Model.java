package org.quizlab.quizlab_game.game;

import javafx.geometry.Point2D;
import javafx.fxml.FXML;

import java.util.*;

/**
 * 
 * Modelo del juego
 *
 */
public class Model {
	@FXML
	private int rowCount;
	@FXML
	private int columnCount;

	/**
	 * 
	 * Direcciones que puede utilizar el jugador
	 *
	 */
	public enum Direction {
		UP, DOWN, LEFT, RIGHT, NONE
	};

	/**
	 * 
	 * Representación de cada celda dentro del juego
	 *
	 */
	public enum CellValue {
		EMPTY, FISH, WALL, ENEMY1HOME, ENEMY2HOME, PLAYERHOME
	};

	private CellValue[][] grid;

	private int score;
	private int level;
	private int fishCount;

	private boolean hasLost;
	private boolean hasWon;

	private Point2D playerLocation;
	private Point2D playerVelocity;

	private Point2D enemy1Location;
	private Point2D enemy1Velocity;

	private Point2D enemy2Location;
	private Point2D enemy2Velocity;

	private static Direction lastDirection;
	private static Direction currentDirection;

	/**
	 * Inicia un nuevo juego
	 */
	public Model() {
		this.startNewGame();
	}

	/**
	 * Configura la cuadrícula según la matriz del nivel, coloca el personaje y los
	 * enemigos en sus ubicaciones iniciales. "W" indica un muro, "E" indica un
	 * cuadrado vacío, "F" indica un pescado, "1" o "2" indica el punto de inicio de
	 * los enemigos "P" indica el punto de inicio del personaje
	 *
	 * @param level Matriz que tiene la configuración del escenario
	 * 
	 */
	public void initializeLevel(char[][] level) {
		this.rowCount = level.length;
		this.columnCount = level[0].length;

		createGrid(level);

		currentDirection = Direction.NONE;
		lastDirection = Direction.NONE;
	}

	/**
	 * Crea la cuadrícula que define el escenario
	 * 
	 * @param level Matriz que tiene la configuración del escenario
	 * 
	 */
	private void createGrid(char[][] level) {
		this.grid = new CellValue[this.rowCount][this.columnCount];
		for (int row = 0; row < this.rowCount; row++) {
			for (int column = 0; column < this.columnCount; column++) {
				char value = level[row][column];
				CellValue cellValue = parseCellValue(value, row, column);
				this.grid[row][column] = cellValue;

			}
		}
	}

	/**
	 * Según el valor de la cuadrícula se establece el tipo de celda que se mostrará
	 * 
	 * @param value  Valor de la cuadrícula
	 * @param row    Fila de la cuadrícula
	 * @param column Columna de la cuadrícula
	 * @return Valor que tomará la cuadrícula
	 */
	private CellValue parseCellValue(char value, int row, int column) {
		CellValue cellValue;
		switch (value) {
		case 'W':
			cellValue = CellValue.WALL;
			break;
		case 'F':
			cellValue = CellValue.FISH;
			this.fishCount++;
			break;
		case '1':
			cellValue = CellValue.ENEMY1HOME;
			this.enemy1Location = new Point2D(row, column);
			this.enemy1Velocity = new Point2D(-1, 0);
			break;
		case '2':
			cellValue = CellValue.ENEMY2HOME;
			this.enemy2Location = new Point2D(row, column);
			this.enemy2Velocity = new Point2D(-1, 0);
			break;
		case 'P':
			cellValue = CellValue.PLAYERHOME;
			this.playerLocation = new Point2D(row, column);
			this.playerVelocity = new Point2D(0, 0);
			break;
		default:
			cellValue = CellValue.EMPTY;
			break;
		}
		return cellValue;
	}

	/**
	 * Inicia el escenario del nivel
	 */
	public void startNewGame() {
		this.hasLost = false;
		this.hasWon = false;
		this.fishCount = 0;
		this.rowCount = 0;
		this.columnCount = 0;
		this.score = 0;
		this.level = 1;
		this.initializeLevel(Controller.getLevelData(0));
	}

	/**
	 * Inicia el escenario del nivel
	 */
	public void continueGame() {
		this.hasLost = false;
		this.hasWon = false;
	}

	/**
	 * Inicia el escenario del nivel para el siguiente nivel
	 *
	 */
	public void startNextLevel() {
		this.level++;
		this.rowCount = 0;
		this.columnCount = 0;
		this.hasWon = false;
		try {
			initializeLevel(Controller.getLevelData(level - 1));
		} catch (ArrayIndexOutOfBoundsException e) {
			finishGame();
		}
	}

	/**
	 * Finaliza el juego cuando ya no hay más niveles
	 *
	 */
	private void finishGame() {
		this.hasWon = true;
		this.level--;
	}

	/**
	 * Mueva el personaje según la dirección indicada por el jugador
	 * 
	 * @param direction La dirección ingresada
	 */
	public void movePlayer(Direction direction) {
		Point2D potentialPlayerVelocity = changeVelocity(direction);
		Point2D potentialPlayerLocation = playerLocation.add(potentialPlayerVelocity);

		if (direction.equals(lastDirection)) {
			handleSameDirectionMovement(potentialPlayerVelocity, potentialPlayerLocation);
		} else {
			handleDifferentDirectionMovement(direction, potentialPlayerVelocity, potentialPlayerLocation);
		}
	}

	/**
	 * 
	 * Controla el movimiento del jugador cuando va en la misma dirección que antes
	 * 
	 * @param potentialPlayerVelocity Velocidad potencial a la que se quiere mover
	 *                                el usuario
	 * @param potentialPlayerLocation Dirección potencial a la que se quiere mover
	 *                                el usuario
	 */
	private void handleSameDirectionMovement(Point2D potentialPlayerVelocity, Point2D potentialPlayerLocation) {
		if (gridContainsWall(potentialPlayerLocation)) {
			stopPlayerMovement();
		} else {
			movePlayerToLocation(potentialPlayerVelocity, potentialPlayerLocation);
		}
	}

	/**
	 * 
	 * Controla el movimiento del jugador cuando va en una dirección diferente a la
	 * de antes
	 * 
	 * @param direction               La dirección ingresada
	 * @param potentialPlayerVelocity Velocidad potencial a la que se quiere mover
	 *                                el usuario
	 * @param potentialPlayerLocation Dirección potencial a la que se quiere mover
	 *                                el usuario
	 */
	private void handleDifferentDirectionMovement(Direction direction, Point2D potentialPlayerVelocity,
			Point2D potentialPlayerLocation) {
		if (gridContainsWall(potentialPlayerLocation)) {
			handleWallCollisionInDifferentDirection(direction);
		} else {
			movePlayerToLocation(potentialPlayerVelocity, potentialPlayerLocation);
			setLastDirection(direction);
		}
	}

	/**
	 * 
	 * Controla la colisión con un muro cuando va en una dirección diferente a la de
	 * antes
	 * 
	 * @param direction La dirección ingresada
	 */
	private void handleWallCollisionInDifferentDirection(Direction direction) {
		Point2D potentialPlayerVelocity = changeVelocity(lastDirection);
		Point2D potentialPlayerLocation = playerLocation.add(potentialPlayerVelocity);

		if (gridContainsWall(potentialPlayerLocation)) {
			stopPlayerMovement();
		} else {
			movePlayerToLocation(potentialPlayerVelocity, potentialPlayerLocation);
		}
	}

	/**
	 * 
	 * Determina si la ubicación es un muro
	 * 
	 * @param location Ubicación a validar
	 * @return
	 */
	private boolean gridContainsWall(Point2D location) {
		return this.grid[(int) location.getX()][(int) location.getY()] == CellValue.WALL;
	}

	/**
	 * Detiene el movimiento del jugador
	 */
	private void stopPlayerMovement() {
		this.playerVelocity = changeVelocity(Direction.NONE);
		setLastDirection(Direction.NONE);
	}

	/**
	 * Mueve al jugador a la velocidad y ubicación indicada
	 * 
	 * 
	 * @param velocity Punto de dos dimensiones que representa la velocidad
	 * @param location Punto de dos dimensiones que representa la ubicación
	 */
	private void movePlayerToLocation(Point2D velocity, Point2D location) {
		this.playerVelocity = velocity;
		this.playerLocation = location;
	}

	/**
	 * Mueve los enemigos para seguir al personaje
	 * 
	 * @see moveEnemy
	 */
	public void moveEnemies() {
		Point2D[] enemy1motionVector = moveEnemy(this.enemy1Velocity, this.enemy1Location);
		Point2D[] enemy2motionVector = moveEnemy(this.enemy2Velocity, this.enemy2Location);
		this.enemy1Velocity = enemy1motionVector[0];
		this.enemy1Location = enemy1motionVector[1];
		this.enemy2Velocity = enemy2motionVector[0];
		this.enemy2Location = enemy2motionVector[1];
	}

	/**
	 * Establece el movimiento del enemigo para buscar al personaje o sino moverse
	 * aleatoriamente hasta colisionar con una pared.
	 * 
	 * @param velocity la velocidad actual del enemigo
	 * @param location la ubicación actual del enemigo
	 * @return Un vector que contiene una nueva velocidad y ubicación para el
	 *         enemigo
	 */
	public Point2D[] moveEnemy(Point2D velocity, Point2D location) {
		if (isSameColumnAsPlayer(location)) {
			velocity = moveTowardsPlayerInColumn(location);
		} else if (isSameRowAsPlayer(location)) {
			velocity = moveTowardsPlayerInRow(location);
		}

		return moveRandomlyUntilWallCollision(velocity, location);
	}

	/**
	 * 
	 * Ejecuta un movimiento a nivel del eje Y
	 * 
	 * @param location Ubicación vectorial
	 * @return
	 */
	private Point2D moveTowardsPlayerInColumn(Point2D location) {
		if (location.getX() > this.playerLocation.getX()) {
			return changeVelocity(Direction.UP);
		} else {
			return changeVelocity(Direction.DOWN);
		}
	}

	/**
	 * 
	 * Valida si la ubicación es la misma que la del usuario en el eje Y
	 * 
	 * @param location Ubicación vectorial
	 * @return
	 */
	private boolean isSameColumnAsPlayer(Point2D location) {
		return location.getY() == this.playerLocation.getY();
	}

	/**
	 * 
	 * Valida si la ubicación es la misma que la del usuario en el eje X
	 * 
	 * @param location Ubicación vectorial
	 * @return
	 */

	private boolean isSameRowAsPlayer(Point2D location) {
		return location.getX() == this.playerLocation.getX();
	}

	/**
	 * 
	 * Valida si la ubicación es la misma que la del usuario en el eje X
	 * 
	 * @param location Ubicación vectorial
	 * @return
	 */
	private Point2D moveTowardsPlayerInRow(Point2D location) {
		if (location.getY() > this.playerLocation.getY()) {
			return changeVelocity(Direction.LEFT);
		} else {
			return changeVelocity(Direction.RIGHT);
		}
	}

	/**
	 * 
	 * @param velocity la velocidad actual del enemigo
	 * @param location la ubicación actual del enemigo
	 * @return
	 */
	private Point2D[] moveRandomlyUntilWallCollision(Point2D velocity, Point2D location) {
		Point2D potentialLocation = location.add(velocity);
		while (gridContainsWall(potentialLocation)) {
			Direction randomDirection = getRandomDirection();
			velocity = changeVelocity(randomDirection);
			potentialLocation = location.add(velocity);
		}
		location = potentialLocation;
		return new Point2D[] { velocity, location };
	}

	/**
	 * 
	 * Genera un movimiento de manera aleatoria
	 * 
	 * @return Dirección de movimiento
	 */
	private Direction getRandomDirection() {
		Random generator = new Random();
		int randomNum = generator.nextInt(4);
		return intToDirection(randomNum);
	}

	/**
	 * Convierte un número entero entre 0 y 3 a una dirección
	 * 
	 * @param x Número entero entre 0 y 3
	 * @return La dirección correspondiente
	 */
	public Direction intToDirection(int x) {
		switch (x) {
		case 0:
			return Direction.LEFT;
		case 1:
			return Direction.RIGHT;
		case 2:
			return Direction.UP;
		default:
			return Direction.DOWN;
		}
	}

	/**
	 * Obtiene la ubicación de origen de la entidad solicitada
	 * 
	 * @return Ubicación de origen de la entidad
	 */
	private Point2D getEntityHomeLocation(CellValue entityHome, Point2D entityLocation, Point2D entityVelocity) {
		for (int row = 0; row < rowCount; row++) {
			for (int column = 0; column < columnCount; column++) {
				if (grid[row][column] == entityHome) {
					return new Point2D(row, column);
				}
			}
		}
		return entityLocation;
	}

	/**
	 * Restablece a todas la entidades a la ubicación de origen
	 */
	private void resetAllPositions() {
		enemy1Location = getEntityHomeLocation(CellValue.ENEMY1HOME, enemy1Location, enemy1Velocity);
		enemy2Location = getEntityHomeLocation(CellValue.ENEMY2HOME, enemy2Location, enemy2Velocity);
		playerLocation = getEntityHomeLocation(CellValue.PLAYERHOME, playerLocation, playerVelocity);

		enemy1Velocity = new Point2D(-1, 0);
		enemy2Velocity = new Point2D(-1, 0);
		playerVelocity = new Point2D(-1, 0);
	}

	/**
	 * Actualiza el modelo para reflejar el movimiento del personaje, los enemigos y
	 * el cambio de estado de las manzanas
	 * 
	 * @param direction La dirección ingresada más recientemente para que el
	 *                  personaje se mueva
	 */
	public void step(Direction direction) {
		movePlayer(direction);
		updateFishCount();
		checkEnemyCollision();
		moveEnemies();
		checkEnemyCollision();
		checkLevelCompletion();
	}

	/**
	 * Actualiza la cuenta de los pescados
	 */
	private void updateFishCount() {
		CellValue playerLocationCellValue = grid[(int) playerLocation.getX()][(int) playerLocation.getY()];
		if (playerLocationCellValue == CellValue.FISH) {
			grid[(int) playerLocation.getX()][(int) playerLocation.getY()] = CellValue.EMPTY;
			fishCount--;
			score += 10;
		}
	}

	/**
	 * Valida si el jugador colisionó con el enemigo
	 */
	private void checkEnemyCollision() {
		if (playerLocation.equals(enemy1Location) || playerLocation.equals(enemy2Location)) {
			resetAllPositions();

			hasLost = true;
			playerVelocity = new Point2D(0, 0);
		}
	}

	/**
	 * Valida si el nivel está completa
	 */
	private void checkLevelCompletion() {
		if (isLevelComplete()) {
			playerVelocity = new Point2D(0, 0);
			startNextLevel();
		}
	}

	/**
	 * Conecta cada dirección al vector de velocidad correspondiente (Izquierda =
	 * (-1,0), Derecha = (1,0), Arriba = (0,-1), Abajo = (0,1))
	 * 
	 * @param direction dirección de la entidad
	 * @return Vector de velocidad
	 */
	public Point2D changeVelocity(Direction direction) {
		switch (direction) {
		case LEFT:
			return new Point2D(0, -1);
		case RIGHT:
			return new Point2D(0, 1);
		case UP:
			return new Point2D(-1, 0);
		case DOWN:
			return new Point2D(1, 0);
		default:
			return new Point2D(0, 0);
		}
	}

	/**
	 * Determia si se ganó
	 * 
	 * @return boolean
	 */
	public boolean hasWonGame() {
		return hasWon;
	}

	/**
	 * Determia si se perdió
	 * 
	 * @return boolean
	 */
	public boolean hasLostGame() {
		return hasLost;
	}

	/**
	 * Determia si el nivel está completo cuando hay cero manzanas
	 * 
	 * @return boolean
	 */
	public boolean isLevelComplete() {
		return this.fishCount == 0;
	}

	/**
	 * Obtiene la cuadrícula
	 * 
	 * @return
	 */
	public CellValue[][] getGrid() {
		return grid;
	}

	/**
	 * Obtiene valor de la celda
	 * 
	 * @param row    Fila de la celda
	 * @param column Columna de la celda
	 * @return Valor de la celda (row, column)
	 */
	public CellValue getCellValue(int row, int column) {
		assert row >= 0 && row < this.grid.length && column >= 0 && column < this.grid[0].length;
		return this.grid[row][column];
	}

	/**
	 * 
	 * Obtiene la dirección actual
	 * 
	 * @return
	 */
	public Direction getCurrentDirection() {
		return currentDirection;
	}

	/**
	 * 
	 * Obtiene la última dirección
	 * 
	 * @return
	 */
	public static Direction getLastDirection() {
		return lastDirection;
	}

	/**
	 * 
	 * Obtien el puntaje del juego
	 * 
	 * @return
	 */
	public int getScore() {
		return score;
	}

	/**
	 * 
	 * Obtien el nivel del juego
	 * 
	 * @return
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * 
	 * Obtiene el número de manzanas restantes
	 * 
	 * @return
	 */
	public int getFishCount() {
		return fishCount;
	}

	/**
	 * 
	 * Obtiene número de filas del escenario
	 * 
	 * @return Número de filas del escenario
	 */
	public int getRowCount() {
		return this.rowCount;
	}

	/**
	 * 
	 * Obtiene número de columnas del escenario
	 * 
	 * @return Número de columnas del escenario
	 */
	public int getColumnCount() {
		return this.columnCount;
	}

	/**
	 * 
	 * @return
	 */
	public Point2D getPlayerLocation() {
		return playerLocation;
	}

	/**
	 * 
	 * @return
	 */
	public Point2D getEnemy1Location() {
		return enemy1Location;
	}

	/**
	 * 
	 * @return
	 */
	public Point2D getEnemy2Location() {
		return enemy2Location;
	}

	/**
	 * 
	 * @return
	 */
	public Point2D getPlayerVelocity() {
		return playerVelocity;
	}

	/**
	 * 
	 * @return
	 */
	public Point2D getEnemy1Velocity() {
		return enemy1Velocity;
	}

	/**
	 * 
	 * @return
	 */
	public Point2D getEnemy2Velocity() {
		return enemy2Velocity;
	}

	/**
	 * 
	 * @param direction
	 */
	public void setCurrentDirection(Direction direction) {
		currentDirection = direction;
	}

	/**
	 * 
	 * @param direction
	 */
	public void setLastDirection(Direction direction) {
		lastDirection = direction;
	}

	/**
	 * 
	 * @param score
	 */
	public void setScore(int score) {
		this.score = score;
	}

	/**
	 * Añade a la puntuación
	 *
	 * @param points Puntos para ganar
	 */
	public void addToScore(int points) {
		this.score += points;
	}

	/**
	 * 
	 * @param level
	 */
	public void setLevel(int level) {
		this.level = level;
	}

	/**
	 * Establece el número de pescados restantes
	 */
	public void setFishCount(int fishCount) {
		this.fishCount = fishCount;
	}

	/**
	 * Establece número de filas del escenario
	 * 
	 * @param rowCount Número de filas del escenario
	 */
	public void setRowCount(int rowCount) {
		this.rowCount = rowCount;
	}

	/**
	 * Obtiene número de columnas del escenario
	 * 
	 * @param columnCount Número de columnas del escenario
	 */
	public void setColumnCount(int columnCount) {
		this.columnCount = columnCount;
	}

	/**
	 * 
	 * @param playerLocation
	 */
	public void setPlayerLocation(Point2D playerLocation) {
		this.playerLocation = playerLocation;
	}

	/**
	 * 
	 * @param enemy1Location
	 */
	public void setEnemy1Location(Point2D enemy1Location) {
		this.enemy1Location = enemy1Location;
	}

	/**
	 * 
	 * @param enemy2Location
	 */
	public void setEnemy2Location(Point2D enemy2Location) {
		this.enemy2Location = enemy2Location;
	}

	/**
	 * 
	 * @param velocity
	 */
	public void setPlayerVelocity(Point2D velocity) {
		this.playerVelocity = velocity;
	}

	/**
	 * 
	 * @param enemy1Velocity
	 */
	public void setEnemy1Velocity(Point2D enemy1Velocity) {
		this.enemy1Velocity = enemy1Velocity;
	}

	/**
	 * 
	 * @param enemy2Velocity
	 */
	public void setEnemy2Velocity(Point2D enemy2Velocity) {
		this.enemy2Velocity = enemy2Velocity;
	}
}
