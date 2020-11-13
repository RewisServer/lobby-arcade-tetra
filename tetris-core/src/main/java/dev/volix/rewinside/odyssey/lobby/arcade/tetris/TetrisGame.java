package dev.volix.rewinside.odyssey.lobby.arcade.tetris;

import dev.volix.rewinside.odyssey.lobby.arcade.KeyMapping;
import dev.volix.rewinside.odyssey.lobby.arcade.SongPlayerFrameGame;
import dev.volix.rewinside.odyssey.lobby.arcade.setting.BooleanSetting;
import dev.volix.rewinside.odyssey.lobby.arcade.setting.Setting;
import dev.volix.rewinside.odyssey.lobby.arcade.setting.SettingOption;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import dev.volix.rewinside.odyssey.common.frames.alignment.Alignment;
import dev.volix.rewinside.odyssey.common.frames.color.ColorTransformer;
import dev.volix.rewinside.odyssey.common.frames.component.CompoundComponent;
import dev.volix.rewinside.odyssey.common.frames.component.ImageComponent;
import dev.volix.rewinside.odyssey.common.frames.component.TextComponent;
import dev.volix.rewinside.odyssey.lobby.arcade.GameState;
import dev.volix.rewinside.odyssey.lobby.arcade.InputKey;
import dev.volix.rewinside.odyssey.lobby.arcade.nbs.song.SongPlayer;
import dev.volix.rewinside.odyssey.common.frames.resource.font.FontAdapter;
import dev.volix.rewinside.odyssey.common.frames.resource.image.ImageAdapter;
import dev.volix.rewinside.odyssey.lobby.arcade.tetris.blueprint.*;
import dev.volix.rewinside.odyssey.lobby.arcade.tetris.component.Piece;
import dev.volix.rewinside.odyssey.lobby.arcade.tetris.component.ScoreBoard;

/**
 * @author Benedikt Wüller
 */
public class TetrisGame extends SongPlayerFrameGame {

    private static final String GAME_OVER_FONT_NAME = "JetBrainsMono-ExtraBold";

    private static final long MIN_SPEED = 400L;
    private static final long MAX_SPEED = 100L;
    private static final long SPEED_STEP = 50L;

    private final Blueprint[] blueprints;

    private final Dimension tiles = new Dimension(10, 24);
    private final Dimension tileDimensions = new Dimension(10, 10);

    private final CompoundComponent gameBoard;
    protected final ScoreBoard scoreBoard;

    private final FontAdapter fontAdapter;

    private Piece nextPiece = null;
    private Piece currentPiece = null;
    private Piece previewPiece = null;

    private final Set<Piece> placedPieces = new HashSet<>();
    protected int piecesPlaced;

    private long speed = MIN_SPEED;
    private long lastUpdate = 0;

    private int rowsRemovedTotal = 0;
    private int rowsRemovedThisTick = 0;
    private long lastMoveDown = 0;

    private final Setting<Boolean> blockPreviewSetting;

    public TetrisGame(@NotNull final Dimension viewportDimension, @NotNull final ColorTransformer transformer,
                      final ImageAdapter imageAdapter, final FontAdapter fontAdapter, final SongPlayer songPlayer) {
        super(fontAdapter, new Dimension(256, 256), viewportDimension, 50, transformer, songPlayer);

        this.blockPreviewSetting = new BooleanSetting("block_preview", "Block-Vorschau", "Zeigt an, wo ein Block landet.", 0);
        this.getSettings().set(this.blockPreviewSetting);

        this.setInputDescription(InputKey.LEFT, "Links");
        this.setInputDescription(InputKey.RIGHT, "Rechts");
        this.setInputDescription(InputKey.DOWN, "Runter");
        this.setInputDescription(InputKey.UP, "Schnell runter");
        this.setInputDescription(InputKey.SPACE, "Rotieren");

        this.setKeyRepeatInterval(100);

        this.fontAdapter = fontAdapter;

        this.getIdleComponent().addComponent(new ImageComponent(new Point(), this.getCanvasDimensions(), imageAdapter.get("idle")));
        this.getBaseComponent().addComponent(new ImageComponent(new Point(), this.getCanvasDimensions(), imageAdapter.get("background")));

        this.gameBoard = new CompoundComponent(
                new Point(28, 8),
                new Dimension(this.tiles.width * this.tileDimensions.width, this.tiles.height * this.tileDimensions.height)
        );

        this.scoreBoard = new ScoreBoard(
                this.gameBoard.getPosition().x + this.gameBoard.getDimensions().width, this.gameBoard.getPosition().y,
                this.gameBoard.getDimensions().width, this.gameBoard.getDimensions().height, fontAdapter
        );

        this.getBaseComponent().addComponent(this.gameBoard);
        this.getBaseComponent().addComponent(this.scoreBoard);

        this.getGameOverComponent().addComponent(new ImageComponent(new Point(), this.getCanvasDimensions(), imageAdapter.get("game-over")));

        this.blueprints = new Blueprint[] {
                new IBlueprint(imageAdapter), new OBlueprint(imageAdapter), new TBlueprint(imageAdapter), new JBlueprint(imageAdapter),
                new LBlueprint(imageAdapter), new SBlueprint(imageAdapter), new ZBlueprint(imageAdapter),
        };

        this.setNextPiece();
    }

