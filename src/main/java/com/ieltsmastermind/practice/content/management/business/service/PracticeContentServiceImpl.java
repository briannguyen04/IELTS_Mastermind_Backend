package com.ieltsmastermind.practice.content.management.business.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltsmastermind.common.json.JsonConverter;
import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.content.management.business.interfaces.FileUploadService;
import com.ieltsmastermind.practice.content.management.business.interfaces.PracticeContentService;
import com.ieltsmastermind.practice.content.management.business.parser.InstructionParser;
import com.ieltsmastermind.practice.content.management.domain.dto.*;
import com.ieltsmastermind.practice.content.management.domain.entity.ListeningPracticeContent;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeContent;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeQuestion;
import com.ieltsmastermind.practice.content.management.domain.entity.ReadingPracticeContent;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.content.management.domain.model.doc.DocNode;
import com.ieltsmastermind.practice.content.management.persistence.PracticeContentRepository;
import com.ieltsmastermind.practice.content.management.persistence.PracticeQuestionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
public class PracticeContentServiceImpl implements PracticeContentService {

    private final PracticeContentRepository practiceContentRepository;
    private final InstructionParser instructionParser;
    private final JsonConverter jsonConverter;
    private final FileUploadService fileUploadService;

    public PracticeContentServiceImpl(PracticeContentRepository practiceContentRepository,
                                      FileUploadService fileUploadService,
                                      InstructionParser instructionParser,
                                      JsonConverter jsonConverter) {
        this.practiceContentRepository = practiceContentRepository;
        this.instructionParser = instructionParser;
        this.jsonConverter = jsonConverter;
        this.fileUploadService = fileUploadService;
    }

    @Override
    @Transactional
    public PracticeContentResponseDto create(PracticeContentCreateRequestDto request) {

        PracticeContent content;
        if (request.getSkill() == PracticeContentSkill.LISTENING) {
            ListeningPracticeContent listening = new ListeningPracticeContent();
            listening.setAudioUrl(request.getAudioUrl());

            listening.setTranscript(request.getTranscript());
            List<DocNode> transcriptNodes = instructionParser.parseInstruction(request.getTranscript());
            JsonNode transcriptParsedJson = jsonConverter.toJsonNode(transcriptNodes);
            listening.setTranscriptParsed(transcriptParsedJson);

            content = listening;

        } else if (request.getSkill() == PracticeContentSkill.READING) {
            ReadingPracticeContent reading = new ReadingPracticeContent();

            reading.setPassage(request.getPassage());
            List<DocNode> passageNodes = instructionParser.parseInstruction(request.getPassage());
            JsonNode passageParsedJson = jsonConverter.toJsonNode(passageNodes);
            reading.setPassageParsed(passageParsedJson);

            content = reading;
        } else if (request.getSkill() == PracticeContentSkill.WRITING || request.getSkill() == PracticeContentSkill.SPEAKING){
            content = new PracticeContent();
            content.setSkill(request.getSkill());
        } else {
            throw new IllegalArgumentException("Unsupported skill: " + request.getSkill());
        }

        content.setTitle(request.getTitle());
        content.setTask(request.getTask());

        content.setInstructions(request.getInstructions());
        List<DocNode> parsed = instructionParser.parseInstruction(request.getInstructions());
        JsonNode parsedJson = jsonConverter.toJsonNode(parsed);
        content.setInstructionsParsed(parsedJson);

        content.setQuestionTypeTags(
                request.getQuestionTypeTags() != null
                        ? new HashSet<>(request.getQuestionTypeTags())
                        : new HashSet<>()
        );
        content.setTopicTags(
                request.getTopicTags() != null
                        ? new HashSet<>(request.getTopicTags())
                        : new HashSet<>()
        );

        content.setThumbnailUrl(request.getThumbnailUrl());

        if (request.getImageUrls() != null) {
            content.setImageUrls(new ArrayList<>(request.getImageUrls()));
        }

        content.setDurationMinutes(request.getDurationMinutes());
        content.setQuestionCount(request.getQuestionCount());

        LocalDateTime now = LocalDateTime.now();
        content.setCreatedOn(now);
        content.setUpdatedOn(now);
        content.setStatus(request.getStatus());

        PracticeContent savedContent = practiceContentRepository.save(content);

        PracticeContentResponseDto responseDto = new PracticeContentResponseDto();
        responseDto.setId(savedContent.getId());
        return responseDto;
    }

    @Override
    @Transactional
    public List<PracticeContentResponseDto> getAll(IncludeSpec includes) {
        List<PracticeContent> contents = practiceContentRepository.findAll();
        List<PracticeContentResponseDto> result = new ArrayList<>();

        for (PracticeContent content : contents) {
            PracticeContentResponseDto dto = new PracticeContentResponseDto();
            dto.setId(content.getId());
            applyIncludes(content, dto, includes);

            result.add(dto);
        }

        return result;
    }

    @Override
    @Transactional
    public PracticeContentResponseDto getById(String id, IncludeSpec includes) {
        PracticeContent content = practiceContentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Practice content not found with id: " + id));

        PracticeContentResponseDto dto = new PracticeContentResponseDto();
        dto.setId(content.getId());
        applyIncludes(content, dto, includes);

        return dto;
    }

