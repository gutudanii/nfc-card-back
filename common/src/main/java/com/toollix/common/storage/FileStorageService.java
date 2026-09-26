package com.toollix.common.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Handles standard file storage directly to the local server disk.
 * 
 * This avoids locking into S3 and allows the application to be self-hosted
 * entirely. Files are saved into the application's root ./uploads directory.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${storage.upload-dir:uploads}")
    private String uploadDir;

    private Path fileStorageLocation;

    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            log.error("Could not create the directory where the uploaded files will be stored.", ex);
            throw new RuntimeException("Could not create upload directory", ex);
        }
        log.info("[STORAGE] Local upload directory initialized at: {}", this.fileStorageLocation);
    }

    /**
     * Saves a multipart file to the local disk and returns the full
     * absolute URL where it can be downloaded.
     */
    public String storeFile(MultipartFile file) {
        String originalName = StringUtils
                .cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");

        // Prevent path traversal
        if (originalName.contains("..")) {
            throw new IllegalArgumentException("Sorry! Filename contains invalid path sequence: " + originalName);
        }

        // Generate safe unique filename
        String ext = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            ext = originalName.substring(dotIndex);
        }
        String fileName = UUID.randomUUID().toString() + ext;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(fileName)
                    .toUriString();

            log.info("[STORAGE] File saved successfully. URL: {}", fileDownloadUri);
            return fileDownloadUri;
        } catch (IOException ex) {
            log.error("[STORAGE] Could not store file {}", fileName, ex);
            throw new RuntimeException("Could not store file " + originalName + ". Please try again!", ex);
        }
    }

    public Path getFilePath(String fileName) {
        return this.fileStorageLocation.resolve(fileName).normalize();
    }
}
