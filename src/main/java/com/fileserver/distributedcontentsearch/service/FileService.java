package com.fileserver.distributedcontentsearch.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;

public interface FileService {
    String[] getAllServingFiles();

    HashMap<String, String> getFile(String fileName);

    ResponseEntity<Resource> downloadContent(String fileName);
}
