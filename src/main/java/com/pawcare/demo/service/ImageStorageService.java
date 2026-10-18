package com.pawcare.demo.service;
import com.pawcare.demo.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.nio.file.*;
import java.io.*;
import java.util.*;
@Service
public class ImageStorageService {
    private final Path root;
    public ImageStorageService(@Value("${app.upload.dir:uploads}") String dir) { root = Path.of(dir).toAbsolutePath().normalize(); }
    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > 5 * 1024 * 1024) throw new BadRequestException("Image must be 5 MB or smaller");
        try (ImageInputStream stream = ImageIO.createImageInputStream(file.getInputStream())) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new BadRequestException("Choose a valid JPEG or PNG image");
            var reader = readers.next();
            try {
                reader.setInput(stream);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!Set.of("jpeg","jpg","png").contains(format)) throw new BadRequestException("Only JPEG and PNG images are supported");
                int w=reader.getWidth(0), h=reader.getHeight(0);
                if (w < 1 || h < 1 || (long)w*h > 16000000) throw new BadRequestException("Image must be at most 16 megapixels");
                var image = reader.read(0);
                Files.createDirectories(root);
                String name = UUID.randomUUID() + ".png";
                Path path = root.resolve(name);
                try { ImageIO.write(image, "png", path.toFile()); }
                catch (IOException ex) { Files.deleteIfExists(path); throw ex; }
                return "/uploads/" + name;
            } finally { reader.dispose(); }
        } catch (IOException ex) { throw new BadRequestException("Image could not be read or stored. Choose another image and try again."); }
    }
    public void remove(String url) {
        if (url == null || !url.matches("/uploads/[a-f0-9-]+\\.png")) return;
        try { Files.deleteIfExists(root.resolve(url.substring("/uploads/".length()))); }
        catch (IOException ignored) { }
    }
}

