package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.LearningPath;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学习路径Mapper接口
 */
@Mapper
public interface LearningPathMapper extends BaseMapper<LearningPath> {

    /**
     * 根据用户ID获取学习路径列表
     */
    @Select("SELECT * FROM learning_path WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<LearningPath> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和课程ID获取学习路径
     */
    @Select("SELECT * FROM learning_path WHERE user_id = #{userId} AND course_id = #{courseId} AND status = 'active' LIMIT 1")
    LearningPath selectActiveByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    /**
     * 根据会话ID获取学习路径
     */
    @Select("SELECT * FROM learning_path WHERE session_id = #{sessionId} ORDER BY created_at DESC LIMIT 1")
    LearningPath selectBySessionId(@Param("sessionId") Long sessionId);
}
