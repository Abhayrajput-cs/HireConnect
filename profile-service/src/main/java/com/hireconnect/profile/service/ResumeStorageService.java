package com.hireconnect.profile.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.hireconnect.profile.dto.ResumeUploadResponse;
import com.hireconnect.profile.exception.ApiException;

@Service
public class ResumeStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final Path resumeDirectory;
    private final String publicBaseUrl;

    public ResumeStorageService(
        @Value("${app.files.resume-dir:uploads/resumes}") String resumeDirectory,
        @Value("${app.files.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.resumeDirectory = Paths.get(resumeDirectory).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl;
    }

    public ResumeUploadResponse uploadResume(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Resume file is required");
        }

        String originalFileName = file.getOriginalFilename();
        String extension = resolveExtension(originalFileName);
        String contentType = normalizeContentType(file.getContentType(), extension);
        String storedFileName = UUID.randomUUID() + "." + extension;

        try {
            Files.createDirectories(resumeDirectory);
            Path target = resumeDirectory.resolve(storedFileName).normalize();
            if (!target.startsWith(resumeDirectory)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid resume file path");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store resume file");
        }

        return new ResumeUploadResponse(
            storedFileName,
            originalFileName == null || originalFileName.isBlank() ? storedFileName : originalFileName,
            contentType,
            file.getSize(),
            normalizeBaseUrl(publicBaseUrl) + "/api/v1/profiles/resumes/" + storedFileName
        );
    }

    public Resource loadResume(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Resume file name is required");
        }

        try {
            Path filePath = resumeDirectory.resolve(fileName).normalize();
            if (!filePath.startsWith(resumeDirectory)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid resume file path");
            }
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Resume file not found");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Resume file not found");
            }
            return resource;
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to load resume file");
        }
    }

    public String determineContentType(String fileName) {
        String extension = resolveExtension(fileName);
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }

    private String resolveExtension(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.contains(".")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Resume file must have a valid extension");
        }

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only PDF, DOC, and DOCX resumes are supported");
        }
        return extension;
    }

    private String normalizeContentType(String contentType, String extension) {
        String resolvedContentType = contentType == null || contentType.isBlank()
            ? determineContentType("resume." + extension)
            : contentType;

        if (!ALLOWED_CONTENT_TYPES.contains(resolvedContentType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported resume content type");
        }
        return resolvedContentType;
    }

    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
