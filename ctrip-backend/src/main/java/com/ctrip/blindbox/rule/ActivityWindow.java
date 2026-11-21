package com.ctrip.blindbox.rule;

import java.time.LocalDateTime;

/**
 * 限定盲盒活动时间窗。
 */
public record ActivityWindow(LocalDateTime start, LocalDateTime end) {}
