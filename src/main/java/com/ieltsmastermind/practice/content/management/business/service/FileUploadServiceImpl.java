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

    // 25 MB
    private static final long MAX_FILE_SIZE_BYTES = 25L * 1024 * 1024;

    @Override
    public String uploadImage(MultipartFile file) {
        validateImage(file);
        return storeFile(file, IMAGE_UPLOAD_DIR, IMAGE_PUBLIC_BASE_PATH);
    }

    @Override
    public void deleteImageByUrl(FileDeleteRequestDto request) {
        String imageUrl = request.getFileUrl();

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new RuntimeException("imageUrl is required");
        }

        String filename = extractFilenameFromPublicUrl(imageUrl, IMAGE_PUBLIC_BASE_PATH);

        Path uploadPath = Paths.get(IMAGE_UPLOAD_DIR).toAbsolutePath().normalize();
        Path target = uploadPath.resolve(filename).normalize();

        try {
            boolean deleted = Files.deleteIfExists(target);
            if (!deleted) {
                throw new RuntimeException("Image file not found");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete image", e);
        }
    }

    @Override
    public String uploadThumbnail(MultipartFile file) {
        validateThumbnail(file);

        return storeFile(file, THUMBNAIL_UPLOAD_DIR, THUMBNAIL_PUBLIC_BASE_PATH);
    }

    @Override
    public String uploadAudio(MultipartFile file) {
        validateAudio(file);

        return storeFile(file, AUDIO_UPLOAD_DIR, AUDIO_PUBLIC_BASE_PATH);
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        validateAvatar(file);

        return storeFile(file, AVATAR_UPLOAD_DIR, AVATAR_PUBLIC_BASE_PATH);
    }

    @Override
    public void deleteThumbnailByUrl(FileDeleteRequestDto request) {
        String thumbnailUrl = request.getFileUrl();

        if (thumbnailUrl == null || thumbnailUrl.trim().isEmpty()) {
            throw new RuntimeException("thumbnailUrl is required");
        }

        String filename = extractFilenameFromPublicUrl(thumbnailUrl, THUMBNAIL_PUBLIC_BASE_PATH);

        Path uploadPath = Paths.get(THUMBNAIL_UPLOAD_DIR).toAbsolutePath().normalize();
        Path target = uploadPath.resolve(filename).normalize();

        try {
            boolean deleted = Files.deleteIfExists(target);
            if (!deleted) {
                throw new RuntimeException("Thumbnail file not found");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete thumbnail", e);
        }
    }

    @Override
    public void deleteAudioByUrl(FileDeleteRequestDto request) {
        String audioUrl = request.getFileUrl();

        if (audioUrl == null || audioUrl.trim().isEmpty()) {
            throw new RuntimeException("audioUrl is required");
        }

        String filename = extractFilenameFromPublicUrl(audioUrl, AUDIO_PUBLIC_BASE_PATH);

        Path uploadPath = Paths.get(AUDIO_UPLOAD_DIR).toAbsolutePath().normalize();
        Path target = uploadPath.resolve(filename).normalize();

        try {
            boolean deleted = Files.deleteIfExists(target);
            if (!deleted) {
                throw new RuntimeException("Audio file not found");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete audio file", e);
        }
    }

    @Override
    public void deleteAvatarByUrl(FileDeleteRequestDto request) {
        String avatarUrl = request.getFileUrl();

        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            throw new RuntimeException("avatarUrl is required");
        }

        String filename = extractFilenameFromPublicUrl(avatarUrl, AVATAR_PUBLIC_BASE_PATH);

        Path uploadPath = Paths.get(AVATAR_UPLOAD_DIR).toAbsolutePath().normalize();
        Path target = uploadPath.resolve(filename).normalize();

        try {
            boolean deleted = Files.deleteIfExists(target);
            if (!deleted) {
                throw new RuntimeException("Avatar file not found");
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to delete avatar", e);
        }
    }

    private void validateFileBase(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Empty file");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new RuntimeException("File too large");
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
            throw new RuntimeException("Missing file name");
        }
        if (file.getContentType() == null || file.getContentType().trim().isEmpty()) {
            throw new RuntimeException("Missing content type");
        }
    }

    private void validateImage(MultipartFile file) {
        validateFileBase(file);

        String contentType = file.getContentType();
        if (!Set.of("image/png", "image/jpeg", "image/webp").contains(contentType)) {
            throw new RuntimeException("Invalid image type");
        }
    }

    private void validateThumbnail(MultipartFile file) {
        validateFileBase(file);

        String contentType = file.getContentType();
         if (!Set.of("image/png", "image/jpeg", "image/webp").contains(contentType)) {
             throw new RuntimeException("Invalid image type");
         }
    }

    private void validateAudio(MultipartFile file) {
        validateFileBase(file);

        String contentType = file.getContentType();
        if (!Set.of("audio/mpeg", "audio/wav").contains(contentType)) {
            throw new RuntimeException("Invalid audio type");
        }
    }

    private void validateAvatar(MultipartFile file) {
        validateFileBase(file);

        String contentType = file.getContentType();
        if (!Set.of("image/png", "image/jpeg", "image/webp").contains(contentType)) {
            throw new RuntimeException("Invalid image type");
        }
    }

    private String storeFile(MultipartFile file, String uploadDir, String publicBasePath) {
        try {
            String originalName = file.getOriginalFilename();
            String safeName = (originalName != null ? originalName.replaceAll("\\s+", "_") : "file");
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
        if (urlOrPath == null) {
            throw new RuntimeException("File Url is required");
        }

        String path = urlOrPath.trim();

        if (!path.startsWith(publicBasePath)) {
            throw new RuntimeException("File Url must start with " + publicBasePath);
        }

        String filename = path.substring(publicBasePath.length());

        if (filename.isBlank()) {
            throw new RuntimeException("File Url missing filename");
        }

        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new RuntimeException("Invalid File Url");
        }

        return filename;
    }
}
