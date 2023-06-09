package org.quizlab.quizlab_game.game;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import java.util.Timer;
import java.util.TimerTask;

import org.quizlab.quizlab_game.levels.Level;
import org.quizlab.quizlab_game.question.Question;
import org.quizlab.quizlab_game.question.RandomQuestionSelector;

/**
 * 
 * Controlador del juego
 *
 */
public class Controller implements EventHandler<KeyEvent> {
	final private static double FRAMES_PER_SECOND = 5.0;

	private String gameOverStyle = "-fx-font-size: 250%; -fx-font-family: 'Iceland'; -fx-text-fill: ";
	@FXML
	private Label scoreLabel;
	@FXML
	private Label levelLabel;
	@FXML
	private Label gameOverLabel;
	@FXML
	private Label newGameLabel;
	@FXML
	private View view;
	private Model model;

	private Timer timer;
	private boolean isPaused;
	private boolean questionResult;

	/**
	 * Inicializa el controlador del juego
	 */
	public Controller() {
		this.isPaused = false;
	}

	/**
	 * Inicializa el modelo y el temporizador para empezar el juego
	 */
	public void initialize() {
		this.model = new Model();
		this.update(Model.Direction.NONE);
		this.startTimer();
	}

	/**
	 * Programa el modelo para que se actualice según el temporizador.
	 */
	private void startTimer() {
		this.timer = new java.util.Timer();
		long frameTimeInMilliseconds = (long) (1000.0 / FRAMES_PER_SECOND);

		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				Platform.runLater(() -> update(model.getCurrentDirection()));
			}
		};

		this.timer.schedule(timerTask, 0, frameTimeInMilliseconds);
	}

	/**
	 * Controla la ejecución del juego y actualiza la vista
	 * 
	 * @param direction La dirección ingresada para que se mueva el personaje
	 */
	private void update(Model.Direction direction) {
		this.model.step(direction);
		this.view.update(model);
		this.scoreLabel.setText(String.format("Puntaje: %d", this.model.getScore()));
		this.levelLabel.setText(String.format("Nivel: %d", this.model.getLevel()));
		checkGameOver();
	}

	/**
	 * Toma la entrada del teclado del usuario para controlar el juego
	 * 
	 * @param keyEvent Entrada del usuario por teclado
	 */
	@Override
	public void handle(KeyEvent keyEvent) {
		boolean keyRecognized = true;
		KeyCode code = keyEvent.getCode();
		Model.Direction direction = Model.Direction.NONE;
		if (code == KeyCode.LEFT) {
			direction = Model.Direction.LEFT;
		} else if (code == KeyCode.RIGHT) {
			direction = Model.Direction.RIGHT;
		} else if (code == KeyCode.UP) {
			direction = Model.Direction.UP;
		} else if (code == KeyCode.DOWN) {
			direction = Model.Direction.DOWN;
		} else if (code == KeyCode.N) {
			startNewGame();
		} else {
			keyRecognized = false;
		}
		if (keyRecognized) {
			keyEvent.consume();
			this.model.setCurrentDirection(direction);
		}
	}

	/**
	 * Inicia un nuevo juego
	 */
	private void startNewGame() {
		pause();
		this.model.startNewGame();
		setGameOverLabel("", "");
		this.isPaused = false;
		this.startTimer();
	}

	/**
	 * Valida si se terminó el juego para mostrar el mensaje apropiado
	 */
	private void checkGameOver() {
		if (model.hasLostGame()) {
			pause();

			Stage questionStage = new Stage();
			showQuestionDialog(questionStage);
			boolean isCorrectQuestion = getQuestionResult();

			if (isCorrectQuestion) {
				this.startTimer();
				this.model.continueGame();
			} else {
				setGameOverLabel("Perdiste", "red");
			}
		}
		if (model.hasWonGame()) {
			setGameOverLabel("¡Ganaste!", "green");
			pause();
		}
	}

	private void showQuestionDialog(Stage questionStage) {
		Stage dialogStage = new Stage();
		dialogStage.initModality(Modality.APPLICATION_MODAL);
		dialogStage.initOwner(questionStage);
		dialogStage.initStyle(StageStyle.UTILITY);

		RandomQuestionSelector randomQuestionSelector = new RandomQuestionSelector();
		Question question = randomQuestionSelector.getRandomQuestion();

		Label statementLabel = new Label(question.getStatement());
		Button option1Button = new Button(question.getOptions()[0]);
		Button option2Button = new Button(question.getOptions()[1]);
		Button option3Button = new Button(question.getOptions()[2]);

		option1Button.setOnAction(e -> {
			setQuestionResult(question.esRespuestaCorrecta(option1Button.getText()));
			dialogStage.close();
		});

		option2Button.setOnAction(e -> {
			setQuestionResult(question.esRespuestaCorrecta(option2Button.getText()));
			dialogStage.close();
		});

		option3Button.setOnAction(e -> {
			setQuestionResult(question.esRespuestaCorrecta(option3Button.getText()));
			dialogStage.close();
		});

		VBox vbox = new VBox(20);
		vbox.setPadding(new Insets(60));
		vbox.setAlignment(Pos.CENTER);
		vbox.getChildren().addAll(statementLabel, option1Button, option2Button, option3Button);

		Scene scene = new Scene(vbox);
		scene.getStylesheets().add(getClass().getResource("/org/quizlab/quizlab_game/styles.css").toExternalForm());

		dialogStage.setScene(scene);
		dialogStage.showAndWait();
	}

	public Boolean getQuestionResult() {
		return questionResult;
	}

	public void setQuestionResult(Boolean questionResult) {
		this.questionResult = questionResult;
	}

	/**
	 * Pausar el temporizador
	 */
	public void pause() {
		this.timer.cancel();
		this.isPaused = true;
	}

	/**
	 * Obtiene el ancho del escenario
	 * 
	 * @return Ancho del escenario
	 */
	public double getBoardWidth() {
		return View.CELL_WIDTH * this.view.getColumnCount();
	}

	/**
	 * Obtiene la altura del escenario
	 * 
	 * @return Altura del escenario
	 */
	public double getBoardHeight() {
		return View.CELL_WIDTH * this.view.getRowCount();
	}

	/**
	 * Obtiene el archivo del nivel que se va a jugar
	 * 
	 * @param x Número del arhivo que se quiere acceder
	 * @return Archivo del nivel
	 */
	public static char[][] getLevelData(int x) {
		return Level.LEVELS[x];
	}

	/**
	 * Obtiene estado para saber si el juego está pausado o no
	 * 
	 * @return Estado de pausa
	 */
	public boolean getPaused() {
		return isPaused;
	}

	/**
	 * Establece el texto y estilo para mostrar en al finalizar el juego
	 * 
	 * @param text  Texto del label
	 * @param color Color del label
	 */
	public void setGameOverLabel(String text, String color) {
		this.gameOverLabel.setText(String.format(text));
		if (color != "") {
			this.gameOverLabel.setStyle(gameOverStyle + color + ";");
		}
	}
}
