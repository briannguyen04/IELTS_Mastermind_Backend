package com.ieltsmastermind.practice.content.management.business.service;

import com.ieltsmastermind.practice.content.management.domain.dto.FileDeleteRequestDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.ieltsmastermind.common.constants.FileStorageConstants.AUDIO_PUBLIC_BASE_PATH;
import static com.ieltsmastermind.common.constants.FileStorageConstants.AUDIO_UPLOAD_DIR;
import static com.ieltsmastermind.common.constants.FileStorageConstants.AVATAR_PUBLIC_BASE_PATH;
import static com.ieltsmastermind.common.constants.FileStorageConstants.AVATAR_UPLOAD_DIR;
import static com.ieltsmastermind.common.constants.FileStorageConstants.IMAGE_PUBLIC_BASE_PATH;
import static com.ieltsmastermind.common.constants.FileStorageConstants.IMAGE_UPLOAD_DIR;
import static com.ieltsmastermind.common.constants.FileStorageConstants.THUMBNAIL_PUBLIC_BASE_PATH;
import static com.ieltsmastermind.common.constants.FileStorageConstants.THUMBNAIL_UPLOAD_DIR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceImplTest {

    private static final byte[] SAMPLE_BYTES = "sample-content".getBytes(StandardCharsets.UTF_8);
    private static final long TOO_LARGE_FILE_SIZE = 100L * 1024 * 1024 + 1;

    private final List<Path> createdPaths = new ArrayList<>();

    @InjectMocks
    private FileUploadServiceImpl fileUploadService;

    @AfterEach
    void cleanupCreatedFiles() {
        createdPaths.stream()
                .sorted(Comparator.comparingInt((Path path) -> path.getNameCount()).reversed())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });

        createdPaths.clear();
    }

    @Test
    void uploadImage_whenFileIsNull_shouldReturnNull() {
        String result = fileUploadService.uploadImage(null);

        assertThat(result).isNull();
    }

    @Test
    void uploadImage_whenFileIsEmpty_shouldReturnNull() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                new byte[0]
        );

        String result = fileUploadService.uploadImage(file);

        assertThat(result).isNull();
    }

    @Test
    void uploadImage_whenFileIsValid_shouldStoreImageAndReturnPublicUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "my image.png",
                "image/png",
                SAMPLE_BYTES
        );

        String result = fileUploadService.uploadImage(file);

        assertThat(result).isNotNull();
        assertThat(result).startsWith(IMAGE_PUBLIC_BASE_PATH);
        assertThat(result).endsWith("-my_image.png");

        Path target = pathFromPublicUrl(result, IMAGE_UPLOAD_DIR, IMAGE_PUBLIC_BASE_PATH);

        assertThat(Files.exists(target)).isTrue();
        assertThat(Files.readAllBytes(target)).isEqualTo(SAMPLE_BYTES);
    }

    @Test
    void uploadImage_whenContentTypeIsBlank_shouldStoreImage() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.png",
                null,
                SAMPLE_BYTES
        );

        String result = fileUploadService.uploadImage(file);

        assertThat(result).isNotNull();
        assertThat(result).startsWith(IMAGE_PUBLIC_BASE_PATH);

        Path target = pathFromPublicUrl(result, IMAGE_UPLOAD_DIR, IMAGE_PUBLIC_BASE_PATH);

        assertThat(Files.exists(target)).isTrue();
    }

    @Test
    void uploadImage_whenContentTypeIsInvalid_shouldThrowRuntimeExceptionAndSkipStore() throws IOException {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1L);
        when(file.getContentType()).thenReturn("application/pdf");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> fileUploadService.uploadImage(file)
        );

        assertThat(exception.getMessage()).isEqualTo("Invalid image type");

        verify(file, never()).getInputStream();
    }

    @Test
    void uploadImage_whenFileIsTooLarge_shouldThrowRuntimeExceptionAndSkipStore() throws IOException {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(TOO_LARGE_FILE_SIZE);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> fileUploadService.uploadImage(file)
        );

        assertThat(exception.getMessage()).isEqualTo("File too large");

        verify(file, never()).getContentType();
        verify(file, never()).getInputStream();
    }

    @Test
    void uploadImage_whenStoreFails_shouldThrowRuntimeException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("image.png");
        when(file.getInputStream()).thenThrow(new IOException("Cannot read file"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> fileUploadService.uploadImage(file)
        );

        assertThat(exception.getMessage()).isEqualTo("Failed to store file");
        assertThat(exception.getCause()).isInstanceOf(IOException.class);

        verify(file).getInputStream();
    }

    @Test
    void uploadThumbnail_whenFileIsValid_shouldStoreThumbnailAndReturnPublicUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "thumbnail.webp",
                "image/webp",
                SAMPLE_BYTES
        );

        String result = fileUploadService.uploadThumbnail(file);

        assertThat(result).isNotNull();
        assertThat(result).startsWith(THUMBNAIL_PUBLIC_BASE_PATH);
        assertThat(result).endsWith("-thumbnail.webp");

        Path target = pathFromPublicUrl(result, THUMBNAIL_UPLOAD_DIR, THUMBNAIL_PUBLIC_BASE_PATH);

        assertThat(Files.exists(target)).isTrue();
        assertThat(Files.readAllBytes(target)).isEqualTo(SAMPLE_BYTES);
    }

    @Test
    void uploadThumbnail_whenContentTypeIsInvalid_shouldThrowRuntimeExceptionAndSkipStore() throws IOException {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1L);
        when(file.getContentType()).thenReturn("text/plain");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> fileUploadService.uploadThumbnail(file)
        );

        assertThat(exception.getMessage()).isEqualTo("Invalid image type");

        verify(file, never()).getInputStream();
    }

    @Test
    void uploadAudio_whenFileIsValid_shouldStoreAudioAndReturnPublicUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "audio file.mp3",
                "audio/mpeg",
                SAMPLE_BYTES
        );

        String result = fileUploadService.uploadAudio(file);

        assertThat(result).isNotNull();
        assertThat(result).startsWith(AUDIO_PUBLIC_BASE_PATH);
        assertThat(result).endsWith("-audio_file.mp3");

        Path target = pathFromPublicUrl(result, AUDIO_UPLOAD_DIR, AUDIO_PUBLIC_BASE_PATH);

        assertThat(Files.exists(target)).isTrue();
        assertThat(Files.readAllBytes(target)).isEqualTo(SAMPLE_BYTES);
    }

    @Test
    void uploadAudio_whenContentTypeIsInvalid_shouldThrowRuntimeExceptionAndSkipStore() throws IOException {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1L);
        when(file.getContentType()).thenReturn("audio/ogg");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> fileUploadService.uploadAudio(file)
        );

        assertThat(exception.getMessage()).isEqualTo("Invalid audio type");

        verify(file, never()).getInputStream();
    }

    @Test
    void uploadAvatar_whenFileIsValid_shouldStoreAvatarAndReturnPublicUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpeg",
                "image/jpeg",
                SAMPLE_BYTES
        );

        String result = fileUploadService.uploadAvatar(file);

        assertThat(result).isNotNull();
        assertThat(result).startsWith(AVATAR_PUBLIC_BASE_PATH);
        assertThat(result).endsWith("-avatar.jpeg");

        Path target = pathFromPublicUrl(result, AVATAR_UPLOAD_DIR, AVATAR_PUBLIC_BASE_PATH);

        assertThat(Files.exists(target)).isTrue();
        assertThat(Files.readAllBytes(target)).isEqualTo(SAMPLE_BYTES);
    }

    @Test
    void uploadAvatar_whenFileIsMissing_shouldReturnNull() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[0]
        );

        String result = fileUploadService.uploadAvatar(file);

        assertThat(result).isNull();
    }

    @Test
    void deleteImageByUrl_whenRequestIsNull_shouldReturnWithoutDeleting() {
        fileUploadService.deleteImageByUrl(null);

        assertThat(createdPaths).isEmpty();
    }

    @Test
    void deleteImageByUrl_whenFileUrlIsBlank_shouldReturnWithoutDeleting() {
        FileDeleteRequestDto request = deleteRequest("   ");

        fileUploadService.deleteImageByUrl(request);

        assertThat(createdPaths).isEmpty();
    }

    @Test
    void deleteImageByUrl_whenFileExists_shouldDeleteImage() throws IOException {
        Path target = createStoredFile(IMAGE_UPLOAD_DIR, "image.png", "image-content");
        FileDeleteRequestDto request = deleteRequest(IMAGE_PUBLIC_BASE_PATH + "image.png");

        fileUploadService.deleteImageByUrl(request);

        assertThat(Files.exists(target)).isFalse();
    }

    @Test
    void deleteImageByUrl_whenFileDoesNotExist_shouldNotThrowException() {
        FileDeleteRequestDto request = deleteRequest(IMAGE_PUBLIC_BASE_PATH + "missing-image.png");

        fileUploadService.deleteImageByUrl(request);

        assertThat(createdPaths).isEmpty();
    }

    @Test
    void deleteImageByUrl_whenFileUrlDoesNotStartWithBasePath_shouldThrowRuntimeException() {
        FileDeleteRequestDto request = deleteRequest("/invalid/images/image.png");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> fileUploadService.deleteImageByUrl(request)
        );

        assertThat(exception.getMessage()).isEqualTo("File Url must start with " + IMAGE_PUBLIC_BASE_PATH);
    }

    @Test
    void deleteImageByUrl_whenFileUrlContainsPathTraversal_shouldThrowRuntimeException() {
        FileDeleteRequestDto request = deleteRequest(IMAGE_PUBLIC_BASE_PATH + "../image.png");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> fileUploadService.deleteImageByUrl(request)
        );

        assertThat(exception.getMessage()).isEqualTo("Invalid File Url");
    }

    @Test
    void deleteThumbnailByUrl_whenFileExists_shouldDeleteThumbnail() throws IOException {
        Path target = createStoredFile(THUMBNAIL_UPLOAD_DIR, "thumbnail.png", "thumbnail-content");
        FileDeleteRequestDto request = deleteRequest(THUMBNAIL_PUBLIC_BASE_PATH + "thumbnail.png");

        fileUploadService.deleteThumbnailByUrl(request);

        assertThat(Files.exists(target)).isFalse();
    }

    @Test
    void deleteThumbnailByUrl_whenFileUrlIsBlank_shouldReturnWithoutDeleting() {
        FileDeleteRequestDto request = deleteRequest(null);

        fileUploadService.deleteThumbnailByUrl(request);

        assertThat(createdPaths).isEmpty();
    }

    @Test
    void deleteAudioByUrl_whenFileExists_shouldDeleteAudio() throws IOException {
        Path target = createStoredFile(AUDIO_UPLOAD_DIR, "audio.mp3", "audio-content");
        FileDeleteRequestDto request = deleteRequest(AUDIO_PUBLIC_BASE_PATH + "audio.mp3");

        fileUploadService.deleteAudioByUrl(request);

        assertThat(Files.exists(target)).isFalse();
    }

    @Test
    void deleteAudioByUrl_whenDeleteFails_shouldThrowRuntimeException() throws IOException {
        createNonEmptyDirectory(AUDIO_UPLOAD_DIR, "blocked-audio.mp3");
        FileDeleteRequestDto request = deleteRequest(AUDIO_PUBLIC_BASE_PATH + "blocked-audio.mp3");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> fileUploadService.deleteAudioByUrl(request)
        );

        assertThat(exception.getMessage()).isEqualTo("Failed to delete audio file");
        assertThat(exception.getCause()).isInstanceOf(IOException.class);
    }

    @Test
    void deleteAvatarByUrl_whenFileExists_shouldDeleteAvatar() throws IOException {
        Path target = createStoredFile(AVATAR_UPLOAD_DIR, "avatar.png", "avatar-content");
        FileDeleteRequestDto request = deleteRequest(AVATAR_PUBLIC_BASE_PATH + "avatar.png");

        fileUploadService.deleteAvatarByUrl(request);

        assertThat(Files.exists(target)).isFalse();
    }

    @Test
    void deleteAvatarByUrl_whenFileUrlDoesNotStartWithBasePath_shouldThrowRuntimeException() {
        FileDeleteRequestDto request = deleteRequest("/invalid/avatar.png");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> fileUploadService.deleteAvatarByUrl(request)
        );

        assertThat(exception.getMessage()).isEqualTo("File Url must start with " + AVATAR_PUBLIC_BASE_PATH);
    }

    private FileDeleteRequestDto deleteRequest(String fileUrl) {
        FileDeleteRequestDto request = new FileDeleteRequestDto();

        request.setFileUrl(fileUrl);

        return request;
    }

    private Path pathFromPublicUrl(String publicUrl, String uploadDir, String publicBasePath) {
        String filename = publicUrl.substring(publicBasePath.length());
        Path target = Paths.get(uploadDir).resolve(filename).toAbsolutePath().normalize();

        createdPaths.add(target);

        return target;
    }

    private Path createStoredFile(String uploadDir, String filename, String content) throws IOException {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        Path target = uploadPath.resolve(filename).normalize();
        Files.writeString(target, content);

        createdPaths.add(target);

        return target;
    }

    private Path createNonEmptyDirectory(String uploadDir, String filename) throws IOException {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        Path directory = uploadPath.resolve(filename).normalize();
        Files.createDirectories(directory);

        Path child = directory.resolve("child.txt").normalize();
        Files.writeString(child, "blocked");

        createdPaths.add(child);
        createdPaths.add(directory);

        return directory;
    }
}
