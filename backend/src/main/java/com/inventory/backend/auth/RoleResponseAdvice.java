package com.inventory.backend.auth;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMappingJacksonResponseBodyAdvice;

/** Hides the fields marked for admins from everyone else. */
@RestControllerAdvice
public class RoleResponseAdvice extends AbstractMappingJacksonResponseBodyAdvice {

    @Override
    protected void beforeBodyWriteInternal(MappingJacksonValue body, MediaType contentType, MethodParameter returnType,
                                           ServerHttpRequest request, ServerHttpResponse response) {
        SessionUser user = SessionUser.from(((ServletServerHttpRequest) request).getServletRequest());
        if (user == null || !user.isAdmin()) {
            body.setSerializationView(Views.Seller.class);
        }
    }
}
