package com.pawcare.demo.service;

import com.pawcare.demo.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class DocumentStorageService {
    private final Path root;
    private static final Set<String> ALLOWED = Set.of("application/pdf", "image/jpeg", "image/png");
    private static final Pattern SAFE_STORED_NAME = Pattern.compile("^[a-f0-9\\-]{36}\\.(pdf|png|jpg)$");

    public DocumentStorageService(@Value("${app.private-upload.dir:private-uploads}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    public StoredFile save(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new BadRequestException("Attachment must be 5 MB or smaller");
        }

        String type = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        if (!ALLOWED.contains(type)) {
            throw new BadRequestException("Only PDF, JPEG and PNG attachments are supported");
        }
        validateSignature(file, type);

        String ext = type.equals("application/pdf") ? ".pdf" : type.equals("image/png") ? ".png" : ".jpg";
        try {
            Files.createDirectories(root);
            String stored = UUID.randomUUID() + ext;
            Path target = root.resolve(stored).normalize();
            if (!target.startsWith(root)) throw new BadRequestException("Invalid attachment path");
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            // Store only the generated private file key. It is never exposed through /uploads/**.
            return new StoredFile(safeOriginalName(file.getOriginalFilename()), stored, type, file.getSize());
        } catch (IOException e) {
            throw new BadRequestException("Attachment could not be stored");
        }
    }

    public byte[] read(String storedName) {
        if (storedName == null || !SAFE_STORED_NAME.matcher(storedName).matches()) {
            throw new BadRequestException("Invalid attachment path");
        }
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) throw new BadRequestException("Invalid attachment path");
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new BadRequestException("Attachment could not be read");
        }
    }

    private void validateSignature(MultipartFile file, String contentType) {
        byte[] header = new byte[8];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.read(header);
        } catch (IOException e) {
            throw new BadRequestException("Attachment could not be validated");
        }

        boolean valid = switch (contentType) {
            case "application/pdf" -> read >= 4 && header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F';
            case "image/png" -> read >= 8
                    && (header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                    && (header[4] & 0xFF) == 0x0D && (header[5] & 0xFF) == 0x0A
                    && (header[6] & 0xFF) == 0x1A && (header[7] & 0xFF) == 0x0A;
            case "image/jpeg" -> read >= 3
                    && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
            default -> false;
        };
        if (!valid) throw new BadRequestException("Attachment content does not match its file type");
    }

    private String safeOriginalName(String original) {
        if (original == null || original.isBlank()) return "attachment";
        String name = Path.of(original).getFileName().toString().replaceAll("[\\r\\n\\\"]", "_");
        return name.length() > 180 ? name.substring(name.length() - 180) : name;
    }

    public record StoredFile(String originalName, String url, String contentType, long size) {}
}
