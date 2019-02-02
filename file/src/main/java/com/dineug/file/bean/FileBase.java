package com.dineug.file.bean;

import com.mortennobel.imagescaling.AdvancedResizeOp;
import com.mortennobel.imagescaling.MultiStepRescaleOp;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.*;

/**
 * base file
 * 기본 파일처리
 *
 * @author dineug2@gmail.com
 * @version 2019.01.31 v1
 */
@Component
public class FileBase {


    /**
     * 내부 parameter
     */
    protected class FileVO {
        Map<String, Object> fileData = new HashMap<>();
        String filePath;
        String fileServerPath;
        String fileHttpPath;

        MultipartHttpServletRequest mpreq;
        Map<String, List<String>> fileNames = new HashMap<>();
        List<String> oldNames = new ArrayList<>();
        List<String> saveNames = new ArrayList<>();
        List<Long> fileSizes = new ArrayList<>();

        HttpServletResponse res;
        String oldName;
        String saveName;
        byte[] fileByte;

        {
            fileData.put("fileNames", fileNames);
            fileData.put("fileSizes", fileSizes);
            fileNames.put("oldNames", oldNames);
            fileNames.put("saveNames", saveNames);
        }
    }

    // 이미지 리사이즈
    protected List<Integer> reSizes = new ArrayList<Integer>() {{
        add(600);
        add(400);
        add(200);
    }};

    // 이미지 파일 정규식
    protected String imageRegExp = "^([\\S]+(\\.(?i)(jpe?g|png|gif))$)";


    /**
     * 파일 업로드
     *
     * @param multipartRequest
     * @param filePath
     * @return
     */
    public FileUtil upload(MultipartHttpServletRequest multipartRequest, String filePath, boolean isImageResize) {
        FileVO file = new FileVO();
        file.mpreq = multipartRequest;
        file.filePath = filePath;

        uploadData(file);
        uploadProcess(file);
        return new FileUtil(file.fileData);
    }

    // 파일 업로드 전 정보 셋팅
    protected void uploadData(FileVO file) {
        Iterator<String> itr = file.mpreq.getFileNames();
        StringBuffer sb = null;
        String oldFileName = null;
        String saveFileName = null;
        String ext = null;

        while (itr.hasNext()) {
            MultipartFile mpf = file.mpreq.getFile(itr.next());
            oldFileName = mpf.getOriginalFilename();

            if (!oldFileName.equals("")) {
                sb = new StringBuffer();
                ext = oldFileName.substring(oldFileName.lastIndexOf("."));
                saveFileName = sb.append(UUID.randomUUID().toString()).append(ext).toString();

                file.oldNames.add(oldFileName);
                file.saveNames.add(saveFileName);
                file.fileSizes.add(mpf.getSize());
            }
        }
    }

    // 파일 업로드 처리
    protected void uploadProcess(FileVO file) {
        Iterator<String> itr = file.mpreq.getFileNames();
        int i = 0;

        while (itr.hasNext()) {
            MultipartFile mpf = file.mpreq.getFile(itr.next());

            if (!mpf.getOriginalFilename().equals("")) {
                try {
                    // 경로생성
                    mkdir(file);

                    // 파일 저장
                    mpf.transferTo(new File(file.filePath + "/" + file.saveNames.get(i++)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
    public FileUtil down(HttpServletResponse response, String filePath, String saveName, String oldName) {
        FileVO file = new FileVO();
        file.res = response;
        file.filePath = filePath;
        file.saveName = saveName;
        file.oldName = oldName;

        downProcess(file);
        file.fileData.put("fileByte", file.fileByte);
        return new FileUtil(file.fileData);
    }

    // 파일 다운로드 처리
    protected void downProcess(FileVO file) {
        try {
            file.fileByte = Files.readAllBytes(new File(file.filePath + "/" + file.saveName).toPath());

            // 다운로드 response 셋팅
            downResponse(file);
        } catch (IOException e) {
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
    public boolean delete(String filePath, String saveName) {
        return deleteProcess(filePath + "/" + saveName);
    }

    // 파일 삭제 처리
    protected boolean deleteProcess(String filePath) {
        boolean isDelete = false;
        File file = new File(filePath);

        if (file.exists()) {
            isDelete = file.delete();
        }
        return isDelete;
    }


    // 파일 경로 생성
    protected void mkdir(FileVO file) {
        File targetDir = new File(file.filePath + "/");

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
    }

    // 다운로드 response 셋팅
    protected void downResponse(FileVO file) throws IOException {
        file.res.setContentType("application/octet-stream");
        file.res.setContentLength(file.fileByte.length);
        file.res.setHeader("Content-Transfer-Encoding", "binary");
        // 다운로드시 변경할 파일명
        file.res.setHeader("Content-Disposition", "attachment; fileName=\"" + URLEncoder.encode(file.oldName, "UTF-8") + "\";");
    }

    // 이미지 리사이징 처리
    protected boolean imageResizeProcess(int width, String filePath, String oldName, String newName) throws IOException {
        String ext = oldName.substring(oldName.lastIndexOf(".")).replaceAll("\\.", "");
        File input = new File(filePath + "/" + oldName);
        BufferedImage img = ImageIO.read(input);
        int height = width * img.getHeight() / img.getWidth();

        // java-image-scaling
        MultiStepRescaleOp rescale = new MultiStepRescaleOp(width, height);
        rescale.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Soft);
        BufferedImage resizedImage = rescale.filter(img, null);

        File output = new File(filePath + "/" + newName);
        return ImageIO.write(resizedImage, ext, output);
    }

}
