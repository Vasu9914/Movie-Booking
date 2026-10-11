package com.vasu.movie_service.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.vasu.movie_service.dto.common.PageResponse;
import com.vasu.movie_service.dto.request.MovieCreateRequest;
import com.vasu.movie_service.dto.request.MovieUpdateRequest;
import com.vasu.movie_service.dto.request.StatusChangeRequest;
import com.vasu.movie_service.dto.response.MovieResponse;
import com.vasu.movie_service.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Slf4j
public class MovieController {

    private final MovieService movieService;
    private final Cloudinary cloudinary;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovieResponse createMovie(@Valid @RequestBody MovieCreateRequest request) {
        return movieService.createMovie(request);
    }

    @GetMapping("/{id}")
    public MovieResponse getMovieById(@PathVariable Long id) {
        return movieService.getMovieById(id);
    }

    @GetMapping
    public PageResponse<MovieResponse> getAllMovies(
            @PageableDefault(size = 20) Pageable pageable) {
        Slice<MovieResponse> slice = movieService.getAllMovies(pageable);
        return toPageResponse(slice);
    }

    @GetMapping("/search")
    public PageResponse<MovieResponse> searchMovies(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        Slice<MovieResponse> slice = movieService.searchMovies(keyword, pageable);
        return toPageResponse(slice);
    }

    @PutMapping("/{id}")
    public MovieResponse updateMovie(@PathVariable Long id, @Valid @RequestBody MovieUpdateRequest request) {
        return movieService.updateMovie(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMovieStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
        movieService.updateMovieStatus(id, request);
    }

    @PostMapping("/upload-poster")
    public ResponseEntity<Map<String, Object>> uploadPoster(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            String secureUrl = (String) uploadResult.get("secure_url");

            log.info("Poster uploaded to Cloudinary: {}", secureUrl);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "url", secureUrl,
                    "publicId", uploadResult.get("public_id")
            ));
        } catch (IOException e) {
            log.error("Failed to upload poster to Cloudinary", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload image"));
        } catch (Exception e) {
            log.error("Cloudinary upload error", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Cloudinary upload failed"));
        }
    }

    private PageResponse<MovieResponse> toPageResponse(Slice<MovieResponse> slice) {
        return PageResponse.<MovieResponse>builder()
                .content(slice.getContent())
                .page(slice.getNumber())
                .size(slice.getSize())
                .first(slice.isFirst())
                .last(slice.isLast())
                .build();
    }
}
