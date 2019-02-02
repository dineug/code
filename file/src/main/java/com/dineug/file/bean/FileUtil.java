package com.dineug.file.bean;

import java.util.List;
import java.util.Map;

/**
 * file util
 * 파일 데이터 형변환 처리 및 util
 *
 * @author dineug2@gmail.com
 * @version 2019.01.31 v1
 */
public class FileUtil {

    private Map<String, Object> fileData;

    public FileUtil(Map<String, Object> fileData) {
        this.fileData = fileData;
    }

    /**
     * 업로드 파일 이름
     *
     * @return
     */
    public Map<String, List<String>> getFileNames() {
        return (Map<String, List<String>>) fileData.get("fileNames");
    }

    /**
     * 업로드 파일 사이즈
     *
     * @return
     */
    public List<Long> getFileSizes() {
        return (List<Long>) fileData.get("fileSizes");
    }

    /**
     * 다운로드 fileByte
     *
     * @return
     */
    public byte[] getFileByte() {
        return (byte[]) fileData.get("fileByte");
    }

    /**
     * 업로드 외부서버 path
     *
     * @return
     */
    public String getFileHttpPath() {
        return (String) fileData.get("fileHttpPath");
    }

}
