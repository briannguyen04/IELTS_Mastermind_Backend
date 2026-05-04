package com.ieltsmastermind.practice.content.management.business.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltsmastermind.common.json.JsonConverter;
import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.content.management.business.interfaces.FileUploadService;
import com.ieltsmastermind.practice.content.management.business.parser.InstructionParser;
import com.ieltsmastermind.practice.content.management.domain.dto.FileDeleteRequestDto;
import com.ieltsmastermind.practice.content.management.domain.dto.PracticeContentCreateRequestDto;
import com.ieltsmastermind.practice.content.management.domain.dto.PracticeContentResponseDto;
import com.ieltsmastermind.practice.content.management.domain.dto.PracticeContentUpdateRequestDto;
import com.ieltsmastermind.practice.content.management.domain.entity.ListeningPracticeContent;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeContent;
import com.ieltsmastermind.practice.content.management.domain.entity.ReadingPracticeContent;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentStatus;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTaskType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.content.management.domain.model.doc.DocNode;
import com.ieltsmastermind.practice.content.management.persistence.PracticeContentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PracticeContentServiceImplTest {

    @Mock
    private PracticeContentRepository practiceContentRepository;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private InstructionParser instructionParser;

    @Mock
    private JsonConverter jsonConverter;

    @Mock
    private IncludeSpec includes;

    @Mock
    private JsonNode parsedJson;

    @InjectMocks
    private PracticeContentServiceImpl practiceContentService;

    @Test
    void create_whenListeningRequestIsValid_shouldSaveListeningContentAndReturnContentId() {
        PracticeContentCreateRequestDto request = listeningCreateRequest();

        stubParserToReturn(parsedJson);
        when(practiceContentRepository.save(any(PracticeContent.class))).thenAnswer(invocation -> {
            PracticeContent content = invocation.getArgument(0);
            setIfPresent(content, "setId", "content-1");
            return content;
        });

        PracticeContentResponseDto result = practiceContentService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("content-1");

        ArgumentCaptor<PracticeContent> contentCaptor = ArgumentCaptor.forClass(PracticeContent.class);
        verify(practiceContentRepository).save(contentCaptor.capture());

        PracticeContent savedContent = contentCaptor.getValue();

        assertThat(savedContent).isInstanceOf(ListeningPracticeContent.class);
        assertThat(savedContent.getTitle()).isEqualTo("Listening Practice");
        assertThat(savedContent.getTask()).isEqualTo(PracticeTaskType.TASK_1);
        assertThat(savedContent.getInstructions()).isEqualTo("General listening instructions");
        assertThat(savedContent.getInstructionsParsed()).isSameAs(parsedJson);
        assertThat(savedContent.getQuestionTypeTags())
                .containsExactlyInAnyOrder(PracticeQuestionType.MULTIPLE_CHOICE, PracticeQuestionType.MATCHING);
        assertThat(savedContent.getTopicTags())
                .containsExactlyInAnyOrder(PracticeTopicTag.EDUCATION_AND_LEARNING, PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI);
        assertThat(savedContent.getThumbnailUrl()).isEqualTo("thumbnail.png");
        assertThat(savedContent.getImageUrls()).containsExactly("image-1.png", "image-2.png");
        assertThat(savedContent.getDurationMinutes()).isEqualTo(30);
        assertThat(savedContent.getQuestionCount()).isEqualTo(10);
        assertThat(savedContent.getCreatedOn()).isNotNull();
        assertThat(savedContent.getUpdatedOn()).isNotNull();
        assertThat(savedContent.getStatus()).isEqualTo(PracticeContentStatus.PUBLISHED);

        ListeningPracticeContent savedListening = (ListeningPracticeContent) savedContent;
        assertThat(savedListening.getAudioUrl()).isEqualTo("audio.mp3");
        assertThat(savedListening.getTranscript()).isEqualTo("Listening transcript");
        assertThat(savedListening.getTranscriptParsed()).isSameAs(parsedJson);

        verify(instructionParser).parseInstruction("Listening transcript");
        verify(instructionParser).parseInstruction("General listening instructions");
        verify(jsonConverter, times(2)).toJsonNode(anyList());
    }

    @Test
    void create_whenReadingRequestIsValid_shouldSaveReadingContentAndReturnContentId() {
        PracticeContentCreateRequestDto request = readingCreateRequest();

        stubParserToReturn(parsedJson);
        when(practiceContentRepository.save(any(PracticeContent.class))).thenAnswer(invocation -> {
            PracticeContent content = invocation.getArgument(0);
            setIfPresent(content, "setId", "content-1");
            return content;
        });

        PracticeContentResponseDto result = practiceContentService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("content-1");

        ArgumentCaptor<PracticeContent> contentCaptor = ArgumentCaptor.forClass(PracticeContent.class);
        verify(practiceContentRepository).save(contentCaptor.capture());

        PracticeContent savedContent = contentCaptor.getValue();

        assertThat(savedContent).isInstanceOf(ReadingPracticeContent.class);
        assertThat(savedContent.getTitle()).isEqualTo("Reading Practice");
        assertThat(savedContent.getTask()).isEqualTo(PracticeTaskType.ALL);
        assertThat(savedContent.getInstructions()).isEqualTo("General reading instructions");
        assertThat(savedContent.getInstructionsParsed()).isSameAs(parsedJson);
        assertThat(savedContent.getThumbnailUrl()).isEqualTo("thumbnail.png");
        assertThat(savedContent.getDurationMinutes()).isEqualTo(60);
        assertThat(savedContent.getQuestionCount()).isEqualTo(14);
        assertThat(savedContent.getStatus()).isEqualTo(PracticeContentStatus.DRAFT);

        ReadingPracticeContent savedReading = (ReadingPracticeContent) savedContent;
        assertThat(savedReading.getPassage()).isEqualTo("Reading passage");
        assertThat(savedReading.getPassageParsed()).isSameAs(parsedJson);

        verify(instructionParser).parseInstruction("Reading passage");
        verify(instructionParser).parseInstruction("General reading instructions");
        verify(jsonConverter, times(2)).toJsonNode(anyList());
    }

    @Test
    void create_whenWritingRequestIsValid_shouldSaveBasePracticeContentWithSkillAndReturnContentId() {
        PracticeContentCreateRequestDto request = writingCreateRequest();

        stubParserToReturn(parsedJson);
        when(practiceContentRepository.save(any(PracticeContent.class))).thenAnswer(invocation -> {
            PracticeContent content = invocation.getArgument(0);
            setIfPresent(content, "setId", "content-1");
            return content;
        });

        PracticeContentResponseDto result = practiceContentService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("content-1");

        ArgumentCaptor<PracticeContent> contentCaptor = ArgumentCaptor.forClass(PracticeContent.class);
        verify(practiceContentRepository).save(contentCaptor.capture());

        PracticeContent savedContent = contentCaptor.getValue();

        assertThat(savedContent).isNotInstanceOf(ListeningPracticeContent.class);
        assertThat(savedContent).isNotInstanceOf(ReadingPracticeContent.class);
        assertThat(savedContent.getSkill()).isEqualTo(PracticeContentSkill.WRITING);
        assertThat(savedContent.getTitle()).isEqualTo("Writing Practice");
        assertThat(savedContent.getInstructions()).isEqualTo("Writing instructions");
        assertThat(savedContent.getInstructionsParsed()).isSameAs(parsedJson);

        verify(instructionParser).parseInstruction("Writing instructions");
        verify(jsonConverter).toJsonNode(anyList());
    }

    @Test
    void create_whenTagsAndImageUrlsAreNull_shouldUseEmptyTagSetsAndKeepImageUrlsNullOrEmpty() {
        PracticeContentCreateRequestDto request = speakingCreateRequest();
        request.setQuestionTypeTags(null);
        request.setTopicTags(null);
        request.setImageUrls(null);

        stubParserToReturn(parsedJson);
        when(practiceContentRepository.save(any(PracticeContent.class))).thenAnswer(invocation -> {
            PracticeContent content = invocation.getArgument(0);
            setIfPresent(content, "setId", "content-1");
            return content;
        });

        PracticeContentResponseDto result = practiceContentService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("content-1");

        ArgumentCaptor<PracticeContent> contentCaptor = ArgumentCaptor.forClass(PracticeContent.class);
        verify(practiceContentRepository).save(contentCaptor.capture());

        PracticeContent savedContent = contentCaptor.getValue();

        assertThat(savedContent.getSkill()).isEqualTo(PracticeContentSkill.SPEAKING);
        assertThat(savedContent.getQuestionTypeTags()).isEmpty();
        assertThat(savedContent.getTopicTags()).isEmpty();
        assertThat(savedContent.getImageUrls()).isNullOrEmpty();

        verify(practiceContentRepository).save(any(PracticeContent.class));
    }

    @Test
    void create_whenSkillIsNull_shouldThrowIllegalArgumentExceptionAndSkipSave() {
        PracticeContentCreateRequestDto request = writingCreateRequest();
        request.setSkill(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> practiceContentService.create(request)
        );

        assertThat(exception.getMessage()).isEqualTo("Unsupported skill: null");

        verify(practiceContentRepository, never()).save(any(PracticeContent.class));
        verify(instructionParser, never()).parseInstruction(anyString());
        verify(jsonConverter, never()).toJsonNode(any());
    }

    @Test
    void getAll_whenContentsExistAndFieldsAreIncluded_shouldReturnDtosWithIncludedFields() {
        ListeningPracticeContent listening = listeningContent("content-1");
        ReadingPracticeContent reading = readingContent("content-2");

        when(practiceContentRepository.findAll()).thenReturn(List.of(listening, reading));
        mockIncludes(
                "skill",
                "title",
                "instructions",
                "instructionsparsed",
                "task",
                "questiontypetags",
                "topictags",
                "thumbnailurl",
                "durationminutes",
                "questioncount",
                "createdon",
                "updatedon",
                "status",
                "imageurls",
                "audiourl",
                "transcript",
                "transcriptparsed",
                "passage",
                "passageparsed"
        );

        List<PracticeContentResponseDto> result = practiceContentService.getAll(includes);

        assertThat(result).hasSize(2);

        PracticeContentResponseDto first = result.get(0);
        assertThat(first.getId()).isEqualTo("content-1");
        assertThat(first.getSkill()).isEqualTo(PracticeContentSkill.LISTENING);
        assertThat(first.getTitle()).isEqualTo("Listening Practice");
        assertThat(first.getInstructions()).isEqualTo("General instructions");
        assertThat(first.getInstructionsParsed()).isSameAs(parsedJson);
        assertThat(first.getTask()).isEqualTo(PracticeTaskType.TASK_1);
        assertThat(first.getQuestionTypeTags()).containsExactlyInAnyOrder(PracticeQuestionType.MULTIPLE_CHOICE);
        assertThat(first.getTopicTags()).containsExactlyInAnyOrder(PracticeTopicTag.EDUCATION_AND_LEARNING);
        assertThat(first.getThumbnailUrl()).isEqualTo("thumbnail.png");
        assertThat(first.getDurationMinutes()).isEqualTo(30);
        assertThat(first.getQuestionCount()).isEqualTo(10);
        assertThat(first.getCreatedOn()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
        assertThat(first.getUpdatedOn()).isEqualTo(LocalDateTime.of(2026, 1, 2, 10, 0));
        assertThat(first.getStatus()).isEqualTo(PracticeContentStatus.PUBLISHED);
        assertThat(first.getImageUrls()).containsExactly("image-1.png", "image-2.png");
        assertThat(first.getAudioUrl()).isEqualTo("audio.mp3");
        assertThat(first.getTranscript()).isEqualTo("Listening transcript");
        assertThat(first.getTranscriptParsed()).isSameAs(parsedJson);

        PracticeContentResponseDto second = result.get(1);
        assertThat(second.getId()).isEqualTo("content-2");
        assertThat(second.getSkill()).isEqualTo(PracticeContentSkill.READING);
        assertThat(second.getTitle()).isEqualTo("Reading Practice");
        assertThat(second.getPassage()).isEqualTo("Reading passage");
        assertThat(second.getPassageParsed()).isSameAs(parsedJson);
        assertThat(second.getAudioUrl()).isNull();

        verify(practiceContentRepository).findAll();
    }

    @Test
    void getAll_whenRepositoryReturnsEmptyList_shouldReturnEmptyList() {
        when(practiceContentRepository.findAll()).thenReturn(List.of());

        List<PracticeContentResponseDto> result = practiceContentService.getAll(includes);

        assertThat(result).isEmpty();

        verify(practiceContentRepository).findAll();
    }

    @Test
    void getById_whenContentExistsAndNoFieldsAreIncluded_shouldReturnOnlyContentId() {
        PracticeContent content = baseContent("content-1", PracticeContentSkill.WRITING);

        when(practiceContentRepository.findById("content-1")).thenReturn(Optional.of(content));
        mockIncludes();

        PracticeContentResponseDto result = practiceContentService.getById("content-1", includes);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("content-1");
        assertThat(result.getSkill()).isNull();
        assertThat(result.getTitle()).isNull();
        assertThat(result.getInstructions()).isNull();
        assertThat(result.getThumbnailUrl()).isNull();

        verify(practiceContentRepository).findById("content-1");
    }

    @Test
    void getById_whenContentDoesNotExist_shouldThrowRuntimeException() {
        when(practiceContentRepository.findById("missing-content")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> practiceContentService.getById("missing-content", includes)
        );

        assertThat(exception.getMessage()).isEqualTo("Practice content not found with id: missing-content");

        verify(practiceContentRepository).findById("missing-content");
    }

    @Test
    void update_whenBaseFieldsAreProvided_shouldUpdateFieldsSaveAndReturnContentId() {
        PracticeContent existingContent = baseContent("content-1", PracticeContentSkill.WRITING);
        PracticeContentUpdateRequestDto request = fullUpdateRequest(PracticeContentSkill.WRITING);

        stubParserToReturn(parsedJson);
        when(practiceContentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));
        when(practiceContentRepository.save(any(PracticeContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PracticeContentResponseDto result = practiceContentService.update("content-1", request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("content-1");

        ArgumentCaptor<PracticeContent> contentCaptor = ArgumentCaptor.forClass(PracticeContent.class);
        verify(practiceContentRepository).save(contentCaptor.capture());

        PracticeContent savedContent = contentCaptor.getValue();

        assertThat(savedContent.getSkill()).isEqualTo(PracticeContentSkill.WRITING);
        assertThat(savedContent.getTitle()).isEqualTo("Updated Title");
        assertThat(savedContent.getTask()).isEqualTo(PracticeTaskType.TASK_2);
        assertThat(savedContent.getInstructions()).isEqualTo("Updated instructions");
        assertThat(savedContent.getInstructionsParsed()).isSameAs(parsedJson);
        assertThat(savedContent.getQuestionTypeTags()).containsExactlyInAnyOrder(PracticeQuestionType.OPINION);
        assertThat(savedContent.getTopicTags()).containsExactlyInAnyOrder(PracticeTopicTag.WORK_JOBS_AND_CAREERS);
        assertThat(savedContent.getImageUrls()).containsExactly("updated-image.png");
        assertThat(savedContent.getDurationMinutes()).isEqualTo(45);
        assertThat(savedContent.getQuestionCount()).isEqualTo(2);
        assertThat(savedContent.getStatus()).isEqualTo(PracticeContentStatus.DRAFT);
        assertThat(savedContent.getThumbnailUrl()).isEqualTo("updated-thumbnail.png");
        assertThat(savedContent.getUpdatedOn()).isNotEqualTo(LocalDateTime.of(2026, 1, 2, 10, 0));

        verify(practiceContentRepository).findById("content-1");
        verify(instructionParser).parseInstruction("Updated instructions");
        verify(jsonConverter).toJsonNode(anyList());
    }

    @Test
    void update_whenRequestContainsOnlyNullFields_shouldKeepExistingValuesClearThumbnailAndStillSave() {
        PracticeContent existingContent = baseContent("content-1", PracticeContentSkill.WRITING);
        existingContent.setThumbnailUrl("old-thumbnail.png");

        PracticeContentUpdateRequestDto request = new PracticeContentUpdateRequestDto();

        when(practiceContentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));
        when(practiceContentRepository.save(any(PracticeContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PracticeContentResponseDto result = practiceContentService.update("content-1", request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("content-1");
        assertThat(existingContent.getTitle()).isEqualTo("Writing Practice");
        assertThat(existingContent.getTask()).isEqualTo(PracticeTaskType.TASK_1);
        assertThat(existingContent.getInstructions()).isEqualTo("General instructions");
        assertThat(existingContent.getThumbnailUrl()).isNull();

        verify(instructionParser, never()).parseInstruction(anyString());
        verify(jsonConverter, never()).toJsonNode(any());
        verify(practiceContentRepository).save(existingContent);
    }

    @Test
    void update_whenSkillChangeRequested_shouldThrowIllegalArgumentExceptionAndSkipSave() {
        PracticeContent existingContent = baseContent("content-1", PracticeContentSkill.WRITING);

        PracticeContentUpdateRequestDto request = new PracticeContentUpdateRequestDto();
        request.setSkill(PracticeContentSkill.READING);

        when(practiceContentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> practiceContentService.update("content-1", request)
        );

        assertThat(exception.getMessage())
                .isEqualTo("Cannot change skill of existing PracticeContent (id=content-1)");

        verify(practiceContentRepository).findById("content-1");
        verify(practiceContentRepository, never()).save(any(PracticeContent.class));
        verify(instructionParser, never()).parseInstruction(anyString());
    }

    @Test
    void update_whenContentDoesNotExist_shouldThrowRuntimeException() {
        PracticeContentUpdateRequestDto request = fullUpdateRequest(PracticeContentSkill.WRITING);

        when(practiceContentRepository.findById("missing-content")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> practiceContentService.update("missing-content", request)
        );

        assertThat(exception.getMessage()).isEqualTo("Practice content not found with id: missing-content");

        verify(practiceContentRepository).findById("missing-content");
        verify(practiceContentRepository, never()).save(any(PracticeContent.class));
    }

    @Test
    void update_whenListeningContentReceivesAudioAndTranscript_shouldUpdateListeningFieldsAndParsedTranscript() {
        ListeningPracticeContent existingContent = listeningContent("content-1");

        PracticeContentUpdateRequestDto request = new PracticeContentUpdateRequestDto();
        request.setSkill(PracticeContentSkill.LISTENING);
        request.setAudioUrl("updated-audio.mp3");
        request.setTranscript("Updated transcript");

        stubParserToReturn(parsedJson);
        when(practiceContentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));
        when(practiceContentRepository.save(any(PracticeContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PracticeContentResponseDto result = practiceContentService.update("content-1", request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("content-1");
        assertThat(existingContent.getAudioUrl()).isEqualTo("updated-audio.mp3");
        assertThat(existingContent.getTranscript()).isEqualTo("Updated transcript");
        assertThat(existingContent.getTranscriptParsed()).isSameAs(parsedJson);

        verify(instructionParser).parseInstruction("Updated transcript");
        verify(jsonConverter).toJsonNode(anyList());
        verify(practiceContentRepository).save(existingContent);
    }

    @Test
    void update_whenReadingContentReceivesPassage_shouldUpdatePassageAndParsedPassage() {
        ReadingPracticeContent existingContent = readingContent("content-1");

        PracticeContentUpdateRequestDto request = new PracticeContentUpdateRequestDto();
        request.setSkill(PracticeContentSkill.READING);
        request.setPassage("Updated passage");

        stubParserToReturn(parsedJson);
        when(practiceContentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));
        when(practiceContentRepository.save(any(PracticeContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PracticeContentResponseDto result = practiceContentService.update("content-1", request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("content-1");
        assertThat(existingContent.getPassage()).isEqualTo("Updated passage");
        assertThat(existingContent.getPassageParsed()).isSameAs(parsedJson);

        verify(instructionParser).parseInstruction("Updated passage");
        verify(jsonConverter).toJsonNode(anyList());
        verify(practiceContentRepository).save(existingContent);
    }

    @Test
    void delete_whenListeningContentHasFilesAndSubmissions_shouldDeleteFilesClearSubmissionFlushAndDelete() {
        ListeningPracticeContent content = listeningContent("content-1");
        content.setAudioUrl("audio.mp3");
        content.setThumbnailUrl("thumbnail.png");
        content.setImageUrls(new ArrayList<>(List.of("image-1.png", "image-2.png")));

        Object submission = addMockRelation(content, "getSubmissions");

        when(practiceContentRepository.findById("content-1")).thenReturn(Optional.of(content));

        practiceContentService.delete("content-1");

        ArgumentCaptor<FileDeleteRequestDto> audioCaptor = ArgumentCaptor.forClass(FileDeleteRequestDto.class);
        ArgumentCaptor<FileDeleteRequestDto> thumbnailCaptor = ArgumentCaptor.forClass(FileDeleteRequestDto.class);
        ArgumentCaptor<FileDeleteRequestDto> imageCaptor = ArgumentCaptor.forClass(FileDeleteRequestDto.class);

        verify(fileUploadService).deleteAudioByUrl(audioCaptor.capture());
        verify(fileUploadService).deleteThumbnailByUrl(thumbnailCaptor.capture());
        verify(fileUploadService, times(2)).deleteImageByUrl(imageCaptor.capture());

        assertThat(audioCaptor.getValue().getFileUrl()).isEqualTo("audio.mp3");
        assertThat(thumbnailCaptor.getValue().getFileUrl()).isEqualTo("thumbnail.png");
        assertThat(imageCaptor.getAllValues())
                .extracting(FileDeleteRequestDto::getFileUrl)
                .containsExactly("image-1.png", "image-2.png");

        verifySetterCalledWithNull(submission, "setPracticeContentId");
        verifySetterCalledWithNull(submission, "setPracticeContent");

        verify(practiceContentRepository).findById("content-1");
        verify(practiceContentRepository).flush();
        verify(practiceContentRepository).delete(content);
    }

    @Test
    void delete_whenContentHasNoOptionalFiles_shouldSkipFileDeletionFlushAndDelete() {
        PracticeContent content = baseContent("content-1", PracticeContentSkill.WRITING);
        content.setThumbnailUrl(null);
        content.setImageUrls(null);
        ensureEmptyRelationCollection(content, "getSubmissions");

        when(practiceContentRepository.findById("content-1")).thenReturn(Optional.of(content));

        practiceContentService.delete("content-1");

        verify(fileUploadService, never()).deleteAudioByUrl(any(FileDeleteRequestDto.class));
        verify(fileUploadService, never()).deleteThumbnailByUrl(any(FileDeleteRequestDto.class));
        verify(fileUploadService, never()).deleteImageByUrl(any(FileDeleteRequestDto.class));
        verify(practiceContentRepository).flush();
        verify(practiceContentRepository).delete(content);
    }

    @Test
    void delete_whenContentDoesNotExist_shouldThrowRuntimeException() {
        when(practiceContentRepository.findById("missing-content")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> practiceContentService.delete("missing-content")
        );

        assertThat(exception.getMessage()).isEqualTo("Practice content not found with id: missing-content");

        verify(practiceContentRepository).findById("missing-content");
        verify(practiceContentRepository, never()).flush();
        verify(practiceContentRepository, never()).delete(any(PracticeContent.class));
    }

    private PracticeContentCreateRequestDto listeningCreateRequest() {
        PracticeContentCreateRequestDto request = new PracticeContentCreateRequestDto();

        request.setSkill(PracticeContentSkill.LISTENING);
        request.setTitle("Listening Practice");
        request.setInstructions("General listening instructions");
        request.setTask(PracticeTaskType.TASK_1);
        request.setQuestionTypeTags(Set.of(
                PracticeQuestionType.MULTIPLE_CHOICE,
                PracticeQuestionType.MATCHING
        ));
        request.setTopicTags(Set.of(
                PracticeTopicTag.EDUCATION_AND_LEARNING,
                PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI
        ));
        request.setThumbnailUrl("thumbnail.png");
        request.setAudioUrl("audio.mp3");
        request.setTranscript("Listening transcript");
        request.setImageUrls(List.of("image-1.png", "image-2.png"));
        request.setDurationMinutes(30);
        request.setQuestionCount(10);
        request.setStatus(PracticeContentStatus.PUBLISHED);

        return request;
    }

    private PracticeContentCreateRequestDto readingCreateRequest() {
        PracticeContentCreateRequestDto request = new PracticeContentCreateRequestDto();

        request.setSkill(PracticeContentSkill.READING);
        request.setTitle("Reading Practice");
        request.setInstructions("General reading instructions");
        request.setTask(PracticeTaskType.ALL);
        request.setQuestionTypeTags(Set.of(PracticeQuestionType.MATCHING_HEADINGS));
        request.setTopicTags(Set.of(PracticeTopicTag.ENVIRONMENT_CLIMATE_AND_SUSTAINABILITY));
        request.setThumbnailUrl("thumbnail.png");
        request.setPassage("Reading passage");
        request.setDurationMinutes(60);
        request.setQuestionCount(14);
        request.setStatus(PracticeContentStatus.DRAFT);

        return request;
    }

    private PracticeContentCreateRequestDto writingCreateRequest() {
        PracticeContentCreateRequestDto request = new PracticeContentCreateRequestDto();

        request.setSkill(PracticeContentSkill.WRITING);
        request.setTitle("Writing Practice");
        request.setInstructions("Writing instructions");
        request.setTask(PracticeTaskType.TASK_2);
        request.setQuestionTypeTags(Set.of(PracticeQuestionType.OPINION));
        request.setTopicTags(Set.of(PracticeTopicTag.WORK_JOBS_AND_CAREERS));
        request.setThumbnailUrl("writing-thumbnail.png");
        request.setImageUrls(List.of("writing-image.png"));
        request.setDurationMinutes(40);
        request.setQuestionCount(1);
        request.setStatus(PracticeContentStatus.PUBLISHED);

        return request;
    }

    private PracticeContentCreateRequestDto speakingCreateRequest() {
        PracticeContentCreateRequestDto request = new PracticeContentCreateRequestDto();

        request.setSkill(PracticeContentSkill.SPEAKING);
        request.setTitle("Speaking Practice");
        request.setInstructions("Speaking instructions");
        request.setTask(PracticeTaskType.ALL);
        request.setThumbnailUrl("speaking-thumbnail.png");
        request.setDurationMinutes(15);
        request.setQuestionCount(3);
        request.setStatus(PracticeContentStatus.DRAFT);

        return request;
    }

    private PracticeContentUpdateRequestDto fullUpdateRequest(PracticeContentSkill skill) {
        PracticeContentUpdateRequestDto request = new PracticeContentUpdateRequestDto();

        request.setSkill(skill);
        request.setTitle("Updated Title");
        request.setInstructions("Updated instructions");
        request.setTask(PracticeTaskType.TASK_2);
        request.setQuestionTypeTags(Set.of(PracticeQuestionType.OPINION));
        request.setTopicTags(Set.of(PracticeTopicTag.WORK_JOBS_AND_CAREERS));
        request.setThumbnailUrl("updated-thumbnail.png");
        request.setImageUrls(List.of("updated-image.png"));
        request.setDurationMinutes(45);
        request.setQuestionCount(2);
        request.setStatus(PracticeContentStatus.DRAFT);

        return request;
    }

    private PracticeContent baseContent(String id, PracticeContentSkill skill) {
        PracticeContent content = new PracticeContent();

        populateCommonContent(content, id, skill, skill == PracticeContentSkill.READING ? "Reading Practice" : "Writing Practice");

        return content;
    }

    private ListeningPracticeContent listeningContent(String id) {
        ListeningPracticeContent content = new ListeningPracticeContent();

        populateCommonContent(content, id, PracticeContentSkill.LISTENING, "Listening Practice");
        content.setAudioUrl("audio.mp3");
        content.setTranscript("Listening transcript");
        content.setTranscriptParsed(parsedJson);

        return content;
    }

    private ReadingPracticeContent readingContent(String id) {
        ReadingPracticeContent content = new ReadingPracticeContent();

        populateCommonContent(content, id, PracticeContentSkill.READING, "Reading Practice");
        content.setPassage("Reading passage");
        content.setPassageParsed(parsedJson);

        return content;
    }

    private void populateCommonContent(PracticeContent content,
                                       String id,
                                       PracticeContentSkill skill,
                                       String title) {
        setIfPresent(content, "setId", id);

        content.setSkill(skill);
        content.setTitle(title);
        content.setInstructions("General instructions");
        content.setInstructionsParsed(parsedJson);
        content.setTask(PracticeTaskType.TASK_1);
        content.setQuestionTypeTags(new HashSet<>(Set.of(PracticeQuestionType.MULTIPLE_CHOICE)));
        content.setTopicTags(new HashSet<>(Set.of(PracticeTopicTag.EDUCATION_AND_LEARNING)));
        content.setThumbnailUrl("thumbnail.png");
        content.setImageUrls(new ArrayList<>(List.of("image-1.png", "image-2.png")));
        content.setDurationMinutes(30);
        content.setQuestionCount(10);
        content.setCreatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        content.setUpdatedOn(LocalDateTime.of(2026, 1, 2, 10, 0));
        content.setStatus(PracticeContentStatus.PUBLISHED);

        setIfPresent(content, "setAttemptCount", 3L);
        ensureEmptyRelationCollection(content, "getSubmissions");
    }

    private void stubParserToReturn(JsonNode jsonNode) {
        List<DocNode> parsedNodes = new ArrayList<>();

        when(instructionParser.parseInstruction(anyString())).thenReturn(parsedNodes);
        when(jsonConverter.toJsonNode(anyList())).thenReturn(jsonNode);
    }

    private void mockIncludes(String... fieldsToInclude) {
        Set<String> includedFields = Set.copyOf(Arrays.asList(fieldsToInclude));

        when(includes.has(anyString())).thenAnswer(invocation -> {
            String fieldName = invocation.getArgument(0);
            return includedFields.contains(fieldName);
        });
    }

    private void setIfPresent(Object target, String setterName, Object value) {
        Method setter = Arrays.stream(target.getClass().getMethods())
                .filter(method -> method.getName().equals(setterName))
                .filter(method -> method.getParameterCount() == 1)
                .filter(method -> value == null || method.getParameterTypes()[0].isAssignableFrom(value.getClass()))
                .findFirst()
                .orElse(null);

        if (setter == null) {
            return;
        }

        try {
            setter.invoke(target, value);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to call " + setterName, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Object addMockRelation(PracticeContent content, String getterName) {
        try {
            Method getter = PracticeContent.class.getMethod(getterName);
            Object collectionObject = getter.invoke(content);

            if (collectionObject == null) {
                collectionObject = createCollectionForReturnType(getter.getReturnType());
                setCollectionIfPresent(content, getterName.replaceFirst("^get", "set"), collectionObject);
            }

            if (!(collectionObject instanceof Collection<?>)) {
                return null;
            }

            Class<?> relationClass = resolveCollectionItemType(getter);

            if (relationClass == null) {
                return null;
            }

            Object relationMock = mock(relationClass);

            ((Collection<Object>) collectionObject).add(relationMock);

            return relationMock;
        } catch (Exception exception) {
            return null;
        }
    }

    private void ensureEmptyRelationCollection(PracticeContent content, String getterName) {
        try {
            Method getter = PracticeContent.class.getMethod(getterName);
            Object collectionObject = getter.invoke(content);

            if (collectionObject != null) {
                return;
            }

            Object emptyCollection = createCollectionForReturnType(getter.getReturnType());
            setCollectionIfPresent(content, getterName.replaceFirst("^get", "set"), emptyCollection);
        } catch (Exception exception) {
            return;
        }
    }

    private Collection<Object> createCollectionForReturnType(Class<?> returnType) {
        if (Set.class.isAssignableFrom(returnType)) {
            return new HashSet<>();
        }

        return new ArrayList<>();
    }

    private void setCollectionIfPresent(Object target, String setterName, Object collection) {
        Method setter = Arrays.stream(target.getClass().getMethods())
                .filter(method -> method.getName().equals(setterName))
                .filter(method -> method.getParameterCount() == 1)
                .filter(method -> method.getParameterTypes()[0].isAssignableFrom(collection.getClass()))
                .findFirst()
                .orElse(null);

        if (setter == null) {
            return;
        }

        try {
            setter.invoke(target, collection);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to call " + setterName, exception);
        }
    }

    private Class<?> resolveCollectionItemType(Method getter) {
        Type genericReturnType = getter.getGenericReturnType();

        if (!(genericReturnType instanceof ParameterizedType parameterizedType)) {
            return null;
        }

        Type firstArgument = parameterizedType.getActualTypeArguments()[0];

        if (firstArgument instanceof Class<?>) {
            return (Class<?>) firstArgument;
        }

        if (firstArgument instanceof ParameterizedType nestedType
                && nestedType.getRawType() instanceof Class<?>) {
            return (Class<?>) nestedType.getRawType();
        }

        return null;
    }

    private void verifySetterCalledWithNull(Object relationMock, String setterName) {
        if (relationMock == null) {
            return;
        }

        Method setter = Arrays.stream(relationMock.getClass().getMethods())
                .filter(method -> method.getName().equals(setterName))
                .filter(method -> method.getParameterCount() == 1)
                .findFirst()
                .orElse(null);

        if (setter == null) {
            return;
        }

        try {
            setter.invoke(verify(relationMock), new Object[]{null});
        } catch (Exception exception) {
            throw new RuntimeException("Failed to verify " + setterName, exception);
        }
    }
}
