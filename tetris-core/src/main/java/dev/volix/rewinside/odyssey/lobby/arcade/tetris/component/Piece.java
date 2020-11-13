package dev.volix.rewinside.odyssey.lobby.arcade.tetris.component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import dev.volix.rewinside.odyssey.common.frames.component.TileBasedComponent;
import dev.volix.rewinside.odyssey.lobby.arcade.tetris.blueprint.Blueprint;

/**
 * @author Benedikt Wüller
 */
public class Piece extends TileBasedComponent {

    public final Blueprint blueprint;

    private final List<Integer> rows = new ArrayList<>();

    private int state = 0;

    public boolean isPreview = false;

    public Piece(@NotNull final Dimension tileDimensions, final Blueprint blueprint) {
        super(new Point(), new Dimension(blueprint.width * tileDimensions.width, blueprint.height * tileDimensions.height), tileDimensions);

        this.blueprint = blueprint;

        for (int i = 0; i < this.blueprint.height; i++) {
            this.rows.add(i);
        }
    }

    public int getState() {
        return state;
    }

    public void setState(final int state) {
        this.state = state;

        this.rows.clear();
        for (int i = 0; i < this.blueprint.height; i++) {
            this.rows.add(i);
        }

        this.setDirty(true);
    }

    public boolean removeRow(final int row) {
        final int baseRow = this.blueprint.height - this.rows.size();
        final int relativeRow = row - this.getTileY() - baseRow;
        if (relativeRow < 0) return false;

        // If the row to be removed is below this piece, move it down by one.
        if (relativeRow >= this.rows.size()) {
            this.setTileY(this.getTileY() + 1);
            return false;
        }

        this.rows.remove(relativeRow);
        this.setDirty(true);
        return true;
    }

    public boolean isEmpty() {
        return this.rows.isEmpty();
    }

    public boolean isSolid(final int x, final int y) {
        if (x < 0 || x >= this.blueprint.width) return false;
        if (y < 0 || y >= this.blueprint.height) return false;

        final int[] state = this.blueprint.getState(this.state);
        final int baseRow = this.blueprint.height - this.rows.size();
        final int index = y - baseRow;

        if (index < 0 || index >= this.rows.size()) return false;
        final int type = state[this.rows.get(index) * this.blueprint.width + x];
        return type > 0;
    }

    public Piece copyWithState(final int state) {
        final Piece copy = new Piece(this.getTileDimensions(), this.blueprint);
        copy.state = state;
        copy.getPosition().setLocation(this.getPosition());
        return copy;
    }

    @Override
    protected void onRender(@NotNull final Graphics2D context, @NotNull final Rectangle bounds) {
        final int baseRow = this.blueprint.height - this.rows.size();

        for (int y = 0; y < this.rows.size(); y++) {
            final int row = this.rows.get(y);

            for (int x = 0; x < this.blueprint.width; x++) {
                final BufferedImage sprite = this.blueprint.getSprite(this.state, x, row + (this.isPreview ? this.blueprint.height : 0));
                final Rectangle section = new Rectangle(
                        x * this.getTileDimensions().width, (baseRow + y) * this.getTileDimensions().height,
                        sprite.getWidth(), sprite.getHeight()
                );

                if (!bounds.intersects(section)) continue;
                context.drawImage(sprite, section.x, section.y, section.width, section.height, null);
            }
        }
    }

}
