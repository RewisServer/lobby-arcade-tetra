package dev.volix.rewinside.odyssey.lobby.arcade.tetris.blueprint;

import dev.volix.rewinside.odyssey.common.frames.resource.image.ImageAdapter;

/**
 * @author Benedikt Wüller
 */
public class SBlueprint extends Blueprint {

    public SBlueprint(final ImageAdapter adapter) {
        super(3, 3, adapter.getSheet("S", 10));
        this.addState(new int[] { 0,1,1, 1,1,0, 0,0,0 });
        this.addState(new int[] { 0,1,0, 0,1,1, 0,0,1 });
        this.addState(new int[] { 0,0,0, 0,1,1, 1,1,0 });
        this.addState(new int[] { 1,0,0, 1,1,0, 0,1,0 });
    }

}
