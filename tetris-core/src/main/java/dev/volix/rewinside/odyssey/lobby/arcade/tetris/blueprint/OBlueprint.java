package dev.volix.rewinside.odyssey.lobby.arcade.tetris.blueprint;

import dev.volix.rewinside.odyssey.common.frames.resource.image.ImageAdapter;

/**
 * @author Benedikt Wüller
 */
public class OBlueprint extends Blueprint {

    public OBlueprint(final ImageAdapter adapter) {
        super(2, 2, adapter.getSheet("O", 10));
        this.addState(new int[] { 1, 1, 1, 1 });
    }

}
