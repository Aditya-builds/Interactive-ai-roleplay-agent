package com.aditya.roleplay.storage;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.model.CharacterListEntry;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.ConversationSummary;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.World;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class JsonStorageService {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @ConfigProperty(name = "roleplay.data.dir")
    String dataDir;

    private Path dataPath;
    private Path conversationsPath;

    @PostConstruct
    void init() throws IOException {
        dataPath = Path.of(dataDir).toAbsolutePath().normalize();
        conversationsPath = dataPath.resolve("conversations");
        Files.createDirectories(conversationsPath);
    }

    public List<CharacterListEntry> loadCharacterIndex() {
        Path indexFile = dataPath.resolve("characters.json");
        if (!Files.exists(indexFile)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(indexFile.toFile(), new TypeReference<>() {});
        } catch (IOException e) {
            throw new RoleplayException("Failed to read characters.json", "STORAGE_ERROR", 500);
        }
    }

    public List<RoleplayCharacter> loadCharacters() {
        List<RoleplayCharacter> characters = new ArrayList<>();
        for (CharacterListEntry entry : loadCharacterIndex()) {
            loadCharacter(entry.id()).ifPresent(characters::add);
        }
        return characters;
    }

    public Optional<RoleplayCharacter> loadCharacter(String id) {
        Path file = dataPath.resolve(id).resolve(id + ".json");
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(file.toFile(), RoleplayCharacter.class));
        } catch (IOException e) {
            throw new RoleplayException("Failed to load character: " + id, "STORAGE_ERROR", 500);
        }
    }

    public List<World> loadWorlds() {
        List<String> ids = readStringIdList(dataPath.resolve("worlds.json"));
        List<World> worlds = new ArrayList<>();
        for (String id : ids) {
            loadWorld(id).ifPresent(worlds::add);
        }
        return worlds;
    }

    public Optional<World> loadWorld(String id) {
        Path file = dataPath.resolve(id).resolve(id + ".json");
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(file.toFile(), World.class));
        } catch (IOException e) {
            throw new RoleplayException("Failed to load world: " + id, "STORAGE_ERROR", 500);
        }
    }

    public Optional<Conversation> loadConversation(String id) {
        Path file = conversationsPath.resolve(id + ".json");
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(file.toFile(), Conversation.class));
        } catch (IOException e) {
            throw new RoleplayException("Failed to load conversation: " + id, "STORAGE_ERROR", 500);
        }
    }

    public Conversation saveConversation(Conversation conversation) {
        Path target = conversationsPath.resolve(conversation.id() + ".json");
        Path temp = conversationsPath.resolve(conversation.id() + ".json.tmp");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), conversation);
            Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            return conversation;
        } catch (IOException e) {
            throw new RoleplayException("Failed to save conversation: " + conversation.id(), "STORAGE_ERROR", 500);
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
            }
        }
    }

    public List<ConversationSummary> listConversations() {
        if (!Files.exists(conversationsPath)) {
            return List.of();
        }
        List<ConversationSummary> summaries = new ArrayList<>();
        try (Stream<Path> files = Files.list(conversationsPath)) {
            files.filter(p -> p.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            Conversation conversation = objectMapper.readValue(path.toFile(), Conversation.class);
                            summaries.add(new ConversationSummary(
                                    conversation.id(),
                                    conversation.characterId(),
                                    conversation.updatedAt()));
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException e) {
            throw new RoleplayException("Failed to list conversations", "STORAGE_ERROR", 500);
        }
        summaries.sort(Comparator.comparing(ConversationSummary::updatedAt).reversed());
        return summaries;
    }

    public void deleteConversation(String id) {
        Path file = conversationsPath.resolve(id + ".json");
        if (!Files.exists(file)) {
            throw new RoleplayException("Conversation not found: " + id, "CONVERSATION_NOT_FOUND", 404);
        }
        try {
            Files.delete(file);
        } catch (IOException e) {
            throw new RoleplayException("Failed to delete conversation: " + id, "STORAGE_ERROR", 500);
        }
    }

    private List<String> readStringIdList(Path indexPath) {
        if (!Files.exists(indexPath)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(indexPath.toFile(), new TypeReference<>() {});
        } catch (IOException e) {
            throw new RoleplayException("Failed to read index: " + indexPath, "STORAGE_ERROR", 500);
        }
    }
}
