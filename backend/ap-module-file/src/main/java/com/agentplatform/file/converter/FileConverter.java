package com.agentplatform.file.converter;

import com.agentplatform.file.dto.FileVO;
import com.agentplatform.common.mybatis.entity.FileEntity;
import org.mapstruct.Mapper;

@Mapper
public interface FileConverter {
    FileVO toVO(FileEntity entity);
}
