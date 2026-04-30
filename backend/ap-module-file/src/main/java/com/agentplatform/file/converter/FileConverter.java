package com.agentplatform.file.converter;

import com.agentplatform.file.dto.FileVO;
import com.agentplatform.file.entity.FileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FileConverter {
    FileVO toVO(FileEntity entity);
}
