package com.newsvision.user.dto.request;

import com.newsvision.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JoinUserRequest {
    private String username;
    private String password;
    private String email;
}