    @Override
    protected boolean onUpdate(final long currentTime, final long delta) {
        if (!super.onUpdate(currentTime, delta)) return false;

        if (this.lastUpdate != 0 && currentTime - this.lastUpdate < this.speed) return true;

        this.rowsRemovedThisTick = 0;
        this.lastUpdate = currentTime;

        if (this.currentPiece == null) {
            this.spawnPiece();
            if (!this.isPositionValid(this.currentPiece.blueprint, this.currentPiece.getState(), this.currentPiece.getTile())) {
                this.setGameOver();
            }
        } else if (currentTime - this.lastMoveDown >= this.speed) {
            if (!this.tryMove(this.currentPiece, 0, 1)) {
                this.placeCurrentPiece();
            } else {
                this.lastMoveDown = this.lastUpdate;
            }
        }

        this.updatePreviewPiece();

        return true;
    }

    private void updatePreviewPiece() {
        final boolean enabled = this.blockPreviewSetting.getSelected().getValue() && this.currentPiece != null;

        if (!enabled) {
            if (this.previewPiece != null) {
                this.gameBoard.removeComponent(this.previewPiece);
                this.previewPiece = null;
            }
            return;
        }

        if (this.previewPiece == null) {
            this.previewPiece = new Piece(this.currentPiece.getTileDimensions(), this.currentPiece.blueprint);
            this.previewPiece.isPreview = true;
            this.gameBoard.addComponentBelow(this.previewPiece, this.currentPiece);
        }

        // Mirror current state of the piece.
        this.previewPiece.setTile(this.currentPiece.getTile());
        this.previewPiece.setState(this.currentPiece.getState());

        // Move preview piece as far down as possible.
        boolean moved;
        do {
            moved = this.tryMove(this.previewPiece, 0, 1);
        } while (moved);
    }

    private void placeCurrentPiece() {
        this.placedPieces.add(this.currentPiece);

        int removedRows = 0;
        for (int dy = this.currentPiece.blueprint.height; dy >= 0; dy--) {
            final int y = this.currentPiece.getTileY() + dy + removedRows;
            if (y < 0 || y >= this.tiles.height) continue;

            boolean solid = true;
            for (int x = 0; x < this.tiles.width; x++) {
                if (this.isSolid(x, y)) continue;
                solid = false;
                break;
            }

            if (!solid) continue;

            this.removeRow(y);
            this.incrementScore();
            removedRows++;
        }

        this.currentPiece = null;

        if (this.previewPiece != null) {
            this.gameBoard.removeComponent(this.previewPiece);
            this.previewPiece = null;
        }
    }

    private void setGameOver() {
        this.setState(GameState.DONE);

        this.getGameOverComponent().addComponent(new TextComponent(
                new Point(53, 127),
                String.valueOf(this.scoreBoard.getScore()),
                new Color(92, 219, 213),
                this.fontAdapter.get(GAME_OVER_FONT_NAME, 20.0f),
                Alignment.TOP_CENTER
        ));
    }

    protected void removeRow(final int row) {
        for (final Piece piece : new HashSet<>(this.placedPieces)) {
            if (!piece.removeRow(row)) continue;
            if (!piece.isEmpty()) continue;
            this.placedPieces.remove(piece);
            this.gameBoard.removeComponent(piece);
        }
    }

