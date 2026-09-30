package com.ctrip.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ctrip.content.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论数据访问接口。
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
