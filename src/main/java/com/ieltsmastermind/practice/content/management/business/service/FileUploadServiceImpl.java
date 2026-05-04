package com.ieltsmastermind.practice.content.management.business.service;

import com.ieltsmastermind.practice.content.management.business.interfaces.FileUploadService;
import com.ieltsmastermind.practice.content.management.domain.dto.FileDeleteRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import static com.ieltsmastermind.common.constants.FileStorageConstants.*;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    // 100 MB
    private static final long MAX_FILE_SIZE_BYTES = 100L * 1024 * 1024;

    @Override
    public String uploadImage(MultipartFile file) {
        if (isMissing(file)) {
            return null;
        }

        validateImage(file);
        return storeFile(file, IMAGE_UPLOAD_DIR, IMAGE_PUBLIC_BASE_PATH);
    }

    @Override
    public void deleteImageByUrl(FileDeleteRequestDto request) {
        String imageUrl = request != null ? request.getFileUrl() : null;

        if (isBlank(imageUrl)) {
            return;
        }

        String filename = extractFilenameFromPublicUrl(imageUrl, IMAGE_PUBLIC_BASE_PATH);
        if (isBlank(filename)) {
            return;
        }

        Path uploadPath = Paths.get(IMAGE_UPLOAD_DIR).toAbsolutePath().normalize();
        Path target = uploadPath.resolve(filename).normalize();

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete image", e);
        }
    }

    @Override
    public String uploadThumbnail(MultipartFile file) {
        if (isMissing(file)) {
            return null;
        }

        validateThumbnail(file);
        return storeFile(file, THUMBNAIL_UPLOAD_DIR, THUMBNAIL_PUBLIC_BASE_PATH);
    }

    @Override
    public String uploadAudio(MultipartFile file) {
        if (isMissing(file)) {
            return null;
        }

        validateAudio(file);
        return storeFile(file, AUDIO_UPLOAD_DIR, AUDIO_PUBLIC_BASE_PATH);
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        if (isMissing(file)) {
            return null;
        }

        validateAvatar(file);
        return storeFile(file, AVATAR_UPLOAD_DIR, AVATAR_PUBLIC_BASE_PATH);
    }

    @Override
    public void deleteThumbnailByUrl(FileDeleteRequestDto request) {
        String thumbnailUrl = request != null ? request.getFileUrl() : null;

        if (isBlank(thumbnailUrl)) {
            return;
        }

        String filename = extractFilenameFromPublicUrl(thumbnailUrl, THUMBNAIL_PUBLIC_BASE_PATH);
        if (isBlank(filename)) {
            return;
        }

        Path uploadPath = Paths.get(THUMBNAIL_UPLOAD_DIR).toAbsolutePath().normalize();
        Path target = uploadPath.resolve(filename).normalize();

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete thumbnail", e);
        }
    }

    @Override
    public void deleteAudioByUrl(FileDeleteRequestDto request) {
        String audioUrl = request != null ? request.getFileUrl() : null;

        if (isBlank(audioUrl)) {
            return;
        }

        String filename = extractFilenameFromPublicUrl(audioUrl, AUDIO_PUBLIC_BASE_PATH);
        if (isBlank(filename)) {
            return;
        }

        Path uploadPath = Paths.get(AUDIO_UPLOAD_DIR).toAbsolutePath().normalize();
        Path target = uploadPath.resolve(filename).normalize();

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete audio file", e);
        }
    }

    @Override
    public void deleteAvatarByUrl(FileDeleteRequestDto request) {
        String avatarUrl = request != null ? request.getFileUrl() : null;

        if (isBlank(avatarUrl)) {
            return;
        }

        String filename = extractFilenameFromPublicUrl(avatarUrl, AVATAR_PUBLIC_BASE_PATH);
        if (isBlank(filename)) {
            return;
        }

        Path uploadPath = Paths.get(AVATAR_UPLOAD_DIR).toAbsolutePath().normalize();
        Path target = uploadPath.resolve(filename).normalize();

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete avatar", e);
        }
    }

    private boolean isMissing(MultipartFile file) {
        return file == null || file.isEmpty();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void validateFileBase(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new RuntimeException("File too large");
        }
    }

    private void validateImage(MultipartFile file) {
        validateFileBase(file);

        String contentType = file.getContentType();
        if (!isBlank(contentType) && !Set.of("image/png", "image/jpeg", "image/webp").contains(contentType)) {
            throw new RuntimeException("Invalid image type");
        }
    }

    private void validateThumbnail(MultipartFile file) {
        validateFileBase(file);

        String contentType = file.getContentType();
        if (!isBlank(contentType) && !Set.of("image/png", "image/jpeg", "image/webp").contains(contentType)) {
            throw new RuntimeException("Invalid image type");
        }
    }

    private void validateAudio(MultipartFile file) {
        validateFileBase(file);

        String contentType = file.getContentType();
        if (!isBlank(contentType) && !Set.of("audio/mpeg", "audio/wav").contains(contentType)) {
            throw new RuntimeException("Invalid audio type");
        }
    }

    private void validateAvatar(MultipartFile file) {
        validateFileBase(file);

        String contentType = file.getContentType();
        if (!isBlank(contentType) && !Set.of("image/png", "image/jpeg", "image/webp").contains(contentType)) {
            throw new RuntimeException("Invalid image type");
        }
    }

    private String storeFile(MultipartFile file, String uploadDir, String publicBasePath) {
        if (isMissing(file)) {
            return null;
        }

        try {
            String originalName = file.getOriginalFilename();
            String safeName = !isBlank(originalName) ? originalName.replaceAll("\\s+", "_") : "file";
            String filename = UUID.randomUUID() + "-" + safeName;

            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            Path target = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return publicBasePath + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    private String extractFilenameFromPublicUrl(String urlOrPath, String publicBasePath) {
        if (isBlank(urlOrPath)) {
            return null;
        }

        String path = urlOrPath.trim();

        if (!path.startsWith(publicBasePath)) {
            throw new RuntimeException("File Url must start with " + publicBasePath);
        }

        String filename = path.substring(publicBasePath.length());

        if (filename.isBlank()) {
            return null;
        }

        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new RuntimeException("Invalid File Url");
        }

        return filename;
    }
}
