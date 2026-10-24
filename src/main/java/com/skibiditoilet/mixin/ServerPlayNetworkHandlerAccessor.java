package com.skibiditoilet.mixin;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets a jetpack keep a player up without the server taking it for flying (it kicks after 4 s of floating). */
@Mixin(ServerPlayNetworkHandler.class)
public interface ServerPlayNetworkHandlerAccessor {
    @Accessor("floatingTicks")
    void skibidi$setFloatingTicks(int ticks);
}
