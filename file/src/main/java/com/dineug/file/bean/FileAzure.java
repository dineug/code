package com.dineug.file.bean;

import com.dineug.file.component.ConfigComponent;
import com.dineug.file.util.ENV;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.text.SimpleDateFormat;

/**
 * azure file
 * azure storage blob file 처리
 *
 * @author dineug2@gmail.com
 * @version 2019.01.31 v1
 */
@Component
public class FileAzure extends FileBase {

    private static final Logger logger = LoggerFactory.getLogger(FileAzure.class);

    private CloudBlobContainer container;
    private String containerUri;

    @Autowired
    private ConfigComponent config;
    @Autowired
    private FileAsync async;

    @PostConstruct
    private void init() throws InvalidKeyException, URISyntaxException, StorageException {
        // azure container connection
        logger.debug("azure container connection");
        CloudStorageAccount account = CloudStorageAccount.parse(config.getAzureKey());
        CloudBlobClient serviceClient = account.createCloudBlobClient();
        container = serviceClient.getContainerReference(config.getAzureBlob());
        container.createIfNotExists();
        containerUri = container.getUri().toString();
    }


    /**
     * 파일 업로드
     *
     * @param multipartRequest
     * @param filePath
     * @return
     */
    @Override
    public FileUtil upload(MultipartHttpServletRequest multipartRequest, String filePath, boolean isImageResize) {
        FileVO file = new FileVO();
        file.mpreq = multipartRequest;
        file.filePath = multipartRequest.getSession().getServletContext().getRealPath("/resources/tempfile");
        file.fileServerPath = fileServerPath(filePath);
        file.fileHttpPath = containerUri + "/" + file.fileServerPath;
        file.fileData.put("fileHttpPath", file.fileHttpPath);

        // 내부 업로드 처리
        this.uploadData(file);
        this.uploadProcess(file);
        // 외부 업로드 처리
        uploadProcessAzure(file, isImageResize);
        return new FileUtil(file.fileData);
    }

    /**
     * 비동기 파일 업로드
     * @param multipartRequest
     * @param filePath
     * @return
     */
    public FileUtil uploadAsync(MultipartHttpServletRequest multipartRequest, String filePath, boolean isImageResize) {
        FileVO file = new FileVO();
        file.mpreq = multipartRequest;
        file.filePath = multipartRequest.getSession().getServletContext().getRealPath("/resources/tempfile");
        file.fileServerPath = fileServerPath(filePath);
        file.fileHttpPath = containerUri + "/" + file.fileServerPath;
        file.fileData.put("fileHttpPath", file.fileHttpPath);

        // 내부 업로드 처리
        this.uploadData(file);
        async.uploadProcess(file, isImageResize);
        return new FileUtil(file.fileData);
    }

    // 비동기 파일 업로드 처리
    public void uploadProcess(FileVO file, boolean isImageResize) {
        this.uploadProcess(file);
        uploadProcessAzure(file, isImageResize);
    }

    // 파일 업로드 처리
    private void uploadProcessAzure(FileVO file, boolean isImageResize) {
        CloudBlockBlob blob = null;
        File sourceFile = null;

        try {
            for (String saveName : file.saveNames) {
                blob = container.getBlockBlobReference(file.fileServerPath + "/" + saveName);
                sourceFile = new File(file.filePath + "/" + saveName);
                blob.upload(new FileInputStream(sourceFile), sourceFile.length());
            }
            if (isImageResize) {
                // 이미지 리사이징 업로드 처리 및 내부파일 삭제
                imageResizeProcessAzure(file);
            } else {
                // 내부 원본 파일 삭제
                for (String saveName : file.saveNames) {
                    this.delete(file.filePath, saveName);
                }
            }
        } catch (StorageException | URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 파일 다운로드
     *
     * @param response
     * @param filePath
     * @param saveName
     * @param oldName
     * @return
     */
    @Override
    public FileUtil down(HttpServletResponse response, String filePath, String saveName, String oldName) {
        HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        FileVO file = new FileVO();
        file.res = response;
        file.filePath = req.getSession().getServletContext().getRealPath("/resources/tempfile");
        file.fileServerPath = filePath;
        file.saveName = saveName;
        file.oldName = oldName;

        downProcessAzure(file);
        file.fileData.put("fileByte", file.fileByte);
        return new FileUtil(file.fileData);
    }

    // 파일 다운로드 처리
    private void downProcessAzure(FileVO file) {
        // 경로생성
        this.mkdir(file);
        try {
            // 다운로드
            CloudBlockBlob blob = container.getBlockBlobReference(file.fileServerPath + "/" + file.saveName);
            blob.download(new FileOutputStream(file.filePath + "/" + file.saveName));
            file.fileByte = Files.readAllBytes(new File(file.filePath + "/" + file.saveName).toPath());

            // 다운로드 response 셋팅
            this.downResponse(file);

            // 파일 삭제
            this.delete(file.filePath, file.saveName);
        } catch (URISyntaxException | StorageException | IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 파일 삭제
     *
     * @param filePath
     * @param saveName
     * @return
     */
    @Override
    public boolean delete(String filePath, String saveName) {
        // 잘쓰지 않아서 기억나지 않지만 아마 containerUri 이후 path 인걸로 기억함
        // 테스트해봐야됨 지금은 안쓰기때문에 패스
        filePath = filePath.replaceAll(containerUri, "");
        return deleteProcessAzure(filePath + "/" + saveName);
    }

    // 파일 삭제 처리
    private boolean deleteProcessAzure(String filePath) {
        boolean isDelete = false;
        try {
            CloudBlockBlob blob = container.getBlockBlobReference(filePath);
            isDelete = blob.deleteIfExists();
        } catch (URISyntaxException | StorageException e) {
            e.printStackTrace();
        }
        return isDelete;
    }


    // file server path 생성
    private String fileServerPath(String filePath) {
        StringBuffer sb = new StringBuffer();
        if (config.getEnv() == ENV.prod) {
            sb.append(filePath);
        } else {
            sb.append("test/" + filePath);
        }
        sb.append("/").append(new SimpleDateFormat("yyyy").format(System.currentTimeMillis()))
                .append("/").append(new SimpleDateFormat("MM").format(System.currentTimeMillis()));
        return sb.toString();
    }

    // 이미지 리사이징 업로드 처리
    private void imageResizeProcessAzure(FileVO file) {
        CloudBlockBlob blob = null;
        File sourceFile = null;
        String newSaveName = null;
        String first = null;
        String ext = null;
        String tempName = null;

        try {
            for (String saveName : file.saveNames) {
                first = saveName.substring(0, saveName.lastIndexOf("."));
                ext = saveName.substring(saveName.lastIndexOf("."));

                if (saveName.matches(this.imageRegExp)) {
                    tempName = saveName;

                    // 외부 업로드
                    for (Integer size : this.reSizes) {
                        newSaveName = first + "-" + size + ext;

                        // 리사이징
                        this.imageResizeProcess(size, file.filePath, tempName, newSaveName);
                        blob = container.getBlockBlobReference(file.fileServerPath + "/" + newSaveName);
                        sourceFile = new File(file.filePath + "/" + newSaveName);
                        blob.upload(new FileInputStream(sourceFile), sourceFile.length());
                        tempName = newSaveName;
                    }

                    // 내부 리사이징 파일 삭제
                    for (Integer size : this.reSizes) {
                        newSaveName = first + "-" + size + ext;
                        this.delete(file.filePath, newSaveName);
                    }
                }

                // 내부 원본 파일 삭제
                this.delete(file.filePath, saveName);
            }
        } catch (StorageException | URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

}
