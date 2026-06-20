package com.pinfan.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class UpdateMeDTO {
    @Size(max = 15, message = "昵称最多15字")
    private String nickname;

    @Size(max = 255, message = "头像URL最多255字")
    private String avatar;
}
