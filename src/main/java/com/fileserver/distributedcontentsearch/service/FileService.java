package com.fileserver.distributedcontentsearch.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface FileService {

    String getFile(String fileName);

    ResponseEntity<Resource> downloadContent(String fileName);

    String[] getServingFiles();

    String[] getAll();
}
