package dev.volix.rewinside.odyssey.lobby.arcade.tetris.blueprint;

import dev.volix.rewinside.odyssey.lobby.arcade.tetris.component.Piece;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import dev.volix.rewinside.odyssey.common.frames.resource.image.SpriteSheet;

/**
 * @author Benedikt Wüller
 */
public abstract class Blueprint {

    private final List<int[]> states = new ArrayList<>();
    private final SpriteSheet spriteSheet;

    public final int width;
    public final int height;

    public Blueprint(final int width, final int height, final SpriteSheet spriteSheet) {
        this.width = width;
        this.height = height;
        this.spriteSheet = spriteSheet;
    }

    protected void addState(final int[] state) {
        this.states.add(state);
    }

    public int getStateCount() {
        return this.states.size();
    }

    public int[] getState(final int index) {
        return this.states.get(index);
    }

    public BufferedImage getSprite(final int stateIndex, final int x, final int y) {
        return this.spriteSheet.getSprite(this.width * stateIndex + x, y);
    }

}
