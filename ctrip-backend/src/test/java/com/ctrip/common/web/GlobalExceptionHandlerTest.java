package com.ctrip.common.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("全局异常处理器单元测试")
class GlobalExceptionHandlerTest {

    @Test
    @DisplayName("方法级权限拒绝：返回 403 而非 500")
    void accessDenied_shouldReturn403() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new DeniedController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/test-denied"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"success\":false")));
    }

    @RestController
    static class DeniedController {
        @GetMapping("/test-denied")
        public void denied() {
            throw new AccessDeniedException("Access Denied");
        }
    }
}
