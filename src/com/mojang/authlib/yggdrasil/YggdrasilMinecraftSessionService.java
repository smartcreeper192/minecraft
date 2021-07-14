// 
// Decompiled by Procyon v0.6-prerelease
// 

package com.mojang.authlib.yggdrasil;

import org.apache.logging.log4j.LogManager;
import com.mojang.authlib.AuthenticationService;
import java.net.URISyntaxException;
import java.net.URI;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import java.util.Iterator;
import com.google.gson.JsonParseException;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import com.mojang.authlib.minecraft.InsecureTextureException;
import com.google.common.collect.Iterables;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import java.util.Map;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.google.common.collect.Multimap;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import java.util.HashMap;
import java.net.InetAddress;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.response.Response;
import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import java.security.spec.KeySpec;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import org.apache.commons.io.IOUtils;
import com.google.common.cache.CacheLoader;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.CacheBuilder;
import java.lang.reflect.Type;
import com.mojang.util.UUIDTypeAdapter;
import java.util.UUID;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import java.security.PublicKey;
import java.net.URL;
import org.apache.logging.log4j.Logger;
import com.mojang.authlib.minecraft.HttpMinecraftSessionService;

public class YggdrasilMinecraftSessionService extends HttpMinecraftSessionService
{
    private static final String[] WHITELISTED_DOMAINS;
    private static final Logger LOGGER;
    private final String baseUrl;
    private final URL joinUrl;
    private final URL checkUrl;
    private final PublicKey publicKey;
    private final Gson gson;
    private final LoadingCache<GameProfile, GameProfile> insecureProfiles;
    
    protected YggdrasilMinecraftSessionService(final YggdrasilAuthenticationService service, final Environment env) {
        super(service);
        this.gson = new GsonBuilder().registerTypeAdapter((Type)UUID.class, (Object)new UUIDTypeAdapter()).create();
        this.insecureProfiles = (LoadingCache<GameProfile, GameProfile>)CacheBuilder.newBuilder().expireAfterWrite(6L, TimeUnit.HOURS).build((CacheLoader)new CacheLoader<GameProfile, GameProfile>() {
            public GameProfile load(final GameProfile key) throws Exception {
                return YggdrasilMinecraftSessionService.this.fillGameProfile(key, false);
            }
        });
        this.baseUrl = env.getSessionHost() + "/session/minecraft/";
        this.joinUrl = HttpAuthenticationService.constantURL(this.baseUrl + "join");
        this.checkUrl = HttpAuthenticationService.constantURL(this.baseUrl + "hasJoined");
        try {
            final X509EncodedKeySpec spec = new X509EncodedKeySpec(IOUtils.toByteArray(YggdrasilMinecraftSessionService.class.getResourceAsStream("/yggdrasil_session_pubkey.der")));
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.publicKey = keyFactory.generatePublic(spec);
        }
        catch (Exception ignored) {
            throw new Error("Missing/invalid yggdrasil public key!");
        }
    }
    
    @Override
    public void joinServer(final GameProfile profile, final String authenticationToken, final String serverId) throws AuthenticationException {
        final JoinMinecraftServerRequest request = new JoinMinecraftServerRequest();
        request.accessToken = authenticationToken;
        request.selectedProfile = profile.getId();
        request.serverId = serverId;
        this.getAuthenticationService().makeRequest(this.joinUrl, request, Response.class);
    }
    
    @Override
    public GameProfile hasJoinedServer(final GameProfile user, final String serverId, final InetAddress address) throws AuthenticationUnavailableException {
        final Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("username", user.getName());
        arguments.put("serverId", serverId);
        if (address != null) {
            arguments.put("ip", address.getHostAddress());
        }
        final URL url = HttpAuthenticationService.concatenateURL(this.checkUrl, HttpAuthenticationService.buildQuery(arguments));
        try {
            final HasJoinedMinecraftServerResponse response = this.getAuthenticationService().makeRequest(url, null, HasJoinedMinecraftServerResponse.class);
            if (response != null && response.getId() != null) {
                final GameProfile result = new GameProfile(response.getId(), user.getName());
                if (response.getProperties() != null) {
                    result.getProperties().putAll((Multimap)response.getProperties());
                }
                return result;
            }
            return null;
        }
        catch (AuthenticationUnavailableException e) {
            throw e;
        }
        catch (AuthenticationException ignored) {
            return null;
        }
    }
    
