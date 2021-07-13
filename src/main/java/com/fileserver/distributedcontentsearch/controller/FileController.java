package com.fileserver.distributedcontentsearch.controller;

import com.fileserver.distributedcontentsearch.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@Slf4j
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;

    @Autowired
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/all")
    public String[] getAll() {
        log.info("Getting all the serving files");
        return fileService.getServingFiles();
    }

    @GetMapping("/file/{fileName}")
    public String getFile(@PathVariable String fileName){
        log.info("Get file -> {}", fileName);
        return   fileService.getFile(fileName);
    }

    @RequestMapping(path = "/download/{fileName}", method = RequestMethod.GET)
    public ResponseEntity<Resource> download(@PathVariable String fileName) {
        log.info("Download file -> {}", fileName);
        return fileService.downloadContent(fileName);
    }
}
