// 
// Decompiled by Procyon v0.6-prerelease
// 

package com.mojang.authlib.minecraft;

import java.util.UUID;

public interface SocialInteractionsService
{
    boolean serversAllowed();
    
    boolean realmsAllowed();
    
    boolean chatAllowed();
    
    boolean isBlockedPlayer(final UUID p0);
}
