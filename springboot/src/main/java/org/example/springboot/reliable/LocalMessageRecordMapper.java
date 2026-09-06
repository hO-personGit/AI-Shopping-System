package org.example.springboot.reliable;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LocalMessageRecordMapper extends BaseMapper<LocalMessageRecord> {
}
