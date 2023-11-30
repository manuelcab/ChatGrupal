package main.java.main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class ChatClientGUI extends Application {
    private ChatClient cliente;
    private TextField usernameField;
    private PasswordField passwordField;
    private Button actionButton1;
    private Button actionButton2;
    private ListView<TextFlow> chatHistory;
    private Map<String, Color> userColors;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        userColors = new HashMap<>();
        cliente = new ChatClient("localhost", 8080);
        VentanaLogin ventanaLogin = new VentanaLogin(primaryStage);
        ventanaLogin.mostrar();
    }

    private void enviarMensaje(String mensaje) {
        cliente.enviarMensaje(mensaje);
        actualizarHistorialChat("Yo: " + mensaje);
    }

    private void actualizarHistorialChat(String message) {
        Platform.runLater(() -> {
            String[] parts = message.split(":", 2);
            if (parts.length > 1) {
                String user = parts[0].trim();
                String content = parts[1].trim();
                Color userColor = userColors.computeIfAbsent(user, k -> obtenerColor());

                TextFlow textFlow = new TextFlow();
                Text userText = new Text(user + ": ");
                userText.setFill(userColor);
                Text contentText = new Text(content);
                textFlow.getChildren().addAll(userText, contentText);

                chatHistory.getItems().add(textFlow);
            }
        });
    }

    private Color obtenerColor() {
        return Color.rgb(
                (int) (Math.random() * 255),
                (int) (Math.random() * 255),
                (int) (Math.random() * 255)
        );
    }

    private void desconectar() {
        cliente.cerrarConexion();
        Platform.exit();
        System.exit(0);
    }

    class VentanaLogin {
        private Stage primaryStage;

        public VentanaLogin(Stage primaryStage) {
            this.primaryStage = primaryStage;
        }

        public void mostrar() {
            primaryStage.setTitle("Iniciar Sesión o Registrarse");

            BorderPane loginPane = new BorderPane();
            loginPane.setStyle("-fx-background-color: #f0f0f0;"); // Color de fondo claro
            loginPane.setPadding(new Insets(20));

            GridPane grid = new GridPane();
            grid.setAlignment(Pos.CENTER);
            grid.setHgap(10);
            grid.setVgap(10);

            usernameField = new TextField();
            passwordField = new PasswordField();

            actionButton1 = new Button("Iniciar Sesión");
            actionButton2 = new Button("Registrarse");

            // Estilo de los botones
            actionButton1.setStyle("-fx-base: #66cc66;"); // Verde claro
            actionButton2.setStyle("-fx-base: #6699cc;"); // Azul claro

            actionButton1.setOnAction(event -> procesarAccion("login"));
            actionButton2.setOnAction(event -> procesarAccion("registro"));

            grid.add(new Label("Usuario:"), 0, 0);
            grid.add(usernameField, 1, 0);
            grid.add(new Label("Contraseña:"), 0, 1);
            grid.add(passwordField, 1, 1);

            HBox buttonBox = new HBox(actionButton1, actionButton2);
            buttonBox.setAlignment(Pos.CENTER); // Centrar los botones
            buttonBox.setSpacing(10); // Espacio entre los botones

            grid.add(buttonBox, 0, 2, 2, 1);

            loginPane.setCenter(grid);

            Scene loginScene = new Scene(loginPane, 300, 200);
            primaryStage.setScene(loginScene);
            primaryStage.show();
        }

        private void procesarAccion(String accion) {
            String username = usernameField.getText();
            String password = passwordField.getText();

            Task<String> task = new Task<String>() {
                @Override
                protected String call() throws Exception {
                    if ("login".equals(accion)) {
                        return cliente.loginUsuario(username, password);
                    } else if ("registro".equals(accion)) {
                        return cliente.registerUsuario(username, password);
                    }
                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                String respuesta = task.getValue();
                if ("SESION_INICIADA".equals(respuesta) || "REGISTRO_EXITOSO".equals(respuesta)) {
                    Platform.runLater(() -> {
                        primaryStage.close();
                        VentanaChat ventanaChat = new VentanaChat();
                        ventanaChat.mostrar();
                    });
                } else {
                    System.out.println("FALLO_" + accion.toUpperCase());
                }
            });

            new Thread(task).start();
        }
    }

    class VentanaChat {
        
        private Reproductor reproductor;
        
        public VentanaChat() {
        this.reproductor = new Reproductor();
    }
        
        public void mostrar() {
            Stage primaryStage = new Stage();
            primaryStage.setTitle("Chat Grupal");

            chatHistory = new ListView<>();
            chatHistory.setPrefHeight(150);
            chatHistory.setEditable(false);
            chatHistory.setCellFactory(param -> new ListCell<>() {
                {
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }

                @Override
                protected void updateItem(TextFlow item, boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty) {
                        setGraphic(item);
                        reproductor.cargarSonido("src/main/resources/sounds/alert.wav");
                        reproductor.reproducir();
                    } else {
                        setGraphic(null);
                    }
                }
            });


            ScrollPane scrollPane = new ScrollPane(chatHistory);

            BorderPane root = new BorderPane();
            root.setPadding(new Insets(10));
            root.setCenter(scrollPane);

            chatHistory.setPrefHeight(250);
            chatHistory.setPrefWidth(350);
            chatHistory.setEditable(false);

            TextField inputField = new TextField();
            inputField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    enviarMensaje(inputField.getText());
                    inputField.setText("");
                }
            });

            Button sendButton = new Button("Enviar");
            sendButton.setOnAction(event -> enviarMensaje(inputField.getText()));

            Button disconnectButton = new Button("Desconectar");
            disconnectButton.setStyle("-fx-base: #FF3333;");
            disconnectButton.setOnAction(event -> desconectar());

            HBox inputBox = new HBox(inputField, sendButton, disconnectButton);
            inputBox.setSpacing(10);

            VBox chatVBox = new VBox(scrollPane, inputBox);
            chatVBox.setSpacing(10);

            root.setBottom(chatVBox);

            primaryStage.setScene(new Scene(root, 350, 300));
            primaryStage.show();
        }
    }
    
    class Reproductor {
        
        private Clip clip;
        
        public void cargarSonido(String ruta){

          
            try {
                File archivoSonido = new File(ruta);
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(archivoSonido);
                
                clip = AudioSystem.getClip();
                clip.open(audioInputStream);
            } catch (LineUnavailableException ex) {
                Logger.getLogger(ChatClientGUI.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(ChatClientGUI.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnsupportedAudioFileException ex) {
                Logger.getLogger(ChatClientGUI.class.getName()).log(Level.SEVERE, null, ex);
            }
               
        
        }

    public void reproducir() {
         
        if(clip != null){
        
            clip.setFramePosition(0);
            clip.start();
        
        }
        
        }

    }
}