    @Override
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(final GameProfile profile, final boolean requireSecure) {
        final Property textureProperty = (Property)Iterables.getFirst((Iterable)profile.getProperties().get("textures"), null);
        if (textureProperty == null) {
            return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
        }
        if (requireSecure) {
            if (!textureProperty.hasSignature()) {
                YggdrasilMinecraftSessionService.LOGGER.error("Signature is missing from textures payload");
                throw new InsecureTextureException("Signature is missing from textures payload");
            }
            if (!textureProperty.isSignatureValid(this.publicKey)) {
                YggdrasilMinecraftSessionService.LOGGER.error("Textures payload has been tampered with (signature invalid)");
                throw new InsecureTextureException("Textures payload has been tampered with (signature invalid)");
            }
        }
        MinecraftTexturesPayload result;
        try {
            final String json = new String(Base64.decodeBase64(textureProperty.getValue()), Charsets.UTF_8);
            result = (MinecraftTexturesPayload)this.gson.fromJson(json, (Class)MinecraftTexturesPayload.class);
        }
        catch (JsonParseException e) {
            YggdrasilMinecraftSessionService.LOGGER.error("Could not decode textures payload", (Throwable)e);
            return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
        }
        if (result == null || result.getTextures() == null) {
            return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
        }
        for (final Map.Entry<MinecraftProfileTexture.Type, MinecraftProfileTexture> entry : result.getTextures().entrySet()) {
            if (!isWhitelistedDomain(entry.getValue().getUrl())) {
                YggdrasilMinecraftSessionService.LOGGER.error("Textures payload has been tampered with (non-whitelisted domain)");
                return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
            }
        }
        return result.getTextures();
    }
    
    @Override
    public GameProfile fillProfileProperties(final GameProfile profile, final boolean requireSecure) {
        if (profile.getId() == null) {
            return profile;
        }
        if (!requireSecure) {
            return (GameProfile)this.insecureProfiles.getUnchecked(profile);
        }
        return this.fillGameProfile(profile, true);
    }
    
    protected GameProfile fillGameProfile(final GameProfile profile, final boolean requireSecure) {
        try {
            URL url = HttpAuthenticationService.constantURL(this.baseUrl + "profile/" + UUIDTypeAdapter.fromUUID(profile.getId()));
            url = HttpAuthenticationService.concatenateURL(url, "unsigned=" + !requireSecure);
            final MinecraftProfilePropertiesResponse response = this.getAuthenticationService().makeRequest(url, null, MinecraftProfilePropertiesResponse.class);
            if (response == null) {
                YggdrasilMinecraftSessionService.LOGGER.debug("Couldn't fetch profile properties for " + profile + " as the profile does not exist");
                return profile;
            }
            final GameProfile result = new GameProfile(response.getId(), response.getName());
            result.getProperties().putAll((Multimap)response.getProperties());
            profile.getProperties().putAll((Multimap)response.getProperties());
            YggdrasilMinecraftSessionService.LOGGER.debug("Successfully fetched profile properties for " + profile);
            return result;
        }
        catch (AuthenticationException e) {
            YggdrasilMinecraftSessionService.LOGGER.warn("Couldn't look up profile properties for " + profile, (Throwable)e);
            return profile;
        }
    }
    
    @Override
    public YggdrasilAuthenticationService getAuthenticationService() {
        return (YggdrasilAuthenticationService)super.getAuthenticationService();
    }
    
    private static boolean isWhitelistedDomain(final String url) {
        URI uri = null;
        try {
            uri = new URI(url);
        }
        catch (URISyntaxException ignored) {
            throw new IllegalArgumentException("Invalid URL '" + url + "'");
        }
        final String domain = uri.getHost();
        for (int i = 0; i < YggdrasilMinecraftSessionService.WHITELISTED_DOMAINS.length; ++i) {
            if (domain.endsWith(YggdrasilMinecraftSessionService.WHITELISTED_DOMAINS[i])) {
                return true;
            }
        }
        return false;
    }
    
    static {
        WHITELISTED_DOMAINS = new String[] { ".minecraft.net", ".mojang.com" };
        LOGGER = LogManager.getLogger();
    }
}
