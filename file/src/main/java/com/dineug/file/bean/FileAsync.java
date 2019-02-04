package com.dineug.file.bean;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * async file
 * 비동기 파일처리
 *
 * @author dineug2@gmail.com
 * @version 2019.01.31 v1
 */
@Component
public class FileAsync extends FileBase {

    private FileAzure fileAzure;
    protected void init(FileAzure fileAzure) {
        this.fileAzure = fileAzure;
    }

    /**
     * 비동기 파일 업로드
     *
     * @param file
     */
    @Async
    public void uploadProcessAzure(FileVO file, boolean isImageResize) {
        fileAzure.uploadProcess(file, isImageResize);
    }

}
