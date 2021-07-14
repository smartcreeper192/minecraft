// 
// Decompiled by Procyon v0.6-prerelease
// 

package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.response.PrivilegesResponse;
import com.mojang.authlib.yggdrasil.response.BlockListResponse;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.Environment;
import java.util.Set;
import javax.annotation.Nullable;
import java.time.Instant;
import java.net.URL;
import java.util.UUID;
import com.mojang.authlib.minecraft.SocialInteractionsService;

public class YggdrasilSocialInteractionsService implements SocialInteractionsService
{
    private static final long BLOCKLIST_REQUEST_COOLDOWN_SECONDS = 120L;
    private static final UUID ZERO_UUID;
    private final URL routePrivileges;
    private final URL routeBlocklist;
    private final YggdrasilAuthenticationService authenticationService;
    private final String accessToken;
    private boolean serversAllowed;
    private boolean realmsAllowed;
    private boolean chatAllowed;
    @Nullable
    private Instant nextAcceptableBlockRequest;
    @Nullable
    private Set<UUID> blockList;
    
    public YggdrasilSocialInteractionsService(final YggdrasilAuthenticationService authenticationService, final String accessToken, final Environment env) throws AuthenticationException {
        this.authenticationService = authenticationService;
        this.accessToken = accessToken;
        this.routePrivileges = HttpAuthenticationService.constantURL(env.getServicesHost() + "/privileges");
        this.routeBlocklist = HttpAuthenticationService.constantURL(env.getServicesHost() + "/privacy/blocklist");
        this.checkPrivileges();
    }
    
    @Override
    public boolean serversAllowed() {
        return this.serversAllowed;
    }
    
    @Override
    public boolean realmsAllowed() {
        return this.realmsAllowed;
    }
    
    @Override
    public boolean chatAllowed() {
        return this.chatAllowed;
    }
    
    @Override
    public boolean isBlockedPlayer(final UUID playerID) {
        if (playerID.equals(YggdrasilSocialInteractionsService.ZERO_UUID)) {
            return false;
        }
        if (this.blockList == null) {
            this.blockList = this.fetchBlockList();
            if (this.blockList == null) {
                return false;
            }
        }
        return this.blockList.contains(playerID);
    }
    
    @Nullable
    private Set<UUID> fetchBlockList() {
        if (this.nextAcceptableBlockRequest != null && this.nextAcceptableBlockRequest.isAfter(Instant.now())) {
            return null;
        }
        this.nextAcceptableBlockRequest = Instant.now().plusSeconds(120L);
        try {
            final BlockListResponse response = this.authenticationService.makeRequest(this.routeBlocklist, null, BlockListResponse.class, "Bearer " + this.accessToken);
            if (response == null) {
                return null;
            }
            return response.getBlockedProfiles();
        }
        catch (AuthenticationException e) {
            return null;
        }
    }
    
    private void checkPrivileges() throws AuthenticationException {
        final PrivilegesResponse response = this.authenticationService.makeRequest(this.routePrivileges, null, PrivilegesResponse.class, "Bearer " + this.accessToken);
        if (response == null) {
            throw new AuthenticationUnavailableException();
        }
        this.chatAllowed = response.getPrivileges().getOnlineChat().isEnabled();
        this.serversAllowed = response.getPrivileges().getMultiplayerServer().isEnabled();
        this.realmsAllowed = response.getPrivileges().getMultiplayerRealms().isEnabled();
    }
    
    static {
        ZERO_UUID = new UUID(0L, 0L);
    }
}
