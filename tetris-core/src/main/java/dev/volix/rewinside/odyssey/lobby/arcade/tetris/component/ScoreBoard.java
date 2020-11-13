package dev.volix.rewinside.odyssey.lobby.arcade.tetris.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import lombok.Getter;
import dev.volix.rewinside.odyssey.common.frames.alignment.Alignment;
import dev.volix.rewinside.odyssey.common.frames.component.CompoundComponent;
import dev.volix.rewinside.odyssey.common.frames.component.TextComponent;
import dev.volix.rewinside.odyssey.common.frames.resource.font.FontAdapter;

/**
 * @author Benedikt Wüller
 */
public class ScoreBoard  extends CompoundComponent {

    private static final Color TEXT_COLOR = Color.WHITE;
    private static final String FONT_NAME = "JetBrainsMono-ExtraBold";

    @Getter private int level;
    @Getter private int score;

    private final TextComponent scoreComponent;
    private final TextComponent levelComponent;

    public ScoreBoard(final int x, final int y, final int width, final int height, final FontAdapter adapter) {
        super(new Point(x, y), new Dimension(width, height));

        final Font font = adapter.get(FONT_NAME, 20.0f);
        this.addComponent(new TextComponent(new Point(width / 2, height / 2 - 20), "SCORE", TEXT_COLOR, font, Alignment.TOP_CENTER));
        this.scoreComponent = new TextComponent(new Point(width / 2, height / 2 + 5), "0", new Color(92, 219, 213), font, Alignment.TOP_CENTER);
        this.addComponent(this.scoreComponent);

        this.addComponent(new TextComponent(new Point(width / 2, height / 4 * 3 - 10), "LEVEL", TEXT_COLOR, font, Alignment.TOP_CENTER));
        this.levelComponent = new TextComponent(new Point(width / 2, height / 4 * 3 + 15), "1", new Color(92, 219, 213), font, Alignment.TOP_CENTER);
        this.addComponent(this.levelComponent);
    }

    public void setLevel(final int levelComponent) {
        this.level = levelComponent;
        this.levelComponent.setText(String.valueOf(levelComponent));
    }

    public void setScore(final int score) {
        this.score = score;
        this.scoreComponent.setText(String.valueOf(score));
    }

}
