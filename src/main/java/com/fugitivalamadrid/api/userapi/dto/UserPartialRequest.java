package com.fugitivalamadrid.api.userapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPartialRequest {
    private String username;
    private String email;
}
