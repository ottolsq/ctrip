package com.ctrip.common.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    @DisplayName("枚举参数非法：返回 400")
    void enumParamMismatch_shouldReturn400() throws Exception {
        MockMvc mockMvc = mockMvc(new ParamController());

        mockMvc.perform(get("/test-enum").param("value", "BAD"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"success\":false")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("参数格式错误")));
    }

    @Test
    @DisplayName("请求体 JSON 非法：返回 400")
    void invalidJsonBody_shouldReturn400() throws Exception {
        MockMvc mockMvc = mockMvc(new ParamController());

        mockMvc.perform(post("/test-body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"success\":false")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("请求体格式错误")));
    }

    private MockMvc mockMvc(Object controller) {
        return MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @RestController
    static class DeniedController {
        @GetMapping("/test-denied")
        public void denied() {
            throw new AccessDeniedException("Access Denied");
        }
    }

    @RestController
    static class ParamController {

        enum Status { A, B }

        record StatusHolder(Status value) {
        }

        @GetMapping("/test-enum")
        public void enumParam(@RequestParam Status value) {
        }

        @PostMapping("/test-body")
        public void body(@RequestBody StatusHolder body) {
        }
    }
}
