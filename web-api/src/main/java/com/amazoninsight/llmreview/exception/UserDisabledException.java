package com.amazoninsight.llmreview.exception;

import lombok.Getter;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;

@Getter
public class UserDisabledException extends DisabledException {

    private final Authentication authentication;

    public UserDisabledException(String msg, Authentication authentication) {
        super(msg);
        this.authentication = authentication;
    }

}