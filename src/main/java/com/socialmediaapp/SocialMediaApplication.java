package com.socialmediaapp;

import com.socialmediaapp.DAO.*;
import com.socialmediaapp.Service.*;
import com.socialmediaapp.Util.DDL;
import com.socialmediaapp.Util.ServiceRegistry;
import com.socialmediaapp.Util.UserSession;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SocialMediaApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        initializeServices();

        FXMLLoader fxmlLoader = new FXMLLoader(SocialMediaApplication.class.getResource("/com/socialmediaapp/View/AppViews.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1150, 760);
        scene.getStylesheets().add(SocialMediaApplication.class.getResource("/com/socialmediaapp/Style/AppStyle.css").toExternalForm());

        stage.setTitle("Social Media Application");
        stage.setScene(scene);
        stage.show();
    }

    private void initializeServices() {
        DDL.createDatabase();
        DDL.createTables();

        AuthService authService = new AuthService(UserSession.getInstance(), new UserDAO());
        UserService userService = new UserService(new UserDAO(), new FriendDAO(), new PostDAO(), new CommentDAO(), new LikeDAO(), authService);
        PostService postService = new PostService(new UserDAO(), new PostDAO(), new CommentDAO(), authService);
        CommentService commentService = new CommentService(new UserDAO(), new CommentDAO(), new PostDAO(), authService);
        FriendService friendService = new FriendService(new FriendDAO(), new UserDAO(), authService);
        LikeService likeService = new LikeService(new UserDAO(), new PostDAO(), new LikeDAO(), authService);

        ServiceRegistry.initialize(authService, userService, postService, commentService, friendService, likeService);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
