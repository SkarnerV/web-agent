package com.agentplatform.file.converter;

import com.agentplatform.common.mybatis.entity.FileEntity;
import com.agentplatform.file.dto.FileVO;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class DefaultFileConverter implements FileConverter {

    @Override
    public FileVO toVO(FileEntity entity) {
        if (entity == null) {
            return null;
        }
        FileVO vo = new FileVO();
        vo.setId(entity.getId());
        vo.setFilename(entity.getFilename());
        vo.setFileSize(entity.getFileSize());
        vo.setMimeType(entity.getMimeType());
        vo.setScanStatus(entity.getScanStatus());
        vo.setStatus(entity.getStatus());
        vo.setExpiresAt(entity.getExpiresAt());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
