package com.socialmediaapp.Util;

import com.socialmediaapp.Service.*;
import lombok.Getter;

@Getter
public class ServiceRegistry {
    private static ServiceRegistry instance;

    private final AuthService authService;
    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;
    private final FriendService friendService;
    private final LikeService likeService;

    private ServiceRegistry(AuthService authService,
                            UserService userService,
                            PostService postService,
                            CommentService commentService,
                            FriendService friendService,
                            LikeService likeService) {
        this.authService = authService;
        this.userService = userService;
        this.postService = postService;
        this.commentService = commentService;
        this.friendService = friendService;
        this.likeService = likeService;
    }

    public static void initialize(AuthService authService,
                                  UserService userService,
                                  PostService postService,
                                  CommentService commentService,
                                  FriendService friendService,
                                  LikeService likeService) {
        instance = new ServiceRegistry(authService, userService, postService, commentService, friendService, likeService);
    }

    public static ServiceRegistry getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ServiceRegistry was not initialized.");
        }
        return instance;
    }
}
