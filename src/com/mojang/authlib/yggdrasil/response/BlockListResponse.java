// 
// Decompiled by Procyon v0.6-prerelease
// 

package com.mojang.authlib.yggdrasil.response;

import java.util.UUID;
import java.util.Set;

public class BlockListResponse extends Response
{
    private Set<UUID> blockedProfiles;
    
    public Set<UUID> getBlockedProfiles() {
        return this.blockedProfiles;
    }
}
