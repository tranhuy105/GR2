package com.tranhuy105.server.dto;

import com.tranhuy105.server.entity.Role;
import com.tranhuy105.server.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private Role role;
    private Long driverId;
    private String driverName;
    
    public static UserDTO fromEntity(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .driverId(user.getDriver() != null ? user.getDriver().getId() : null)
                .driverName(user.getDriver() != null ? user.getDriver().getName() : null)
                .build();
    }
}
