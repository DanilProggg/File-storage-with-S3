package com.goodprod.project.services;

import com.goodprod.project.entities.Role;
import com.goodprod.project.entities.User;
import com.goodprod.project.repos.UserRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.nio.file.Files;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class S3Service {

    private final UserRepo userRepo;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3Service(UserRepo userRepo, S3Client s3Client) {
        this.userRepo = userRepo;
        this.s3Client = s3Client;
    }

    // Загрузка файла в S3
    public ResponseEntity<?> uploadFile(Principal principal, File file, String key, String currentFolder) {
        try {
            User user = userRepo.findByLogin(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            // Проверка прав на загрузку файла
            boolean isAdminOrTeacher = user.getRoles().contains(Role.ROLE_ADMIN) || user.getRoles().contains(Role.ROLE_TEACHER);
            boolean isStudent = user.getRoles().contains(Role.ROLE_STUDENT);

            // Проверка на существование файла
            boolean fileExists = false;
            try {
                fileExists = s3Client.headObject(b -> b.bucket(bucketName).key(key)).sdkHttpResponse().isSuccessful();
            } catch (NoSuchKeyException e) {
                // Файл не существует, игнорируем
                fileExists = false;
            }

            // Логика для студентов
            if (isStudent) {
                // Если файл существует и это не его файл, то запрещаем замену
                if (fileExists && !isOwner(user, key)) {
                    return new ResponseEntity<>("Access Denied: You can only replace your own files.", HttpStatus.FORBIDDEN);
                }
            }

            // Если файл с таким названием существует, удаляем его (только для админов и преподавателей)
            if (fileExists && isAdminOrTeacher) {
                s3Client.deleteObject(b -> b.bucket(bucketName).key(key));
            }

            // Метаданные файла
            Map<String, String> metadata = new HashMap<>();
            metadata.put("author", user.getLogin());

            // Загрузка нового файла
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .metadata(metadata)
                            .build(),
                    RequestBody.fromBytes(Files.readAllBytes(file.toPath())));

            return ResponseEntity.ok(listFiles());
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<?> deleteFile(Principal principal, String fileName, String currentFolder) {
        try {
            User user = userRepo.findByLogin(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            // Добавляем слеш в конце, если его нет
            currentFolder = currentFolder.endsWith("/") || currentFolder.isEmpty() ? currentFolder : currentFolder + "/";
            String key = currentFolder + fileName;

            System.out.println(currentFolder);
            System.out.println(key);

            // Проверка прав на удаление
            if (user.getRoles().contains(Role.ROLE_ADMIN) || user.getRoles().contains(Role.ROLE_TEACHER) ||
                    (user.getRoles().contains(Role.ROLE_STUDENT) && isOwner(user, key))) {

                // Удаляем файл или папку
                s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());

                // Добавляем пустой объект-заглушку для сохранения "папки" после каждого удаления
                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(currentFolder) // Добавляем заглушку в виде папки
                                .build(),
                        RequestBody.fromBytes(new byte[0])
                );

                return ResponseEntity.ok(listFiles()); // Возвращаем оставшиеся файлы
            } else {
                return new ResponseEntity<>("Access Denied", HttpStatus.FORBIDDEN);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    private boolean isOwner(User user, String key) {
        Map<String, String> metadata = s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build()).metadata();
        String author = metadata.get("author");
        return user.getLogin().equals(author);
    }


    // Получение списка файлов
    public List<Map<String, String>> listFiles() {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build(); // Убираем префикс и делимитеры

        ListObjectsV2Response result = s3Client.listObjectsV2(request);

        List<Map<String, String>> filesAndFolders = new ArrayList<>();

        // Обрабатываем содержимое
        for (S3Object object : result.contents()) {
            Map<String, String> info = new HashMap<>();
            info.put("name", object.key());

            // Определяем, является ли объект папкой или файлом
            if (object.key().endsWith("/")) {
                info.put("type", "folder"); // Это папка
            } else {
                info.put("type", "file"); // Это файл
            }

            filesAndFolders.add(info);
        }

        return filesAndFolders;
    }




    public ResponseEntity<?> createFolder(Principal principal, String folderName, String currentFolder) {
        try {
            User user = userRepo.findByLogin(principal.getName()).orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            // Проверка прав на создание папки
            if (user.getRoles().contains(Role.ROLE_ADMIN) || user.getRoles().contains(Role.ROLE_TEACHER)) {
                //Слеш в конце если его не поставили
                currentFolder = currentFolder.endsWith("/") || currentFolder.isEmpty() ? currentFolder : currentFolder + "/";
                // Если префикс не заканчивается на "/", добавляем его для обозначения "папки"
                folderName = folderName.endsWith("/") ? folderName : folderName + "/";
                String folderKey = currentFolder + folderName;

                Map<String, String> metadata = new HashMap<>();
                metadata.put("author", user.getLogin());

                // Создаем пустой объект-заглушку для создания "папки"
                s3Client.putObject(PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(folderKey)
                                .metadata(metadata)
                                .build(),
                        RequestBody.fromBytes(new byte[0]));

                return ResponseEntity.ok(listFiles());
            } else {
                return new ResponseEntity<>("Access Denied", HttpStatus.FORBIDDEN);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<?> deleteFolder(Principal principal, String folderName, String currentFolder) {
        try {
            //Слеш в конце если его не поставили
            currentFolder = currentFolder.endsWith("/") || currentFolder.isEmpty() ? currentFolder : currentFolder + "/";

            folderName = folderName.endsWith("/") ? folderName : folderName + "/";
            String folderKey = currentFolder + folderName;

            User user = userRepo.findByLogin(principal.getName()).orElseThrow(()-> new RuntimeException("Пользователь не найден"));
            if(user.getRoles().contains(Role.ROLE_ADMIN) || user.getRoles().contains(Role.ROLE_TEACHER)){
                //Удаляем
                s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(folderKey).build());

                return ResponseEntity.ok(listFiles());
            } else {
                //Получение методанных объекта
                Map<String,String> metadata = s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(folderKey).build()).metadata();
                //Если сутдент - проверяем владельца
                User owner = userRepo.findByLogin(metadata.get("author")).orElseThrow(()->new RuntimeException("Владелец не найден"));
                if(user.getLogin().equals(metadata.get("author"))){
                    //Удаляем
                    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(folderKey).build());
                    return ResponseEntity.ok(listFiles());

                }
                return new ResponseEntity<>("Access Denied", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    public byte[] downloadFile(String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            return objectBytes.asByteArray();
        } catch (S3Exception e) {
            throw new RuntimeException("Error downloading file: " + e.getMessage(), e);
        }
    }

}

