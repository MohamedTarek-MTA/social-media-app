package com.socialmediaapp.Controller;

import com.socialmediaapp.DAO.NotificationDAO;
import com.socialmediaapp.Enum.Privacy;
import com.socialmediaapp.Enum.Status;
import com.socialmediaapp.Enum.Type;
import com.socialmediaapp.Model.*;
import com.socialmediaapp.Service.*;
import com.socialmediaapp.Util.Page;
import com.socialmediaapp.Util.ServiceRegistry;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AppControllers {

    private AuthService authService;
    private UserService userService;
    private PostService postService;
    private FriendService friendService;
    private LikeService likeService;
    private CommentService commentService;
    private final NotificationDAO notificationDAO = new NotificationDAO();

    @FXML private TextField registerNameField;
    @FXML private TextField registerEmailField;
    @FXML private PasswordField registerPasswordField;
    @FXML private TextField loginEmailField;
    @FXML private PasswordField loginPasswordField;

    @FXML private TextField profileNameField;
    @FXML private TextField profileBioField;

    @FXML private TextArea postContentField;
    @FXML private ComboBox<Privacy> privacyCombo;

    @FXML private TextField likePostIdField;
    @FXML private TextField commentPostIdField;
    @FXML private TextField commentContentField;

    @FXML private TextField friendUserIdField;
    @FXML private TextField friendTargetIdField;

    @FXML private TextField searchTextField;

    @FXML private Label currentUserLabel;
    @FXML private Label statusLabel;

    @FXML private ListView<String> feedList;
    @FXML private ListView<String> friendsList;
    @FXML private ListView<String> notificationsList;
    @FXML private ListView<String> usersList;

    @FXML
    public void initialize() {
        ServiceRegistry registry = ServiceRegistry.getInstance();
        authService = registry.getAuthService();
        userService = registry.getUserService();
        postService = registry.getPostService();
        friendService = registry.getFriendService();
        likeService = registry.getLikeService();
        commentService = registry.getCommentService();

        privacyCombo.setItems(FXCollections.observableArrayList(Privacy.values()));
        privacyCombo.setValue(Privacy.PUBLIC);
        refreshAll();
    }

    @FXML
    private void onRegister() {
        try {
            User user = User.builder()
                    .name(registerNameField.getText().trim())
                    .email(registerEmailField.getText().trim())
                    .password(registerPasswordField.getText())
                    .bio("New user")
                    .createdAt(LocalDateTime.now())
                    .build();
            authService.register(user, Optional.empty());
            setStatus("Registration successful. You can login now.");
        } catch (Exception e) {
            setStatus("Register failed: " + e.getMessage());
        }
    }

    @FXML
    private void onLogin() {
        try {
            authService.login(loginEmailField.getText().trim(), loginPasswordField.getText());
            setStatus("Login successful.");
            refreshAll();
        } catch (Exception e) {
            setStatus("Login failed: " + e.getMessage());
        }
    }

    @FXML
    private void onLogout() {
        authService.logout();
        refreshAll();
        setStatus("Logged out.");
    }

    @FXML
    private void onUpdateProfile() {
        try {
            User current = requireCurrentUser();
            current.setName(profileNameField.getText().trim());
            current.setBio(profileBioField.getText().trim());
            userService.updateUser(current, Optional.empty());
            setStatus("Profile updated.");
            refreshAll();
        } catch (Exception e) {
            setStatus("Profile update failed: " + e.getMessage());
        }
    }

    @FXML
    private void onCreatePost() {
        try {
            requireCurrentUser();
            Post post = Post.builder()
                    .content(postContentField.getText().trim())
                    .privacy(privacyCombo.getValue())
                    .createdAt(LocalDateTime.now())
                    .build();
            postService.createPost(post, Optional.empty());
            setStatus("Post created.");
            postContentField.clear();
            refreshFeed();
        } catch (Exception e) {
            setStatus("Create post failed: " + e.getMessage());
        }
    }

    @FXML
    private void onLikePost() {
        try {
            User current = requireCurrentUser();
            int postId = Integer.parseInt(likePostIdField.getText().trim());
            likeService.createLike(Like.builder().userId(current.getId()).postId(postId).createdAt(LocalDateTime.now()).build());
            Post target = postService.getPostById(postId);
            createNotification(target.getUserId(), current.getId(), Type.LIKE, postId);
            setStatus("Post liked.");
            refreshNotifications();
        } catch (Exception e) {
            setStatus("Like failed: " + e.getMessage());
        }
    }

    @FXML
    private void onCommentPost() {
        try {
            User current = requireCurrentUser();
            int postId = Integer.parseInt(commentPostIdField.getText().trim());
            Comment comment = Comment.builder()
                    .userId(current.getId())
                    .postId(postId)
                    .content(commentContentField.getText().trim())
                    .createdAt(LocalDateTime.now())
                    .build();
            commentService.createComment(comment);
            Post target = postService.getPostById(postId);
            createNotification(target.getUserId(), current.getId(), Type.COMMENT, postId);
            setStatus("Comment added.");
            commentContentField.clear();
            refreshNotifications();
        } catch (Exception e) {
            setStatus("Comment failed: " + e.getMessage());
        }
    }

    @FXML
    private void onSendFriendRequest() {
        try {
            requireCurrentUser();
            int userId = Integer.parseInt(friendUserIdField.getText().trim());
            int friendId = Integer.parseInt(friendTargetIdField.getText().trim());
            Friend friend = Friend.builder().userId(userId).friendId(friendId).status(Status.PENDING).createdAt(LocalDateTime.now()).build();
            friendService.createFriend(friend);
            createNotification(friendId, userId, Type.FRIEND_REQUEST, friend.getId());
            setStatus("Friend request sent.");
            refreshFriends();
            refreshNotifications();
        } catch (Exception e) {
            setStatus("Friend request failed: " + e.getMessage());
        }
    }

    @FXML
    private void onRefreshData() {
        refreshAll();
        setStatus("Data refreshed.");
    }

    @FXML
    private void onSearch() {
        try {
            String term = searchTextField.getText().trim();
            Page<User> userPage = userService.getAllUsersAsPage(0, 15, "name", "ASC", term);
            List<String> filteredPosts = postService.getAllPosts().stream()
                    .filter(p -> p.getContent() != null && p.getContent().toLowerCase().contains(term.toLowerCase()))
                    .map(p -> "POST#" + p.getId() + " (User " + p.getUserId() + "): " + p.getContent())
                    .toList();

            List<String> userRows = userPage.getContent().stream()
                    .map(u -> "USER#" + u.getId() + " | " + u.getName() + " | " + u.getEmail())
                    .collect(Collectors.toList());
            userRows.addAll(filteredPosts);
            usersList.setItems(FXCollections.observableArrayList(userRows));
            setStatus("Search finished.");
        } catch (Exception e) {
            setStatus("Search failed: " + e.getMessage());
        }
    }

    private void refreshAll() {
        refreshCurrentUserInfo();
        refreshFeed();
        refreshFriends();
        refreshNotifications();
        refreshUsers();
    }

    private void refreshCurrentUserInfo() {
        User current = authService.getCurrentUser();
        if (current == null) {
            currentUserLabel.setText("Current user: Guest");
            profileNameField.clear();
            profileBioField.clear();
            return;
        }
        currentUserLabel.setText("Current user: " + current.getName() + " (#" + current.getId() + ")");
        profileNameField.setText(current.getName());
        profileBioField.setText(current.getBio() == null ? "" : current.getBio());
    }

    private void refreshFeed() {
        List<String> rows = postService.getAllPosts().stream()
                .map(post -> "#" + post.getId() + " | User " + post.getUserId() + " | " + post.getPrivacy() + " | " + post.getContent())
                .collect(Collectors.toList());
        feedList.setItems(FXCollections.observableArrayList(rows));
    }

    private void refreshFriends() {
        List<String> rows = friendService.getAllFriends().stream()
                .map(f -> "Friendship#" + f.getId() + ": " + f.getUserId() + " <-> " + f.getFriendId() + " (" + f.getStatus() + ")")
                .collect(Collectors.toList());
        friendsList.setItems(FXCollections.observableArrayList(rows));
    }

    private void refreshNotifications() {
        User current = authService.getCurrentUser();
        if (current == null) {
            notificationsList.setItems(FXCollections.observableArrayList());
            return;
        }
        List<String> rows = notificationDAO.findAllByUserId(current.getId()).stream()
                .map(n -> "Notification#" + n.getId() + " | " + n.getType() + " from " + n.getSenderId() + (n.isRead() ? " (READ)" : " (NEW)"))
                .collect(Collectors.toList());
        notificationsList.setItems(FXCollections.observableArrayList(rows));
    }

    private void refreshUsers() {
        List<String> rows = userService.getAllUsers().stream()
                .map(u -> "#" + u.getId() + " | " + u.getName() + " | " + u.getEmail())
                .collect(Collectors.toList());
        usersList.setItems(FXCollections.observableArrayList(rows));
    }

    private User requireCurrentUser() {
        User current = authService.getCurrentUser();
        if (current == null) {
            throw new IllegalStateException("You need to login first.");
        }
        return current;
    }

    private void createNotification(int targetUserId, int senderId, Type type, int referenceId) {
        if (targetUserId == senderId) {
            return;
        }
        Notification notification = Notification.builder()
                .userId(targetUserId)
                .senderId(senderId)
                .type(type)
                .referenceId(referenceId)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationDAO.save(notification);
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}
