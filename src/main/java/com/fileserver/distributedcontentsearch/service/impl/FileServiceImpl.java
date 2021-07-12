package com.fileserver.distributedcontentsearch.service.impl;

import com.fileserver.distributedcontentsearch.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class FileServiceImpl implements FileService {

    @Override
    public String[] getAllServingFiles() {
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