    @Override
    @Transactional
    public PracticeContentResponseDto update(String id, PracticeContentUpdateRequestDto request) {

        PracticeContent content = practiceContentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Practice content not found with id: " + id));

        if (request.getSkill() != null && request.getSkill() != content.getSkill()) {
            throw new IllegalArgumentException("Cannot change skill of existing PracticeContent (id=" + id + ")");
        }

        if (request.getTitle() != null) content.setTitle(request.getTitle());
        if (request.getTask() != null) content.setTask(request.getTask());

        if (request.getInstructions() != null) {
            content.setInstructions(request.getInstructions());
            List<DocNode> parsed = instructionParser.parseInstruction(request.getInstructions());
            JsonNode parsedJson = jsonConverter.toJsonNode(parsed);
            content.setInstructionsParsed(parsedJson);
        }

        if (request.getQuestionTypeTags() != null) {
            content.setQuestionTypeTags(new HashSet<>(request.getQuestionTypeTags()));
        }
        if (request.getTopicTags() != null) {
            content.setTopicTags(new HashSet<>(request.getTopicTags()));
        }
        if (request.getImageUrls() != null) {

            content.setImageUrls(new ArrayList<>(request.getImageUrls()));
        }

        if (request.getDurationMinutes() != null) content.setDurationMinutes(request.getDurationMinutes());
        if (request.getQuestionCount() != null) content.setQuestionCount(request.getQuestionCount());
        if (request.getStatus() != null) content.setStatus(request.getStatus());

        content.setUpdatedOn(LocalDateTime.now());

        if (content instanceof ListeningPracticeContent listening) {
            if (request.getAudioUrl() != null) {
                listening.setAudioUrl(request.getAudioUrl());
            }

            if (request.getTranscript() != null) {
                listening.setTranscript(request.getTranscript());
                List<DocNode> transcriptNodes = instructionParser.parseInstruction(request.getTranscript());
                JsonNode transcriptParsedJson = jsonConverter.toJsonNode(transcriptNodes);
                listening.setTranscriptParsed(transcriptParsedJson);
            }
        } else if (content instanceof ReadingPracticeContent reading) {
            if (request.getPassage() != null) {
                reading.setPassage(request.getPassage());
                List<DocNode> passageNodes = instructionParser.parseInstruction(request.getPassage());
                JsonNode passageParsedJson = jsonConverter.toJsonNode(passageNodes);
                reading.setPassageParsed(passageParsedJson);
            }
        }

        content.setThumbnailUrl(request.getThumbnailUrl());

        PracticeContent savedContent = practiceContentRepository.save(content);

        PracticeContentResponseDto responseDto = new PracticeContentResponseDto();
        responseDto.setId(savedContent.getId());
        return responseDto;
    }

    @Override
    @Transactional
    public void delete(String id) {

        PracticeContent content = practiceContentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Practice content not found with id: " + id));

        // Delete audio if listening
        if (content instanceof ListeningPracticeContent listening
                && listening.getAudioUrl() != null) {

            FileDeleteRequestDto dto = new FileDeleteRequestDto();
            dto.setFileUrl(listening.getAudioUrl());
            fileUploadService.deleteAudioByUrl(dto);
        }

        // Delete thumbnail
        if (content.getThumbnailUrl() != null) {

            FileDeleteRequestDto dto = new FileDeleteRequestDto();
            dto.setFileUrl(content.getThumbnailUrl());
            fileUploadService.deleteThumbnailByUrl(dto);
        }
        // Delete supporting images
        if (content.getImageUrls() != null) {
            for (String url : content.getImageUrls()) {
                FileDeleteRequestDto dto = new FileDeleteRequestDto();
                dto.setFileUrl(url);
                fileUploadService.deleteImageByUrl(dto);
            }
        }

        new ArrayList<>(content.getSubmissions())
                .forEach(submission -> {
                    submission.setPracticeContentId(null);
                    submission.setPracticeContent(null);
                });

        practiceContentRepository.flush();

        practiceContentRepository.delete(content);
    }

    private void applyIncludes(PracticeContent content, PracticeContentResponseDto dto, IncludeSpec includes) {
        if (includes.has("skill")) dto.setSkill(content.getSkill());
        if (includes.has("title")) dto.setTitle(content.getTitle());
        if (includes.has("instructions")) dto.setInstructions(content.getInstructions());
        if (includes.has("instructionsparsed")) dto.setInstructionsParsed(content.getInstructionsParsed());
        if (includes.has("task")) dto.setTask(content.getTask());
        if (includes.has("questiontypetags")) dto.setQuestionTypeTags(content.getQuestionTypeTags());
        if (includes.has("topictags")) dto.setTopicTags(content.getTopicTags());
        if (includes.has("thumbnailurl")) dto.setThumbnailUrl(content.getThumbnailUrl());
        if (includes.has("durationminutes")) dto.setDurationMinutes(content.getDurationMinutes());
        if (includes.has("questioncount")) dto.setQuestionCount(content.getQuestionCount());
        if (includes.has("createdon")) dto.setCreatedOn(content.getCreatedOn());
        if (includes.has("updatedon")) dto.setUpdatedOn(content.getUpdatedOn());
        if (includes.has("status")) dto.setStatus(content.getStatus());
        if (includes.has("attemptcount")) dto.setAttemptCount(content.getAttemptCount());
        if (includes.has("imageurls")) dto.setImageUrls(content.getImageUrls());

        if (content instanceof ListeningPracticeContent listening) {
            if (includes.has("audiourl")) dto.setAudioUrl(listening.getAudioUrl());
            if (includes.has("transcript")) dto.setTranscript(listening.getTranscript());
            if (includes.has("transcriptparsed")) dto.setTranscriptParsed(listening.getTranscriptParsed());
        } else if (content instanceof ReadingPracticeContent reading) {
            if (includes.has("passage")) dto.setPassage(reading.getPassage());
            if (includes.has("passageparsed")) dto.setPassageParsed(reading.getPassageParsed());
        }
    }
}