    private void incrementScore() {
        this.rowsRemovedTotal += 1;
        this.rowsRemovedThisTick += 1;

        this.scoreBoard.setScore(this.scoreBoard.getScore() + this.rowsRemovedThisTick);
        final int level = this.rowsRemovedTotal / 30 + 1;
        if (level != this.scoreBoard.getLevel()) {
            this.scoreBoard.setLevel(level);
            this.speed = Math.max(MIN_SPEED - (level * SPEED_STEP), MAX_SPEED);
        }
    }

    private boolean tryMove(final Piece piece, final int deltaX, final int deltaY) {
        if (this.canMove(piece, deltaX, deltaY)) {
            piece.setTileX(piece.getTileX() + deltaX);
            piece.setTileY(piece.getTileY() + deltaY);
            return true;
        }
        return false;
    }

    private boolean canMove(final Piece piece, final int deltaX, final int deltaY) {
        if (piece == null) return false;

        if (deltaY != 0) {
            for (int dy = 1; dy <= Math.abs(deltaY); dy++) {
                final int baseY = piece.getTileY() + (deltaY < 0 ? -dy : dy);
                final int baseX = piece.getTileX();
                if (this.isPositionValid(piece.blueprint, piece.getState(), new Point(baseX, baseY))) continue;
                return false;
            }
        }

        if (deltaX != 0) {
            for (int dx = 1; dx <= Math.abs(deltaX); dx++) {
                final int baseY = piece.getTileY();
                final int baseX = piece.getTileX() + (deltaX < 0 ? -dx : dx);
                if (this.isPositionValid(piece.blueprint, piece.getState(), new Point(baseX, baseY))) continue;
                return false;
            }
        }

        return true;
    }

    private boolean isPositionValid(final Blueprint blueprint, final int stateIndex, final Point position) {
        final int[] state = blueprint.getState(stateIndex);

        for (int y = 0; y < blueprint.height; y++) {
            for (int x = 0; x < blueprint.width; x++) {
                final int type = state[y * blueprint.width + x];
                if (type <= 0) continue;

                if (!this.isSolid(position.x + x, position.y + y)) continue;
                return false;
            }
        }

        return true;
    }

    private boolean isPieceSolid(final Piece piece, final int tileX, final int tileY) {
        final int baseX = piece.getTileX();
        final int baseY = piece.getTileY();

        if (baseX > tileX || baseX + piece.blueprint.width <= tileX) return false;
        if (baseY > tileY || baseY + piece.blueprint.height <= tileY) return false;

        final int relativeX = tileX - baseX;
        final int relativeY = tileY - baseY;

        return piece.isSolid(relativeX, relativeY);
    }

    private boolean isSolid(final int tileX, final int tileY) {
        if (tileX < 0 || tileX >= this.tiles.width) return true;
        if (tileY >= this.tiles.height) return true;

        for (final Piece piece : this.placedPieces) {
            if (this.isPieceSolid(piece, tileX, tileY)) return true;
        }

        return false;
    }

    private void spawnPiece() {
        this.currentPiece = new Piece(this.tileDimensions, this.nextPiece.blueprint);
        this.currentPiece.setTile(new Point(this.tiles.width / 2 - this.currentPiece.blueprint.width / 2, 0));
        this.gameBoard.addComponent(this.currentPiece);
        this.setNextPiece();
    }

    private void setNextPiece() {
        this.piecesPlaced += 1;

        if (this.nextPiece != null) {
            this.scoreBoard.removeComponent(this.nextPiece);
        }

        final Blueprint blueprint = this.getNextBlueprint();

        this.nextPiece = new Piece(this.tileDimensions, blueprint);
        this.nextPiece.getPosition().setLocation(
                this.scoreBoard.getDimensions().width / 2 - this.nextPiece.blueprint.width * this.tileDimensions.width / 2,
                50 - (this.nextPiece.blueprint.height == 4 ? 4 : 3) * this.tileDimensions.height / 2
        );

        this.scoreBoard.addComponent(this.nextPiece);
    }

    private Blueprint getNextBlueprint() {
        return this.getNextBlueprint(1);
    }

