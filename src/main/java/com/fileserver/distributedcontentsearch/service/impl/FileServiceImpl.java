package com.fileserver.distributedcontentsearch.service.impl;

import com.fileserver.distributedcontentsearch.DistributedContentSearchApplication;
import com.fileserver.distributedcontentsearch.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

@Service
@Slf4j
public class FileServiceImpl implements FileService {
    Random random = new Random();
    String[] files = new String[20];
    String[] servingFiles;

    public FileServiceImpl() {
        try {
        File file = ResourceUtils.getFile("fileNames.txt");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        String line;
        int counter=0;
        while ((line = bufferedReader.readLine()) != null){
            files[counter] = line;
            counter++;
        }
        bufferedReader.close();
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        setServingFiles();
    }

    public void setServingFiles(){
        int rand = 3 + random.nextInt(6-3);
        servingFiles = new String[rand];
        for(int i=0; i<rand; i++) {
            int index = random.nextInt(20);
            servingFiles[i] = files[index];
        }
        DistributedContentSearchApplication.filesServed = this.servingFiles;
    }

    @Override
    public String getFile(String fileName) {
        for (String servingFile : servingFiles) {
            if (servingFile.equals(fileName)) {
                return servingFile;
            }
        }
        return null;
    }

    @Override
    public ResponseEntity<Resource> downloadContent(String fileName) {
        boolean isIncluded = false;
        for(String file: servingFiles){
            if(file.equalsIgnoreCase(fileName)){
                isIncluded = true;
                break;
            }
        }

        //check if file is serving
        if (isIncluded) {
            try {
                Random random = new Random();
                int fileSize = (2 + random.nextInt(8)) * 1024 * 1024;
                char[] chars = new char[fileSize];
                Arrays.fill(chars, 'c');
                String writeString = new String(chars);

                //calculating hash
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(writeString.getBytes(StandardCharsets.UTF_8));
                String encodeToString = Base64.getEncoder().encodeToString(hash);
                log.info("File: {} \nFile Size: {}MB\nHash: {}", fileName, fileSize / (1024 * 1024), encodeToString );

                //create file
//                String workingDirectory = System.getProperty("user.dir");
                String target = "createdFiles/" + fileName + ".txt";

                BufferedWriter writer = new BufferedWriter(new FileWriter(target));
                writer.write(writeString);
                writer.close();

                HttpHeaders headers = new HttpHeaders();
                String headerValue = "attachment; filename=" + fileName + ".txt";
                headers.add(HttpHeaders.CONTENT_DISPOSITION, headerValue);

                File file = ResourceUtils.getFile(target);
                Path path = Paths.get(file.getAbsolutePath());
                ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

                //send created file
                return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(file.length())
                        .contentType(MediaType.parseMediaType("application/octet-stream"))
                        .body(resource);
                } catch (IOException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    log.error(e.getMessage());
                    return ResponseEntity.internalServerError().build();
                }
            } else{
                log.warn("File does not exists!");
                return ResponseEntity.noContent().build();
        }
    }

    @Override
    public String[] getServingFiles() {
        return servingFiles;
    }

    @Override
    public String[] getAll()  {
        return files;
    }
}
