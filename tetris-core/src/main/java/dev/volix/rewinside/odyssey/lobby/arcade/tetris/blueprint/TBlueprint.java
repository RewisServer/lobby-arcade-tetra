package dev.volix.rewinside.odyssey.lobby.arcade.tetris.blueprint;

import dev.volix.rewinside.odyssey.common.frames.resource.image.ImageAdapter;

/**
 * @author Benedikt Wüller
 */
public class TBlueprint extends Blueprint {

    public TBlueprint(final ImageAdapter adapter) {
        super(3, 3, adapter.getSheet("T", 10));
        this.addState(new int[] { 0,1,0, 1,1,1, 0,0,0 });
        this.addState(new int[] { 0,1,0, 0,1,1, 0,1,0 });
        this.addState(new int[] { 0,0,0, 1,1,1, 0,1,0 });
        this.addState(new int[] { 0,1,0, 1,1,0, 0,1,0 });
    }

}
