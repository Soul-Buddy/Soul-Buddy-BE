package com.soulbuddy.be.domain.chat.dto;

import com.soulbuddy.be.global.enums.PersonaType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class SessionCreateRequest {

    @NotNull
    private PersonaType personaType;
}
