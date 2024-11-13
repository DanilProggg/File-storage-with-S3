package com.goodprod.project.controllers;

import com.goodprod.project.services.S3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/s3")
public class S3Controller {


    private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile multipartFile,
                                        @RequestParam("folder") String currentFolder,
                                        Principal principal) {
        System.out.println(currentFolder);
        File file = null;
        try {
            // Создание временного файла
            file = new File(System.getProperty("java.io.tmpdir"), multipartFile.getOriginalFilename());
            multipartFile.transferTo(file);

            String key = (currentFolder.endsWith("/") ? currentFolder : currentFolder + "/") + multipartFile.getOriginalFilename();

            // Загрузка файла в S3
            return s3Service.uploadFile(principal, file, key, currentFolder);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>("Ошибка при загрузке файла: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            if (file != null && file.exists()) {
                file.delete(); // Удаление временного файла после загрузки
            }
        }
    }


    @GetMapping("/files")
    public ResponseEntity<?> listFiles() {
        List<Map<String, String>> files = s3Service.listFiles();
        return ResponseEntity.ok(files);
    }

    @PostMapping("/delete-file")
    public ResponseEntity<?> deleteFile(@RequestParam("file-name") String fileName,
                                        @RequestParam("current-folder") String currentFolder,
                                        Principal principal){
        System.out.println("Folder: "+currentFolder + "    Name: "+ fileName);
        return s3Service.deleteFile(principal, fileName, currentFolder);
    }


    @PostMapping("/create-folder")
    public ResponseEntity<?> createFolder(@RequestParam("create-name") String folderName,
                                           @RequestParam("current-folder") String currentFolder,
                                           Principal principal){
        return s3Service.createFolder(principal, folderName, currentFolder);
    }


    @PostMapping("/delete-folder")
    public ResponseEntity<?> deleteFolder(@RequestParam("delete-name") String folderName,
                                           @RequestParam("current-folder") String currentFolder,
                                           Principal principal){
        return s3Service.deleteFolder(principal, folderName, currentFolder);
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadFile(@RequestParam String key) {
        System.out.println(key);
        byte[] fileData = s3Service.downloadFile(key);

        // Устанавливаем имя файла и кодировку UTF-8
        String filename = URLEncoder.encode(key.substring(key.lastIndexOf("/") + 1), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileData);
    }
}
