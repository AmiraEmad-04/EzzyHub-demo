package com.ezzyhub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class GitHubService {

    private final WebClient webClient;

    @Value("${github.token:}")
    private String githubToken;

    public GitHubService() {
        this.webClient = WebClient.builder()
                .defaultHeader("User-Agent", "EzzyHub-Demo")
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();
    }

    /**
     * Downloads repository from GitHub and extracts Java files
     */
    public List<File> downloadAndExtract(String githubUrl) throws IOException {
        System.out.println("Downloading from: " + githubUrl);

        // Parse GitHub URL - remove .git if present
        String cleanUrl = githubUrl
                .replace("https://github.com/", "")
                .replace("http://github.com/", "")
                .replace(".git", "")
                .trim();

        String[] parts = cleanUrl.split("/");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid GitHub URL format. Expected: https://github.com/owner/repo");
        }

        String owner = parts[0];
        String repo = parts[1];

        System.out.println("Owner: " + owner + ", Repo: " + repo);

        // GitHub API to download repository as ZIP
        String zipUrl = String.format("https://api.github.com/repos/%s/%s/zipball", owner, repo);

        System.out.println("Downloading ZIP from: " + zipUrl);

        try {
            // Build request with authentication if token is available
            WebClient.RequestHeadersSpec<?> request = webClient.get().uri(zipUrl);

            if (githubToken != null && !githubToken.isEmpty()) {
                request = request.header("Authorization", "Bearer " + githubToken);
                System.out.println("Using GitHub token for authentication");
            } else {
                System.out.println("WARNING: No GitHub token provided. May hit rate limits.");
            }

            // Download ZIP file with better error handling
            byte[] zipData = request
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError(),
                            response -> {
                                System.err.println("GitHub API Error: " + response.statusCode());
                                return Mono.error(new IOException(
                                        "Repository not found or access denied (Status: " + response.statusCode() + "). " +
                                                "Make sure:\n" +
                                                "1. The repository is public, OR\n" +
                                                "2. You've added a GitHub token in application.properties\n" +
                                                "3. The URL is correct: https://github.com/owner/repo"
                                ));
                            }
                    )
                    .onStatus(
                            status -> status.is5xxServerError(),
                            response -> {
                                System.err.println("GitHub Server Error: " + response.statusCode());
                                return Mono.error(new IOException("GitHub server error. Please try again later."));
                            }
                    )
                    .bodyToMono(byte[].class)
                    .block();

            if (zipData == null || zipData.length == 0) {
                throw new IOException("Received empty response from GitHub. The repository might be empty or inaccessible.");
            }

            System.out.println("Downloaded " + zipData.length + " bytes");

            // Create temp directory
            Path tempDir = Files.createTempDirectory("ezzyhub_");
            System.out.println("Extracting to: " + tempDir);

            // Extract ZIP
            int fileCount = extractJavaFiles(zipData, tempDir);
            System.out.println("Extracted " + fileCount + " Java files");

            // Find all Java files
            List<File> javaFiles = findJavaFiles(tempDir);

            if (javaFiles.isEmpty()) {
                throw new IOException("No Java files found in the repository. Make sure your repository contains .java files.");
            }

            System.out.println("Found " + javaFiles.size() + " Java files");
            return javaFiles;

        } catch (Exception e) {
            System.err.println("Error downloading repository: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to download repository: " + e.getMessage(), e);
        }
    }

    /**
     * Extract Java files from ZIP data
     */
    private int extractJavaFiles(byte[] zipData, Path tempDir) throws IOException {
        int fileCount = 0;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".java")) {
                    Path filePath = tempDir.resolve(entry.getName());
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
                    fileCount++;
                    System.out.println("Extracted: " + entry.getName());
                }
                zis.closeEntry();
            }
        }

        return fileCount;
    }

    /**
     * Find all Java files in directory
     */
    private List<File> findJavaFiles(Path directory) throws IOException {
        List<File> javaFiles = new ArrayList<>();
        Files.walk(directory)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> javaFiles.add(path.toFile()));
        return javaFiles;
    }

    /**
     * Load Java files from local directory (fallback for demo)
     */
    public List<File> loadFromLocal(String localPath) throws IOException {
        Path directory = Paths.get(localPath);

        if (!Files.exists(directory)) {
            throw new IOException("Directory not found: " + localPath);
        }

        System.out.println("Loading from local directory: " + localPath);
        return findJavaFiles(directory);
    }
}