    private Blueprint getNextBlueprint(final int triesLeft) {
        final int index = (int) Math.round(Math.random() * (this.blueprints.length - 1));
        final Blueprint blueprint = this.blueprints[index];

        // Reduce the chance of the same blueprint being selected multiple times in a row.
        if (this.currentPiece != null && this.currentPiece.blueprint == blueprint && triesLeft >= 1) {
            return this.getNextBlueprint(triesLeft - 1);
        }

        return blueprint;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean tryRotate(final Piece piece) {
        final Blueprint blueprint = piece.blueprint;

        final int nextStateIndex = piece.getState() + 1 < blueprint.getStateCount() ? piece.getState() + 1 : 0;
        final int[] nextState = blueprint.getState(nextStateIndex);
        final Piece tempPiece = piece.copyWithState(nextStateIndex);

        int shiftRight = 0;
        int shiftLeft = 0;

        for (int y = 0; y < blueprint.height; y++) {
            int rowShiftRight = 0;
            int rowShiftLeft = 0;

            for (int x = 0; x < blueprint.width; x++) {
                final int type = nextState[y * blueprint.width + x];
                if (type <= 0) continue;

                final int boardX = tempPiece.getTileX() + x;
                final int boardY = tempPiece.getTileY() + y;

                if (boardX < 0) rowShiftRight = Math.max(rowShiftRight, -boardX);
                if (boardX >= this.tiles.width) rowShiftLeft = Math.max(rowShiftLeft, boardX - this.tiles.width + 1);
                if (boardX < 0 || boardX >= this.tiles.width) continue;
                if (boardY >= this.tiles.height) return false;

                if (this.isSolid(boardX, boardY)) {
                    rowShiftRight = Math.max(rowShiftRight, x + 1);
                    rowShiftLeft = Math.max(rowShiftLeft, blueprint.width - x);
                }
            }

            shiftRight = Math.max(shiftRight, rowShiftRight);
            shiftLeft = Math.max(shiftLeft, rowShiftLeft);
        }

        if (shiftRight == 0 && shiftLeft == 0) {
            piece.setState(nextStateIndex);
            return true;
        }

        if (shiftRight > 0 && this.canMove(tempPiece, shiftRight, 0)) {
            piece.setState(nextStateIndex);
            piece.setTileX(tempPiece.getTileX() + shiftRight);
            return true;
        }

        if (shiftLeft > 0 && this.canMove(tempPiece, -shiftLeft, 0)) {
            piece.setState(nextStateIndex);
            piece.setTileX(tempPiece.getTileX() - shiftLeft);
            return true;
        }

        return false;
    }

    private void handleSidewaysKeys(final InputKey key) {
        if (key == InputKey.LEFT) {
            this.tryMove(this.currentPiece, -1, 0);
        } else if (key == InputKey.RIGHT) {
            this.tryMove(this.currentPiece, 1, 0);
        }
        this.updatePreviewPiece();
    }

    private void handleDownKey(final InputKey key, final long currentTime) {
        if (key != InputKey.DOWN) return;
        if (currentTime - this.lastMoveDown < MAX_SPEED) return;
        if (!this.tryMove(this.currentPiece, 0, 1)) return;
        this.lastMoveDown = currentTime;
    }

    private void handleUpKey(final InputKey key, final long currentTime) {
        if (key != InputKey.UP) return;

        boolean moved;
        do {
            moved = this.tryMove(this.currentPiece, 0, 1);
        } while (moved);

        this.placeCurrentPiece();
        this.lastMoveDown = currentTime;
    }

    @Override
    protected void onKeyDown(@NotNull final InputKey key, long currentTime) {
        super.onKeyDown(key, currentTime);

        if (this.getState() != GameState.RUNNING) return;
        if (!this.getStarted()) return;

        if (this.currentPiece == null) return;

        if (key == InputKey.SPACE) {
            if (!this.tryRotate(this.currentPiece)) return;
            this.updatePreviewPiece();
            return;
        }

        this.handleSidewaysKeys(key);
        this.handleDownKey(key, currentTime);
        this.handleUpKey(key, currentTime);
    }

    @Override
    protected void onKeyRepeat(@NotNull final InputKey key, long currentTime) {
        if (this.getState() != GameState.RUNNING) return;
        if (this.currentPiece == null) return;
        this.handleSidewaysKeys(key);
        this.handleDownKey(key, currentTime);
    }

    @NotNull @Override
    protected List<SettingOption<KeyMapping>> getKeyMappings() {
        final List<SettingOption<KeyMapping>> mappings = new ArrayList<>();
        mappings.add(new SettingOption<>("default", "Standard", new KeyMapping()));
        mappings.add(new SettingOption<>("classic", "Klassisch", new KeyMapping(InputKey.LEFT, InputKey.RIGHT, InputKey.SPACE, InputKey.DOWN, InputKey.UP)));
        return mappings;
    }
}
