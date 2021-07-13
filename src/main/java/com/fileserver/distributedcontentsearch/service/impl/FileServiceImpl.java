package com.fileserver.distributedcontentsearch.service.impl;

import com.fileserver.distributedcontentsearch.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

@Service
public class FileServiceImpl implements FileService {
    Random randomNum = new Random();
    String[] files = new String[20];
    String[] servingFiles;

    public FileServiceImpl() throws IOException {
        File file = ResourceUtils.getFile("classpath:static/fileNames.txt");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        String line;
        int counter=0;
        while ((line = bufferedReader.readLine()) != null){
            files[counter] = line;
            counter++;
        }
        bufferedReader.close();
        setServingFiles();
    }

    public void setServingFiles(){
        int rand = 3 + randomNum.nextInt(6-3);
        servingFiles = new String[rand];
        for(int i=0; i<rand; i++) {
            int index = randomNum.nextInt(20);
            servingFiles[i] = files[index];
        }
    }

    @Override
    public String[] getAllFiles() {
        return new String[0];
    }

    @Override
    public HashMap<String, String> getFile(String fileName) {
        return null;
    }

    @Override
    public ResponseEntity<Resource> downloadContent(String fileName) {
        return null;
    }
}
