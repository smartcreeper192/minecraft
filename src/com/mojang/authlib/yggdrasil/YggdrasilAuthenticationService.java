// 
// Decompiled by Procyon v0.6-prerelease
// 

package com.mojang.authlib.yggdrasil;

import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import org.apache.logging.log4j.LogManager;
import com.google.gson.JsonParseException;
import java.io.IOException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InsufficientPrivilegesException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.UserMigratedException;
import org.apache.commons.lang3.StringUtils;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.response.Response;
import java.net.URL;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.Agent;
import com.mojang.authlib.EnvironmentParser;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import com.mojang.util.UUIDTypeAdapter;
import java.util.UUID;
import com.mojang.authlib.properties.PropertyMap;
import java.lang.reflect.Type;
import com.mojang.authlib.GameProfile;
import com.google.gson.GsonBuilder;
import java.net.Proxy;
import com.mojang.authlib.Environment;
import com.google.gson.Gson;
import javax.annotation.Nullable;
import org.apache.logging.log4j.Logger;
import com.mojang.authlib.HttpAuthenticationService;

public class YggdrasilAuthenticationService extends HttpAuthenticationService
{
    private static final Logger LOGGER;
    @Nullable
    private final String clientToken;
    private final Gson gson;
    private final Environment environment;
    
    public YggdrasilAuthenticationService(final Proxy proxy) {
        this(proxy, determineEnvironment());
    }
    
    public YggdrasilAuthenticationService(final Proxy proxy, final Environment environment) {
        this(proxy, null, environment);
    }
    
    public YggdrasilAuthenticationService(final Proxy proxy, @Nullable final String clientToken) {
        this(proxy, clientToken, determineEnvironment());
    }
    
    public YggdrasilAuthenticationService(final Proxy proxy, @Nullable final String clientToken, final Environment environment) {
        super(proxy);
        this.clientToken = clientToken;
        this.environment = environment;
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter((Type)GameProfile.class, (Object)new GameProfileSerializer());
        builder.registerTypeAdapter((Type)PropertyMap.class, (Object)new PropertyMap.Serializer());
        builder.registerTypeAdapter((Type)UUID.class, (Object)new UUIDTypeAdapter());
        builder.registerTypeAdapter((Type)ProfileSearchResultsResponse.class, (Object)new ProfileSearchResultsResponse.Serializer());
        this.gson = builder.create();
        YggdrasilAuthenticationService.LOGGER.info("Environment: " + environment.asString());
    }
    
    private static Environment determineEnvironment() {
        return EnvironmentParser.getEnvironmentFromProperties().orElse(YggdrasilEnvironment.PROD);
    }
    
    @Override
    public UserAuthentication createUserAuthentication(final Agent agent) {
        if (this.clientToken == null) {
            throw new IllegalStateException("Missing client token");
        }
        return new YggdrasilUserAuthentication(this, this.clientToken, agent, this.environment);
    }
    
    @Override
    public MinecraftSessionService createMinecraftSessionService() {
        return new YggdrasilMinecraftSessionService(this, this.environment);
    }
    
    @Override
    public GameProfileRepository createProfileRepository() {
        return new YggdrasilGameProfileRepository(this, this.environment);
    }
    
    protected <T extends Response> T makeRequest(final URL url, final Object input, final Class<T> classOfT) throws AuthenticationException {
        return this.makeRequest(url, input, classOfT, null);
    }
    
    protected <T extends Response> T makeRequest(final URL url, final Object input, final Class<T> classOfT, @Nullable final String authentication) throws AuthenticationException {
        try {
            final String jsonResult = (input == null) ? this.performGetRequest(url, authentication) : this.performPostRequest(url, this.gson.toJson(input), "application/json");
            final T result = (T)this.gson.fromJson(jsonResult, (Class)classOfT);
            if (result == null) {
                return null;
            }
            try {
                if (!StringUtils.isNotBlank((CharSequence)result.getError())) {
                    return result;
                }
                if ("UserMigratedException".equals(result.getCause())) {
                    throw new UserMigratedException(result.getErrorMessage());
                }
                if ("ForbiddenOperationException".equals(result.getError())) {
                    throw new InvalidCredentialsException(result.getErrorMessage());
                }
                if ("InsufficientPrivilegesException".equals(result.getError())) {
                    throw new InsufficientPrivilegesException(result.getErrorMessage());
                }
                throw new AuthenticationException(result.getErrorMessage());
            }
            catch (IllegalStateException e) {
                throw new AuthenticationUnavailableException("Cannot contact authentication server", e);
            }
        }
        catch (IOException ex) {}
        catch (IllegalStateException ex2) {}
        catch (JsonParseException ex3) {}
        return null;
    }
    
    public YggdrasilSocialInteractionsService createSocialInteractionsService(final String accessToken) throws AuthenticationException {
        return new YggdrasilSocialInteractionsService(this, accessToken, this.environment);
    }
    
    static {
        LOGGER = LogManager.getLogger();
    }
    
    private static class GameProfileSerializer implements JsonSerializer<GameProfile>, JsonDeserializer<GameProfile>
    {
        public GameProfile deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            final JsonObject object = (JsonObject)json;
            final UUID id = object.has("id") ? ((UUID)context.deserialize(object.get("id"), (Type)UUID.class)) : null;
            final String name = object.has("name") ? object.getAsJsonPrimitive("name").getAsString() : null;
            return new GameProfile(id, name);
        }
        
        public JsonElement serialize(final GameProfile src, final Type typeOfSrc, final JsonSerializationContext context) {
            final JsonObject result = new JsonObject();
            if (src.getId() != null) {
                result.add("id", context.serialize((Object)src.getId()));
            }
            if (src.getName() != null) {
                result.addProperty("name", src.getName());
            }
            return (JsonElement)result;
        }
    }
}
