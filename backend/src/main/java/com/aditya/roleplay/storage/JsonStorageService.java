package com.aditya.roleplay.storage;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.model.CharacterListEntry;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.ConversationSummary;
import com.aditya.roleplay.model.Message;
import com.aditya.roleplay.model.PlayerPersona;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.Story;
import com.aditya.roleplay.model.World;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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
        Path structured = dataPath.resolve("characters").resolve(id + ".json");
        if (Files.exists(structured)) {
            try {
                return Optional.of(objectMapper.readValue(structured.toFile(), RoleplayCharacter.class));
            } catch (IOException e) {
                throw new RoleplayException("Failed to load character: " + id, "STORAGE_ERROR", 500);
            }
        }

        Path legacy = dataPath.resolve(id).resolve(id + ".json");
        if (Files.exists(legacy)) {
            try {
                return Optional.of(objectMapper.readValue(legacy.toFile(), RoleplayCharacter.class));
            } catch (IOException e) {
                throw new RoleplayException("Failed to load character: " + id, "STORAGE_ERROR", 500);
            }
        }

        return Optional.empty();
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
        Path structured = dataPath.resolve("worlds").resolve(id + ".json");
        if (Files.exists(structured)) {
            try {
                return Optional.of(objectMapper.readValue(structured.toFile(), World.class));
            } catch (IOException e) {
                throw new RoleplayException("Failed to load world: " + id, "STORAGE_ERROR", 500);
            }
        }

        Path legacy = dataPath.resolve(id).resolve(id + ".json");
        if (!Files.exists(legacy)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(legacy.toFile(), World.class));
        } catch (IOException e) {
            throw new RoleplayException("Failed to load world: " + id, "STORAGE_ERROR", 500);
        }
    }

    public List<PlayerPersona> loadPersonas() {
        List<String> ids = readStringIdList(dataPath.resolve("personas.json"));
        List<PlayerPersona> personas = new ArrayList<>();
        for (String id : ids) {
            loadPersona(id).ifPresent(personas::add);
        }
        return personas;
    }

    public Optional<PlayerPersona> loadPersona(String id) {
        Path file = dataPath.resolve("personas").resolve(id + ".json");
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(file.toFile(), PlayerPersona.class));
        } catch (IOException e) {
            throw new RoleplayException("Failed to load persona: " + id, "STORAGE_ERROR", 500);
        }
    }

    public List<Story> loadStories() {
        List<String> ids = readStringIdList(dataPath.resolve("stories.json"));
        List<Story> stories = new ArrayList<>();
        for (String id : ids) {
            loadStory(id).ifPresent(stories::add);
        }
        return stories;
    }

    public Optional<Story> loadStory(String id) {
        Path file = dataPath.resolve("stories").resolve(id + ".json");
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(file.toFile(), Story.class));
        } catch (IOException e) {
            throw new RoleplayException("Failed to load story: " + id, "STORAGE_ERROR", 500);
        }
    }

    public Optional<Conversation> loadConversation(String id) {
        Path file = conversationsPath.resolve(id + ".json");
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(file.toFile());
            JsonNode migrated = ConversationJsonMigration.migrate(objectMapper, root);
            return Optional.of(objectMapper.treeToValue(migrated, Conversation.class));
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
        return listConversations(null);
    }

    public List<ConversationSummary> listConversations(String characterIdFilter) {
        if (!Files.exists(conversationsPath)) {
            return List.of();
        }
        List<ConversationSummary> summaries = new ArrayList<>();
        try (Stream<Path> files = Files.list(conversationsPath)) {
            files.filter(p -> p.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            JsonNode root = objectMapper.readTree(path.toFile());
                            JsonNode migrated = ConversationJsonMigration.migrate(objectMapper, root);
                            Conversation conversation = objectMapper.treeToValue(migrated, Conversation.class);
                            if (characterIdFilter != null
                                    && !characterIdFilter.isBlank()
                                    && !characterIdFilter.equals(conversation.characterId())) {
                                return;
                            }
                            summaries.add(toSummary(conversation));
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException e) {
            throw new RoleplayException("Failed to list conversations", "STORAGE_ERROR", 500);
        }
        summaries.sort(Comparator.comparing(ConversationSummary::updatedAt).reversed());
        return summaries;
    }

    private ConversationSummary toSummary(Conversation conversation) {
        String preview = "";
        if (!conversation.messages().isEmpty()) {
            Message last = conversation.messages().get(conversation.messages().size() - 1);
            preview = truncate(last.content(), 120);
        }
        String characterName = loadCharacter(conversation.characterId())
                .map(RoleplayCharacter::name)
                .orElse(conversation.characterId());
        return new ConversationSummary(
                conversation.id(),
                conversation.characterId(),
                characterName,
                preview,
                conversation.messages().size(),
                conversation.updatedAt());
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength - 1).trim() + "…";
    }

    public RoleplayCharacter saveCharacter(RoleplayCharacter character) {
        Path charactersDir = dataPath.resolve("characters");
        try {
            Files.createDirectories(charactersDir);
            Path target = charactersDir.resolve(character.id() + ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), character);
            appendCharacterIndex(character);
            return character;
        } catch (IOException e) {
            throw new RoleplayException("Failed to save character: " + character.id(), "STORAGE_ERROR", 500);
        }
    }

    public PlayerPersona savePersona(PlayerPersona persona) {
        Path personasDir = dataPath.resolve("personas");
        try {
            Files.createDirectories(personasDir);
            Path target = personasDir.resolve(persona.id() + ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), persona);
            appendStringIndex(dataPath.resolve("personas.json"), persona.id());
            return persona;
        } catch (IOException e) {
            throw new RoleplayException("Failed to save persona: " + persona.id(), "STORAGE_ERROR", 500);
        }
    }

    public Story saveStory(Story story) {
        Path storiesDir = dataPath.resolve("stories");
        try {
            Files.createDirectories(storiesDir);
            Path target = storiesDir.resolve(story.id() + ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), story);
            appendStringIndex(dataPath.resolve("stories.json"), story.id());
            return story;
        } catch (IOException e) {
            throw new RoleplayException("Failed to save story: " + story.id(), "STORAGE_ERROR", 500);
        }
    }

    public List<String> listWorldIds() {
        return readStringIdList(dataPath.resolve("worlds.json"));
    }

    private void appendCharacterIndex(RoleplayCharacter character) throws IOException {
        Path indexFile = dataPath.resolve("characters.json");
        List<CharacterListEntry> entries = loadCharacterIndex();
        boolean exists = entries.stream().anyMatch(entry -> entry.id().equals(character.id()));
        if (exists) {
            return;
        }
        List<CharacterListEntry> updated = new ArrayList<>(entries);
        updated.add(new CharacterListEntry(character.id(), character.name(), character.worldId(), character.imageUrl()));
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(indexFile.toFile(), updated);
    }

    private void appendStringIndex(Path indexPath, String id) throws IOException {
        List<String> ids = readStringIdList(indexPath);
        if (ids.contains(id)) {
            return;
        }
        List<String> updated = new ArrayList<>(ids);
        updated.add(id);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(indexPath.toFile(), updated);
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
